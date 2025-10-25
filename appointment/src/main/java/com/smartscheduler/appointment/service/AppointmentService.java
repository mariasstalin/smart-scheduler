package com.smartscheduler.appointment.service;

import com.smartscheduler.appointment.exception.SlotUnavailableException;
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

import java.time.Instant;
import java.time.Duration;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AppointmentService {

    private final AppointmentRepository appointmentRepository;
    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final SlotCancellationRepository slotCancellationRepository;
    private final RescheduleHistoryRepository rescheduleHistoryRepository;
    private final RabbitTemplate rabbitTemplate;

    // --- Core Helper Methods (Internal Logic) ---

    public Patient findOrCreatePatient(String name, String email, String phone, String timeZone) {
        return patientRepository.findByPhone(phone)
                .orElseGet(() -> patientRepository.save(Patient.builder()
                        .name(name)
                        .email(email)
                        .phone(phone)
                        .createdAt(Instant.now())
                        .timeZone(timeZone)
                        .build()));
    }

    public Doctor findOrCreateDoctor(Long zohoId, String name, String email, String phone, String specialization) {
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

    /**
     * Common logic to cancel an appointment and publish the event.
     * @param appointment The appointment to be cancelled.
     */
    @Transactional
    public void performCancellation(Appointment appointment) {
        if (appointment.getStatus() == Appointment.Status.CANCELLED) {
            log.info("Appointment already cancelled: {}", appointment.getId());
            return;
        }

        appointment.setStatus(Appointment.Status.CANCELLED);
        appointment.setUpdatedAt(Instant.now());
        appointmentRepository.save(appointment);

        // Record Cancellation Slot
        SlotCancellation sc = SlotCancellation.builder()
                .appointment(appointment)
                .doctor(appointment.getDoctor())
                .cancelledAt(Instant.now())
                .notificationSent(false)
                .build();
        slotCancellationRepository.save(sc);

        // Publish Event
        SlotCancelledEvent slotCancelledEvent = new SlotCancelledEvent(
                appointment.getId(),
                appointment.getDoctor().getId(),
                appointment.getStartTime(),
                appointment.getEndTime()
        );
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.cancelled", slotCancelledEvent);
        log.info("Appointment cancelled and SlotCancelledEvent published: {}", appointment.getId());
    }

    /**
     * Common logic to publish events for a RESCHEDULE (old slot freed, new slot booked).
     */
    private void publishRescheduleEvents(Appointment oldAppointment, Appointment newAppointment) {
        Long doctorId = oldAppointment.getDoctor().getId();
        Long patientId = oldAppointment.getPatient().getId();

        // 1. Publish freed old slot event
        SlotCancelledEvent oldSlotEvent = new SlotCancelledEvent(
                oldAppointment.getId(),
                doctorId,
                oldAppointment.getStartTime(),
                oldAppointment.getEndTime()
        );
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.cancelled", oldSlotEvent);

        // 2. Publish new booking event
        SlotBookedEvent newSlotEvent = new SlotBookedEvent(
                newAppointment.getId(),
                newAppointment.getZohoId(),
                doctorId,
                patientId,
                newAppointment.getStartTime(),
                newAppointment.getEndTime()
        );
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.booked", newSlotEvent);
    }

    /**
     * Logic for manual/Rasa reschedule: Cancels the old, creates a new Appointment record.
     * @param oldAppointment The appointment to be rescheduled.
     * @param newStartTime The requested new start time.
     * @return The newly created appointment.
     */
    @Transactional
    public Appointment createNewRescheduleAppointment(Appointment oldAppointment, Instant newStartTime) {
        Instant newEndTime = newStartTime.plus(Duration.between(oldAppointment.getStartTime(), oldAppointment.getEndTime()));
        Long doctorId = oldAppointment.getDoctor().getId();

        // 1. Conflict Check (Essential for manual bookings)
        List<Appointment> conflicts = appointmentRepository.findConflictingAppointmentsByDoctorId(
                doctorId, newStartTime, newEndTime, Appointment.Status.UPCOMING
        );
        if (!conflicts.isEmpty()) {
            log.warn("Conflict found during manual reschedule for doctor {} on {}", doctorId, newStartTime);
            throw new SlotUnavailableException(newStartTime.toString());
        }

        // 2. Update Old Appointment Status
        oldAppointment.setStatus(Appointment.Status.RESCHEDULED);
        oldAppointment.setUpdatedAt(Instant.now());
        appointmentRepository.save(oldAppointment);

        // 3. Create New Appointment
        Appointment newAppointment = Appointment.builder()
                .zohoId(null) // New appointment will have a new Zoho ID when synced back
                .doctor(oldAppointment.getDoctor())
                .patient(oldAppointment.getPatient())
                .startTime(newStartTime)
                .endTime(newEndTime)
                .status(Appointment.Status.UPCOMING)
                .source(Appointment.Source.MANUAL)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isWhatsappNumber(oldAppointment.getIsWhatsappNumber())
                .build();
        appointmentRepository.save(newAppointment);

        // 4. Save Reschedule History
        RescheduleHistory rh = RescheduleHistory.builder()
                .oldAppointment(oldAppointment)
                .newAppointment(newAppointment)
                .rescheduledAt(Instant.now())
                .build();
        rescheduleHistoryRepository.save(rh);

        // 5. Publish events
        publishRescheduleEvents(oldAppointment, newAppointment);

        return newAppointment;
    }
}