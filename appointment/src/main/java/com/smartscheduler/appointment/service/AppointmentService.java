package com.smartscheduler.appointment.service;

import com.smartscheduler.appointment.payload.BookAppointmentPayload;
import com.smartscheduler.appointment.payload.CancelAppointmentPayload;
import com.smartscheduler.common.entity.*;
import com.smartscheduler.common.event.SlotBookedEvent;
import com.smartscheduler.common.event.SlotCancelledEvent;
import com.smartscheduler.common.repository.*;
import com.smartscheduler.common.util.DateUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SlotCancellationRepository slotCancellationRepository;
    private final RescheduleHistoryRepository rescheduleHistoryRepository;
    private final WaitlistRepository waitlistRepository;
    private final RabbitTemplate rabbitTemplate;

    private static final DateTimeFormatter ZOHO_DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");

    @Transactional
    public Appointment handleBooking(BookAppointmentPayload payload) {
        Doctor doctor = findOrCreateDoctor(Long.valueOf(payload.getStaffId()), payload.getStaffName(), payload.getStaffEmail(), payload.getStaffPhone(), payload.getStaffSpecialization());
        Patient patient = findOrCreatePatient(payload.getPatientName(), payload.getPatientEmail(), payload.getPatientPhone());

        Instant startTime = DateUtils.parseIsoDateTime(payload.getStartTimeIso());
        Instant endTime = DateUtils.parseIsoDateTime(payload.getEndTimeIso());

        Appointment appointment = Appointment.builder()
                .zohoId(payload.getBookingId())
                .doctor(doctor)
                .patient(patient)
                .startTime(startTime)
                .endTime(endTime)
                .status(Appointment.Status.UPCOMING)
                .source(Appointment.Source.ZOHO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isWhatsappNumber(Boolean.TRUE.equals(payload.getIsWhatsappNumber()))
                .build();

        appointmentRepository.save(appointment);

        List<String> slotAlertPreferredDateStrings = payload.getSlotAlertPreferredDates();
        if (!slotAlertPreferredDateStrings.isEmpty()) {
            List<Instant> slotAlertPreferredDates = slotAlertPreferredDateStrings.stream().map(dateStr -> DateUtils.toInstantDate(dateStr, ZoneId.of(payload.getTimeZone()))).collect(Collectors.toList());
            Waitlist wl = Waitlist.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .appointment(appointment)
                    .preferredDates(slotAlertPreferredDates)
                    .active(true)
                    .consecutiveMisses(0)
                    .createdAt(Instant.now())
                    .build();
            waitlistRepository.save(wl);
            log.info("Added waitlist entry for patient={} doctor={} preferredDates={}", patient.getId(), doctor.getId(), slotAlertPreferredDates);
        }

        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.booked", new SlotBookedEvent(appointment.getId(), appointment.getZohoId(), doctor.getId(), patient.getId(), startTime, endTime));

        log.info("Appointment saved and SlotBookedEvent published: {}", appointment.getId());
        return appointment;
    }

    @Transactional
    public void handleCancellation(CancelAppointmentPayload payload) {
        Optional<Appointment> opt = appointmentRepository.findByZohoId(payload.getBookingId());
        if (opt.isEmpty()) {
            log.warn("Appointment not found for cancellation: {}", payload.getBookingId());
            return;
        }

        Appointment appointment = opt.get();
        if (appointment.getStatus() == Appointment.Status.CANCELLED) {
            log.info("Appointment already cancelled: {}", appointment.getId());
            return;
        }

        appointment.setStatus(Appointment.Status.CANCELLED);
        appointment.setUpdatedAt(Instant.now());
        appointmentRepository.save(appointment);

        SlotCancellation sc = SlotCancellation.builder()
                .appointment(appointment)
                .doctor(appointment.getDoctor())
                .cancelledAt(Instant.now())
                .notificationSent(false)
                .build();
        slotCancellationRepository.save(sc);

        SlotCancelledEvent slotCancelledEvent = new SlotCancelledEvent(appointment.getId(), appointment.getDoctor().getId(), appointment.getStartTime(), appointment.getEndTime());
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.cancelled", slotCancelledEvent);

        log.info("Appointment cancelled and SlotCancelledEvent published: {}", appointment.getId());
    }

    @Transactional
    public Appointment performReschedule(Long notificationId, Long oldAppointmentId, Long patientId, Long doctorId, String startTimeIso, String endTimeIso) {
        // Idempotency: if notification already resulted in reschedule, return existing
        if (notificationId != null) {
            Optional<RescheduleHistory> existing = rescheduleHistoryRepository.findByNotificationId(notificationId);
            if (existing.isPresent()) {
                log.info("Reschedule already performed for notificationId={}, returning existing", notificationId);
                return existing.get().getNewAppointment();
            }
        }

        Instant newStartTime = DateUtils.toInstantDate(startTimeIso, ZoneId.of(""));
        Instant oldStartTime = DateUtils.toInstantDate(endTimeIso, ZoneId.of(""));

        // check conflicts for doctor in UPCOMING appointments
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointments(doctorId, newStartTime, oldStartTime, Appointment.Status.UPCOMING);
        if (!conflicts.isEmpty()) {
            log.warn("Conflict found for doctor {} on {}-{}", doctorId, newStartTime, oldStartTime);
            throw new IllegalStateException("Requested slot not available");
        }

        Appointment oldAppointment = null;
        if (oldAppointmentId != null) {
            oldAppointment = appointmentRepository.findById(oldAppointmentId)
                    .orElseThrow(() -> new IllegalArgumentException("Old appointment not found: " + oldAppointmentId));
            oldAppointment.setStatus(Appointment.Status.RESCHEDULED);
            oldAppointment.setUpdatedAt(Instant.now());
            appointmentRepository.save(oldAppointment);
        }

        Patient patient = patientRepository.findById(patientId).orElseThrow(() -> new IllegalArgumentException("Patient not found: " + patientId));
        Doctor doctor = doctorRepository.findById(doctorId).orElseThrow(() -> new IllegalArgumentException("Doctor not found: " + doctorId));

        Appointment newAppointment = Appointment.builder()
                .zohoId(null)
                .doctor(doctor)
                .patient(patient)
                .startTime(newStartTime)
                .endTime(oldStartTime)
                .status(Appointment.Status.UPCOMING)
                .source(Appointment.Source.MANUAL)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isWhatsappNumber(patient.getPhone() != null && !patient.getPhone().isBlank())
                .build();
        appointmentRepository.save(newAppointment);

        RescheduleHistory rh = RescheduleHistory.builder()
                .oldAppointment(oldAppointment)
                .newAppointment(newAppointment)
                .rescheduledAt(Instant.now())
                .build();
        rescheduleHistoryRepository.save(rh);

        // Publish freed old slot event (so NotificationService can continue chain)
        if (oldAppointment != null) {
            SlotCancelledEvent SlotCancelledEvent = new SlotCancelledEvent(oldAppointment.getId(), doctor.getId(), oldAppointment.getStartTime(), oldAppointment.getEndTime());
            rabbitTemplate.convertAndSend("appointments.exchange", "appointments.rescheduled", SlotCancelledEvent);
        }

        // Publish new booking event (inform other systems)
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.booked", new SlotBookedEvent(newAppointment.getId(), newAppointment.getZohoId(), doctor.getId(), patient.getId(), newStartTime, oldStartTime));

        log.info("Reschedule successful oldId={} newId={} notificationId={}", oldAppointmentId, newAppointment.getId(), notificationId);
        return newAppointment;
    }

    private Doctor findOrCreateDoctor(Long zohoId, String name, String email, String phone, String specialization) {
        if (name == null && email == null)
            throw new IllegalArgumentException("Doctor info missing");

        return doctorRepository.findByZohoId(zohoId)
                .orElseGet(() -> doctorRepository.save(Doctor.builder()
                        .zohoId(zohoId)
                        .name(name)
                        .email(email)
                        .phone(phone)
                        .specialization(specialization)
                        .createdAt(Instant.now())
                        .build()));
    }

    private Patient findOrCreatePatient(String name, String email, String phone) {
        return patientRepository.findByEmail(email)
                .orElseGet(() -> patientRepository.save(Patient.builder()
                        .name(name)
                        .email(email)
                        .phone(phone)
                        .createdAt(Instant.now())
                        .build()));
    }

}
