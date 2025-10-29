package com.smartscheduler.notification.service;

import com.smartscheduler.common.entity.*;
import com.smartscheduler.common.event.*;
import com.smartscheduler.common.repository.*;
import com.smartscheduler.notification.config.NotificationProperties;
import com.smartscheduler.common.util.DateUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional; // Ensure this is imported

import java.time.*;
import java.util.*;
import java.util.concurrent.*;

@Service
@Slf4j
public class SlotAllocationService {

    @Autowired
    private WaitlistRepository waitlistRepository;

    @Autowired
    private AppointmentRepository appointmentRepository;

    @Autowired
    private NotificationRepository notificationRepository;

    @Autowired
    private DoctorRepository doctorRepository;

    @Autowired
    private PatientRepository patientRepository;

    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Autowired
    private RasaService rasaService;

    @Autowired
    private PriorityCalculator priorityService;

    @Autowired
    private NotificationProperties notificationProperties;

    @Autowired
    private MessageService messageService;

    private final ScheduledExecutorService scheduler = Executors.newScheduledThreadPool(Runtime.getRuntime().availableProcessors());
    private final ConcurrentMap<Long, ScheduledFuture<?>> timeoutTasks = new ConcurrentHashMap<>();

    // @RabbitListener methods are generally transactional by default due to AMQP configuration,
    // but explicit annotations on service methods are often necessary for nested calls.
    @RabbitListener(queues = "appointments.slots.cancelled.queue")
    public void onSlotCancelled(SlotCancelledEvent event) {
        log.info("Received SlotCancelledEvent: {}", event);

        try {
            doctorRepository.findById(event.getDoctorId()).ifPresent(doctor -> {
                // This call relies on the Patient/Waitlist entities being loaded/modified later,
                // but the method itself doesn't need to be transactional.
                processCancelledSlot(doctor, event.getStartTimeLocal(), event.getEndTimeLocal(), event);
            });
        } catch (Exception ex) {
            log.error("Exception while processing SlotCancelledEvent", ex);
        }
    }

    private void processCancelledSlot(Doctor doctor, LocalDateTime slotStart, LocalDateTime slotEnd, SlotCancelledEvent event) {
        LocalDate slotDateUtc = slotStart.toLocalDate();
        log.info("Processing cancelled slot for doctor={}, slotStart={} (UTC date={})", doctor.getId(), slotStart, slotDateUtc);

        // 1. Reactivate expired opt-outs (logic remains correct)
        Instant now = Instant.now();
        for (Waitlist w : waitlistRepository.findByDoctorAndActiveFalseWithPreferredDates(doctor)) {
            if (w.getOptOutExpiry() != null && w.getOptOutExpiry().isBefore(now)) {
                w.setActive(true);
                w.setConsecutiveMisses(0);
                w.setOptOutExpiry(null);
                waitlistRepository.save(w);
                log.info("Re-activated waitlist id={} after optOutExpiry", w.getId());
            }
        }

        // 2. Build and filter waitlist candidates
        List<Waitlist> candidates = new ArrayList<>(waitlistRepository.findByDoctorAndActiveTrueWithPreferredDates(doctor)
                .stream()
                .filter(w -> preferredDatesContainUtcDate(w, slotDateUtc))
                .toList());

        candidates.sort(Comparator.<Waitlist>comparingDouble(w -> priorityService.calculateScore(w.getPatient()))
                .reversed()
                .thenComparing(Waitlist::getCreatedAt));

        candidates.removeIf(w -> w.getPatient() == null || w.getPatient().getConsecutiveMisses() >= notificationProperties.getMaxConsecutiveMisses());

        // 3. Stage 1: Notify Waitlist
        if (!candidates.isEmpty()) {
            log.info("Starting sequential waitlist notification for {} candidates.", candidates.size());
            notifyWaitlistSequentially(candidates.iterator(), doctor, slotStart, slotEnd, event);
            return;
        }
        log.info("No suitable candidates on the waitlist. Checking future appointments.");


        // 4. Stage 2: Notify Future Appointments (only if waitlist is exhausted)
        List<Appointment> futureAppointments = appointmentRepository.findByDoctorAndStartTimeAfterAndStatus(doctor, slotStart.toInstant(ZoneOffset.UTC), Appointment.Status.UPCOMING);
        futureAppointments.sort(Comparator.comparing(Appointment::getStartTime));
        if (!futureAppointments.isEmpty()) {
            log.info("Starting sequential future appointment notification for {} patients.", futureAppointments.size());
            notifyFutureSequentially(futureAppointments.iterator(), doctor, slotStart, slotEnd, event);
            return;
        }
        log.info("No suitable future appointments for earlier slot. Opening to public.");


        // 5. Stage 3: Open to Public (if both lists are exhausted)
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

    // FIX 1: ADD @Transactional to ensure the Patient proxy can be loaded and saved.
    @Transactional
    private void notifyWaitlistSequentially(Iterator<Waitlist> it, Doctor doctor, LocalDateTime slotStart, LocalDateTime slotEnd, SlotCancelledEvent event) {
        if (!it.hasNext()) {
            // Iteration is complete.
            return;
        }

        Waitlist wl = it.next();
        Patient patient = wl.getPatient();
        if (patient == null) {
            log.warn("Waitlist {} has no patient, skip", wl.getId());
            notifyWaitlistSequentially(it, doctor, slotStart, slotEnd, event);
            return;
        }

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

        Notification notification = Notification.builder()
                .patient(patient)
                .doctor(doctor)
                .appointment(wl.getAppointment())
                .notificationType(Notification.NotificationType.SLOT_OPEN)
                .status(Notification.Status.SENT)
                .sentAt(Instant.now())
                .expiresAt(Instant.now().plus(notificationProperties.getResponseWindowDuration()))
                .startTime(slotStart.toInstant(ZoneOffset.UTC))
                .endTime(slotEnd.toInstant(ZoneOffset.UTC))
                .build();
        notificationRepository.save(notification);

        // This line is now safe due to the @Transactional annotation
        patient.setTotalNotificationsSent(patient.getTotalNotificationsSent() + 1);
        patientRepository.save(patient);

        wl.setNotified(true);
        waitlistRepository.save(wl);

        try {
            List<Map<String, Object>> rasaResponse = rasaService.sendExternalSlotOffer(
                    patient.getPhone(),
                    DateUtils.toFormattedDateTimeString(slotStart, patient.getTimeZoneId()),
                    String.valueOf(wl.getAppointment().getId()),
                    String.valueOf(notification.getId())
            );
            messageService.sendWhatsAppMessage(rasaResponse);
        } catch (Exception ex) {
            log.error("Failed to send message to patientId={}, skipping to next candidate", patient.getId(), ex);
            notification.setStatus(Notification.Status.EXPIRED);
            notificationRepository.save(notification);
            notifyWaitlistSequentially(it, doctor, slotStart, slotEnd, event);
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(
                () -> handleWaitlistTimeout(notification.getId(), it, doctor, slotStart, slotEnd, event),
                notificationProperties.getResponseWindowMinutes(),
                TimeUnit.MINUTES
        );
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
            notif.setStatus(Notification.Status.EXPIRED);
            notif.setExpiresAt(Instant.now());
            notificationRepository.save(notif);

            // Accessing patient needs a session, which @Transactional provides
            Patient patient = notif.getPatient();
            patient.setConsecutiveMisses(patient.getConsecutiveMisses() + 1);
            patientRepository.save(patient);

            if (patient.getConsecutiveMisses() >= notificationProperties.getMaxConsecutiveMisses()) {
                List<Waitlist> userWaitlists = waitlistRepository.findByPatientAndActiveTrueWithPreferredDates(patient);
                Instant optOutExpiry = Instant.now().plus(notificationProperties.getOptOutDuration());
                for (Waitlist w : userWaitlists) {
                    w.setActive(false);
                    w.setOptOutExpiry(optOutExpiry);
                    waitlistRepository.save(w);
                    log.info("Deactivated waitlist id={} for patient={} due to consecutive misses; optOutExpiry={}", w.getId(), patient.getId(), optOutExpiry);
                }
            }

            timeoutTasks.remove(notificationId);
            notifyWaitlistSequentially(it, doctor, slotStart, slotEnd, event);
        } else {
            timeoutTasks.remove(notificationId);
        }
    }

    // FIX 2: ADD @Transactional to ensure the Patient proxy can be loaded and saved.
    @Transactional
    private void notifyFutureSequentially(Iterator<Appointment> it, Doctor doctor, LocalDateTime slotStart, LocalDateTime slotEnd, SlotCancelledEvent event) {
        if (!it.hasNext()) {
            // Iteration is complete. Move to the final stage.
            openSlotToPublic(doctor, slotStart, event);
            return;
        }

        Appointment appointment = it.next();
        Patient patient = appointment.getPatient(); // Accessing patient needs a session

        Notification notification = Notification.builder()
                .patient(patient)
                .appointment(appointment)
                .doctor(doctor)
                .notificationType(Notification.NotificationType.SLOT_OPEN)
                .status(Notification.Status.SENT)
                .sentAt(Instant.now())
                .expiresAt(Instant.now().plus(notificationProperties.getResponseWindowDuration()))
                .startTime(slotStart.toInstant(ZoneOffset.UTC))
                .endTime(slotEnd.toInstant(ZoneOffset.UTC))
                .build();
        notificationRepository.save(notification);

        // This line is now safe due to the @Transactional annotation
        patient.setTotalNotificationsSent(patient.getTotalNotificationsSent() + 1);
        patientRepository.save(patient);

        try {
            rasaService.sendExternalSlotOffer(
                    patient.getPhone(),
                    DateUtils.toFormattedDateTimeString(slotStart, patient.getTimeZoneId()),
                    String.valueOf(appointment.getId()),
                    String.valueOf(notification.getId())
            );
        } catch (Exception ex) {
            log.error("Failed to send message to patientId={}, skipping to next future appointment", patient.getId(), ex);
            notification.setStatus(Notification.Status.EXPIRED);
            notificationRepository.save(notification);
            notifyFutureSequentially(it, doctor, slotStart, slotEnd, event);
            return;
        }

        ScheduledFuture<?> future = scheduler.schedule(
                () -> handleFutureTimeout(notification.getId(), it, doctor, slotStart, slotEnd, event),
                notificationProperties.getResponseWindowMinutes(),
                TimeUnit.MINUTES
        );
        timeoutTasks.put(notification.getId(), future);
    }

    // FIX 3: ADD @Transactional to ensure the Patient proxy can be loaded and saved.
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

            // Accessing patient needs a session
            Patient patient = notification.getPatient();
            patient.setConsecutiveMisses(patient.getConsecutiveMisses() + 1);
            patientRepository.save(patient);

            if (patient.getConsecutiveMisses() >= notificationProperties.getMaxConsecutiveMisses()) {
                List<Waitlist> userWaitlists = waitlistRepository.findByPatientAndActiveTrueWithPreferredDates(patient);
                Instant optOutExpiry = Instant.now().plus(notificationProperties.getOptOutDuration());
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

    // This is already marked @Transactional
    @RabbitListener(queues = "appointments.slots.reallocated.queue")
    @Transactional
    public void onSlotsReallocated(SlotReallocatedEvent slotReallocatedEvent) {
        notificationRepository.findById(slotReallocatedEvent.getNotificationId()).ifPresent(notification -> {
            if (notification.getStatus() != Notification.Status.SENT) {
                log.info("Notification {} already handled (status={})", notification.getId(), notification.getStatus());
                return;
            }

            Patient patient = notification.getPatient(); // Accessing patient needs a session

            ScheduledFuture<?> future = timeoutTasks.remove(notification.getId());
            if (future != null) future.cancel(false);

            notification.setStatus(Notification.Status.CONFIRMED);
            notification.setConfirmedAt(Instant.now());
            notificationRepository.save(notification);

            patient.setTotalNotificationsResponded(patient.getTotalNotificationsResponded() + 1);
            patient.setConsecutiveMisses(0);
            patientRepository.save(patient);

            SlotRescheduledEvent slotRescheduledEvent = SlotRescheduledEvent.builder()
                    .notificationId(notification.getId())
                    .startTime(notification.getStartTime())
                    .endTime(notification.getEndTime())
                    .build();
            rabbitTemplate.convertAndSend("appointments.exchange", "appointments.slots.rescheduled", slotRescheduledEvent);
            log.info("Successfully confirmed and reallocated slot via notification {}. Emitted SlotRescheduledEvent.", notification.getId());
        });
    }

    // This is already marked @Transactional
    @RabbitListener(queues = "appointments.slots.denied.queue")
    @Transactional
    public void onSlotsDenied(SlotReallocatedEvent slotReallocatedEvent) {
        notificationRepository.findById(slotReallocatedEvent.getNotificationId()).ifPresent(notification -> {
            if (notification.getStatus() != Notification.Status.SENT) {
                log.info("Notification {} already handled (status={})", notification.getId(), notification.getStatus());
                return;
            }

            ScheduledFuture<?> future = timeoutTasks.remove(notification.getId());
            if (future != null) future.cancel(false);

            notification.setStatus(Notification.Status.RESPONDED);
            notification.setResponse(Notification.Response.NO);
            notification.setExpiresAt(Instant.now());
            notificationRepository.save(notification);

            SlotCancelledEvent reEmit = new SlotCancelledEvent(
                    notification.getAppointment() != null ? notification.getAppointment().getId() : null,
                    notification.getDoctor().getId(),
                    notification.getStartTime(),
                    notification.getEndTime()
            );
            rabbitTemplate.convertAndSend("appointments.exchange", "appointments.slots.cancelled", reEmit);
            log.info("Patient declined; re-emitted SlotCancelledEvent for doctor={} slot={}", notification.getDoctor().getId(), notification.getStartTime());
        });
    }

    private void openSlotToPublic(Doctor doctor, LocalDateTime slotStart, SlotCancelledEvent event) {
        log.info("Opening slot to public for doctor={} slotStart={}", doctor.getId(), slotStart);
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.slots.opened", Map.of("doctorId", doctor.getId(), "startIso", event.getStartTime(), "endIso", event.getEndTime()));
    }
}