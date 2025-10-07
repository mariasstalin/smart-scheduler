package com.smartscheduler.notification.service;

import com.smartscheduler.common.entity.*;
import com.smartscheduler.common.event.*;
import com.smartscheduler.common.repository.*;
import com.smartscheduler.notification.twilio.TwilioClient;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.*;
import java.time.temporal.TemporalAmount;
import java.time.temporal.TemporalUnit;
import java.util.*;
import java.util.concurrent.*;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final WaitlistRepository waitlistRepository;
    private final AppointmentRepository appointmentRepository;
    private final NotificationRepository notificationRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final TwilioClient twilioClient;
    private final RabbitTemplate rabbitTemplate;

    // single-thread scheduling for sequencing notifications; pool size tuned to needs
    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(8);
    private final ConcurrentMap<Long, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();

    // configuration: how long to wait for a reply
    private static final long RESPONSE_WINDOW_MINUTES = 5;
    private static final int MAX_CONSECUTIVE_MISSES = 3;
    private static final Duration OPT_OUT_DURATION = Duration.ofHours(24);

    // The AppointmentService publishes SlotCancelledEvent to "appointments.exchange" with routing "appointments.cancelled".
    @RabbitListener(queues = "appointments.cancelled")
    public void onSlotCancelled(SlotCancelledEvent event) {
        log.info("Received SlotCancelledEvent: {}", event);

        try {
            doctorRepository.findById(event.getDoctorId()).ifPresent(doctor -> {
                processCancelledSlot(doctor, event.getStartTimeLocal(), event.getEndTimeLocal(), event);
            });
        } catch (Exception ex) {
            log.error("Exception while processing SlotCancelledEvent", ex);
        }
    }

    private void processCancelledSlot(Doctor doctor, LocalDateTime slotStart, LocalDateTime slotEnd, SlotCancelledEvent event) {
        LocalDate slotDateUtc = slotStart.toLocalDate();
        log.info("Processing cancelled slot for doctor={}, slotStart={} (UTC date={})", doctor.getId(), slotStart, slotDateUtc);

        // 1) Find waitlist entries for this doctor whose preferredDates include this slot's UTC LocalDate and are active.
        List<Waitlist> candidates = waitlistRepository.findByDoctorAndActiveTrue(doctor)
                .stream()
                .filter(w -> preferredDatesContainUtcDate(w, slotDateUtc))
                .toList();

        // Reactivate waitlist entries if optOutExpiry passed
        Instant now = Instant.now();
        for (Waitlist w : waitlistRepository.findByDoctorAndActiveFalse(doctor)) {
            if (w.getOptOutExpiry() != null && w.getOptOutExpiry().isBefore(now)) {
                w.setActive(true);
                w.setConsecutiveMisses(0);
                w.setOptOutExpiry(null);
                waitlistRepository.save(w);
                log.info("Re-activated waitlist id={} after optOutExpiry", w.getId());
            }
        }

        // compute effective priority score: patient.getPriorityScore() minus penalty for consecutive misses
        candidates.sort(Comparator.<Waitlist>comparingDouble(w -> effectivePriorityScore(w.getPatient()))
                .reversed()
                .thenComparing(Waitlist::getCreatedAt));

        // remove candidates who are currently opted-out (active false should already filter) or with consecutive misses >= threshold
        candidates.removeIf(w -> w.getPatient() == null || w.getPatient().getConsecutiveMisses() >= MAX_CONSECUTIVE_MISSES);

        if (!candidates.isEmpty()) {
            notifyWaitlistSequentially(candidates.iterator(), doctor, slotStart, slotEnd, event);
            return;
        }

        // 2) fallback - notify same-day future appointments (later in day) for this doctor
        List<Appointment> futureAppointments = appointmentRepository.findByDoctorAndStartTimeAfterAndStatus(doctor, slotStart, Appointment.Status.UPCOMING);
        futureAppointments.sort(Comparator.comparing(Appointment::getStartTime));
        if (!futureAppointments.isEmpty()) {
            notifyFutureSequentially(futureAppointments.iterator(), doctor, slotStart, slotEnd, event);
            return;
        }

        // 3) no candidates -> open slot to public
        openSlotToPublic(doctor, slotStart, event);
    }

    private boolean preferredDatesContainUtcDate(Waitlist w, LocalDate slotDateUtc) {
        if (w.getPreferredDates() == null) {
            return false;
        }
        for (WaitlistPreferredDate waitlistPreferredDate : w.getPreferredDates()) {
            if (waitlistPreferredDate == null) {
                continue;
            }
            if (waitlistPreferredDate.getPreferredDateLocal().equals(slotDateUtc)) {
                return true;
            }
        }
        return false;
    }

    private double effectivePriorityScore(Patient p) {
        double base = p.getPriorityScore(); // 0..100
        // Penalize consecutive misses (10 points per miss)
        double penalty = p.getConsecutiveMisses() * 10.0;
        return Math.max(0.0, base - penalty);
    }

    private void notifyWaitlistSequentially(Iterator<Waitlist> it, Doctor doctor, LocalDateTime slotStart, LocalDateTime slotEnd, SlotCancelledEvent event) {
        if (!it.hasNext()) {
            processCancelledSlot(doctor, slotStart, slotEnd, event); // fallback
            return;
        }

        Waitlist wl = it.next();
        Patient patient = wl.getPatient();
        if (patient == null) {
            log.warn("Waitlist {} has no patient, skip", wl.getId());
            notifyWaitlistSequentially(it, doctor, slotStart, slotEnd, event);
            return;
        }

        // check opt-out expiry now
        if (!wl.getActive()) {
            if (wl.getOptOutExpiry() != null && wl.getOptOutExpiry().isBefore(Instant.now())) {
                wl.setActive(true);
                wl.setConsecutiveMisses(0);
                wl.setOptOutExpiry(null);
                waitlistRepository.save(wl);
            } else {
                log.info("Skipping inactive waitlist id={} for patient={}", wl.getId(), patient.getId());
                notifyWaitlistSequentially(it, doctor, slotStart, slotEnd, event);
                return;
            }
        }

        // prepare notification record
        Notification notification = Notification.builder()
                .patient(patient)
                .doctor(doctor)
                .appointment(wl.getAppointment())
                .notificationType(Notification.NotificationType.SLOT_OPEN)
                .status(Notification.Status.SENT)
                .sentAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMinutes(RESPONSE_WINDOW_MINUTES)))
                .startTime(slotStart.toInstant(ZoneOffset.UTC))
                .endTime(slotEnd.toInstant(ZoneOffset.UTC))
                .build();
        notificationRepository.save(notification);

        // increment total notifications sent (for analytics/responsiveness)
        patient.setTotalNotificationsSent(patient.getTotalNotificationsSent() + 1);
        patientRepository.save(patient);

        // mark waitlist notified
        wl.setNotified(true);
        waitlistRepository.save(wl);

        // send message (Twilio wrapper). Twilio client expected to be robust/retry internally.
        String humanSlot = slotStart.toString() + " (UTC)"; // in prod convert to patient's timezone for message
        String msg = String.format("Slot available at %s. Reply YES to confirm. (notificationId:%d)", humanSlot, notification.getId());
        try {
            twilioClient.sendMessage(patient.getPhone(), msg);
        } catch (Exception ex) {
            log.error("Failed to send Twilio message to patientId={}, skipping to next candidate", patient.getId(), ex);
            notification.setStatus(Notification.Status.EXPIRED);
            notificationRepository.save(notification);
            // do not increment consecutiveMisses here (send failure is not patient fault)
            notifyWaitlistSequentially(it, doctor, slotStart, slotEnd, event);
            return;
        }

        // schedule timeout task
        ScheduledFuture<?> future = scheduler.schedule(() -> handleWaitlistTimeout(notification.getId(), it, doctor, slotStart, slotEnd, event), RESPONSE_WINDOW_MINUTES, TimeUnit.MINUTES);
        timeoutTasks.put(notification.getId(), future);
    }

    @Transactional
    protected void handleWaitlistTimeout(Long notificationId, Iterator<Waitlist> it, Doctor doctor, LocalDateTime slotStart, LocalDateTime slotEnd, SlotCancelledEvent event) {
        Notification notif = notificationRepository.findById(notificationId).orElse(null);
        if (notif == null) {
            notifyWaitlistSequentially(it, doctor, slotStart, slotEnd, event);
            return;
        }

        if (notif.getStatus() == Notification.Status.SENT) {
            // expired w/o response
            notif.setStatus(Notification.Status.EXPIRED);
            notif.setExpiresAt(Instant.now());
            notificationRepository.save(notif);

            Patient patient = notif.getPatient();
            // increment consecutive misses and totalSent already incremented earlier
            patient.setConsecutiveMisses(patient.getConsecutiveMisses() + 1);
            patientRepository.save(patient);

            // If consecutive misses exceed threshold, deactivate all their waitlist entries and set opt-out expiry
            if (patient.getConsecutiveMisses() >= MAX_CONSECUTIVE_MISSES) {
                List<Waitlist> userWaitlists = waitlistRepository.findByPatientAndActiveTrue(patient);
                Instant optOutExpiry = Instant.now().plus(OPT_OUT_DURATION);
                for (Waitlist w : userWaitlists) {
                    w.setActive(false);
                    w.setOptOutExpiry(optOutExpiry);
                    waitlistRepository.save(w);
                    log.info("Deactivated waitlist id={} for patient={} due to consecutive misses; optOutExpiry={}", w.getId(), patient.getId(), optOutExpiry);
                }
            }

            timeoutTasks.remove(notificationId);
            // continue chain to next waitlist candidate
            notifyWaitlistSequentially(it, doctor, slotStart, slotEnd, event);
        } else {
            // already handled (responded)
            timeoutTasks.remove(notificationId);
        }
    }

    private void notifyFutureSequentially(Iterator<Appointment> it, Doctor doctor, LocalDateTime slotStart, LocalDateTime slotEnd, SlotCancelledEvent event) {
        if (!it.hasNext()) {
            openSlotToPublic(doctor, slotStart, event);
            return;
        }

        Appointment appointment = it.next();
        Patient patient = appointment.getPatient();

        Notification notification = Notification.builder()
                .patient(patient)
                .appointment(appointment)
                .doctor(doctor)
                .notificationType(Notification.NotificationType.SLOT_OPEN)
                .status(Notification.Status.SENT)
                .sentAt(Instant.now())
                .expiresAt(Instant.now().plus(Duration.ofMinutes(RESPONSE_WINDOW_MINUTES)))
                .startTime(slotStart.toInstant(ZoneOffset.UTC))
                .endTime(slotEnd.toInstant(ZoneOffset.UTC))
                .build();
        notificationRepository.save(notification);

        patient.setTotalNotificationsSent(patient.getTotalNotificationsSent() + 1);
        patientRepository.save(patient);

        String humanSlot = slotStart.toString() + " (UTC)";
        String msg = String.format("Slot available at %s. Reply YES to confirm. (notificationId:%d)", humanSlot, notification.getId());

        try {
            twilioClient.sendMessage(patient.getPhone(), msg);
        } catch (Exception ex) {
            log.error("Failed to send Twilio message to patientId={}, skipping to next future appointment", patient.getId(), ex);
            notification.setStatus(Notification.Status.EXPIRED);
            notificationRepository.save(notification);
            notifyFutureSequentially(it, doctor, slotStart, slotEnd, event);
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(() -> handleFutureTimeout(notification.getId(), it, doctor, slotStart, slotEnd, event), RESPONSE_WINDOW_MINUTES, TimeUnit.MINUTES);
        timeoutTasks.put(notification.getId(), future);
    }

    @Transactional
    protected void handleFutureTimeout(Long notificationId, Iterator<Appointment> it, Doctor doctor, LocalDateTime slotStart, LocalDateTime slotEnd, SlotCancelledEvent event) {
        Notification notification = notificationRepository.findById(notificationId).orElse(null);
        if (notification == null) {
            notifyFutureSequentially(it, doctor, slotStart, slotEnd, event);
            return;
        }

        if (notification.getStatus() == Notification.Status.SENT) {
            notification.setStatus(Notification.Status.EXPIRED);
            notification.setExpiresAt(Instant.now());
            notificationRepository.save(notification);

            // increment consecutive misses for the patient (future appointment owner) - may apply same opt-out logic
            Patient patient = notification.getPatient();
            patient.setConsecutiveMisses(patient.getConsecutiveMisses() + 1);
            patientRepository.save(patient);

            if (patient.getConsecutiveMisses() >= MAX_CONSECUTIVE_MISSES) {
                List<Waitlist> userWaitlists = waitlistRepository.findByPatientAndActiveTrue(patient);
                Instant optOutExpiry = Instant.now().plus(OPT_OUT_DURATION);
                for (Waitlist w : userWaitlists) {
                    w.setActive(false);
                    w.setOptOutExpiry(optOutExpiry);
                    waitlistRepository.save(w);
                }
            }

            timeoutTasks.remove(notificationId);
            notifyFutureSequentially(it, doctor, slotStart, slotEnd, event);
        } else {
            timeoutTasks.remove(notificationId);
        }
    }

    @RabbitListener(queues = "appointments.patient_response")
    @Transactional
    public void onPatientResponse(PatientResponseEvent responseEvent) {
        log.info("Received PatientResponseEvent: {}", responseEvent);

        notificationRepository.findById(responseEvent.getNotificationId()).ifPresent(notification -> {
            if (notification.getStatus() != Notification.Status.SENT) {
                log.info("Notification {} already handled (status={})", notification.getId(), notification.getStatus());
                return;
            }

            Patient patient = notification.getPatient();
            String resp = responseEvent.getResponse();

            // Cancel timeout task
            ScheduledFuture<?> future = timeoutTasks.remove(notification.getId());
            if (future != null) future.cancel(false);

            if ("YES".equalsIgnoreCase(resp)) {
                notification.setStatus(Notification.Status.CONFIRMED);
                notification.setConfirmedAt(Instant.now());
                notificationRepository.save(notification);

                // update patient stats
                patient.setTotalNotificationsResponded(patient.getTotalNotificationsResponded() + 1);
                patient.setConsecutiveMisses(0);
                patientRepository.save(patient);

                // publish reschedule request to rescheduler queue (SlotRescheduledEvent)
                SlotRescheduledEvent res = SlotRescheduledEvent.builder()
                        .notificationId(notification.getId())
                        .appointmentId(notification.getAppointment() != null ? notification.getAppointment().getId() : null)
                        .patientId(patient.getId())
                        .doctorId(notification.getDoctor().getId())
                        .startTime(notification.getStartTime())
                        .endTime(notification.getEndTime())
                        .build();

                rabbitTemplate.convertAndSend("reschedule_request_queue", res);
                log.info("Published SlotRescheduledEvent notificationId={}", notification.getId());

            } else {
                // NO or any other response => mark declined and re-emit the slot cancelled so chain continues
                notification.setStatus(Notification.Status.RESPONDED);
                notification.setResponse(Notification.Response.NO);
                notification.setExpiresAt(Instant.now());
                notificationRepository.save(notification);

                // increment consecutive misses? The patient actively declined, we treat as responded (do not increment consecutive misses)
                // Re-emit SlotCancelledEvent so NotificationService picks next candidate
                SlotCancelledEvent reEmit = new SlotCancelledEvent(
                        notification.getAppointment() != null ? notification.getAppointment().getId() : null,
                        notification.getDoctor().getId(),
                        notification.getStartTime(),
                        notification.getEndTime()
                );
                rabbitTemplate.convertAndSend("appointments.exchange", "appointments.cancelled", reEmit);
                log.info("Patient declined; re-emitted SlotCancelledEvent for doctor={} slot={}", notification.getDoctor().getId(), notification.getStartTime());
            }
        });
    }

    private void openSlotToPublic(Doctor doctor, LocalDateTime slotStart, SlotCancelledEvent event) {
        log.info("Opening slot to public for doctor={} slotStart={}", doctor.getId(), slotStart);
        // Publish to appointments.exchange with routing key appointments.open — AppointmentService or a sync worker should handle making slot visible.
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.open",
                Map.of("doctorId", doctor.getId(), "startIso", event.getStartTime(), "endIso", event.getEndTime()));
    }
}
