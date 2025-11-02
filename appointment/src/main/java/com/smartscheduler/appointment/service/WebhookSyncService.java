package com.smartscheduler.appointment.service;

import com.smartscheduler.appointment.exception.AppointmentNotFoundException;
import com.smartscheduler.appointment.payload.BookAppointmentPayload;
import com.smartscheduler.appointment.payload.CancelAppointmentPayload;
import com.smartscheduler.appointment.payload.RescheduleAppointmentPayload;
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
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class WebhookSyncService {

    private final DoctorRepository doctorRepository;
    private final PatientRepository patientRepository;
    private final AppointmentRepository appointmentRepository;
    private final RescheduleHistoryRepository rescheduleHistoryRepository;
    private final WaitlistRepository waitlistRepository;
    private final SlotCancellationRepository slotCancellationRepository;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public void handleBooking(BookAppointmentPayload payload) {
        Doctor doctor = findOrCreateDoctor(
                Long.valueOf(payload.getStaffId()), payload.getStaffName(), payload.getStaffEmail(),
                removeLeadingPlus(payload.getStaffPhone()), payload.getStaffSpecialization()
        );
        Patient patient = findOrCreatePatient(
                payload.getPatientName(), payload.getPatientEmail(), removeLeadingPlus(payload.getPatientPhone()),
                payload.getTimeZone()
        );

        Instant startTime = DateUtils.parseDateTimeIso(payload.getStartTimeIso());
        Instant endTime = DateUtils.parseDateTimeIso(payload.getEndTimeIso());

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

        // Waitlist logic
        List<String> slotAlertPreferredDateStrings = payload.getSlotAlertPreferredDates();
        if (!slotAlertPreferredDateStrings.isEmpty()) {
            List<WaitlistPreferredDate> slotAlertPreferredDates = slotAlertPreferredDateStrings.stream()
                    .map(dateStr -> new WaitlistPreferredDate(DateUtils.toInstantDate(dateStr, ZoneId.of(payload.getTimeZone()))))
                    .toList();

            Waitlist wl = Waitlist.builder()
                    .doctor(doctor)
                    .patient(patient)
                    .appointment(appointment)
                    .createdAt(Instant.now())
                    .preferredDates(new ArrayList<>()) // Using ArrayList constructor safety
                    .build();

            slotAlertPreferredDates.forEach(wl::addPreferredDate);
            waitlistRepository.save(wl);
            log.info("Added waitlist entry for patient={} doctor={}", patient.getId(), doctor.getId());
        }

        // Publish Slot Booked Event
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.slots.booked", new SlotBookedEvent(appointment.getId(), appointment.getZohoId(), doctor.getId(), patient.getId(), startTime, endTime));

        log.info("Appointment saved and SlotBookedEvent published: {}", appointment.getId());
    }

    @Transactional
    public void handleCancellation(CancelAppointmentPayload payload) {
        Appointment appointment = appointmentRepository.findByZohoId(payload.getBookingId()).orElseThrow(() -> {
            log.warn("Appointment not found for Zoho cancellation: {}", payload.getBookingId());
            return new AppointmentNotFoundException(payload.getBookingId());
        });

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
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.slots.cancelled", slotCancelledEvent);
        log.info("Zoho cancellation sync processed for Zoho ID: {}", payload.getBookingId());
    }

    @Transactional
    public void handleReschedule(RescheduleAppointmentPayload payload) {
        Appointment oldAppointment = appointmentRepository.findByZohoId(payload.getBookingId()).orElseThrow(() -> {
            log.warn("Appointment not found during Zoho reschedule sync: {}", payload.getBookingId());
            return new AppointmentNotFoundException(payload.getBookingId());
        });

        Instant oldStartTime = oldAppointment.getStartTime();
        Instant oldEndTime = oldAppointment.getEndTime();

        Instant newStartTime = DateUtils.parseDateTimeIso(payload.getIsoStartTime());
        Instant newEndTime = DateUtils.parseDateTimeIso(payload.getIsoEndTime());

        oldAppointment.setStatus(Appointment.Status.RESCHEDULED);
        String archivedZohoId = "ARCHIVED|" + oldAppointment.getId() + "|" + payload.getBookingId();
        oldAppointment.setZohoId(archivedZohoId);
        oldAppointment.setUpdatedAt(Instant.now());
        appointmentRepository.saveAndFlush(oldAppointment);

        Appointment newAppointment = Appointment.builder()
                .zohoId(payload.getBookingId())
                .doctor(oldAppointment.getDoctor())
                .patient(oldAppointment.getPatient())
                .startTime(newStartTime)
                .endTime(newEndTime)
                .status(Appointment.Status.UPCOMING)
                .source(Appointment.Source.ZOHO)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .isWhatsappNumber(oldAppointment.getIsWhatsappNumber())
                .build();
        appointmentRepository.save(newAppointment);

        RescheduleHistory rh = RescheduleHistory.builder()
                .oldAppointment(oldAppointment)
                .newAppointment(newAppointment)
                .rescheduledAt(Instant.now())
                .build();
        rescheduleHistoryRepository.save(rh);

        SlotCancelledEvent oldSlotEvent = new SlotCancelledEvent(oldAppointment.getId(), oldAppointment.getDoctor().getId(), oldStartTime, oldEndTime);
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.slots.cancelled", oldSlotEvent);

        SlotBookedEvent newSlotEvent = new SlotBookedEvent(newAppointment.getId(), newAppointment.getZohoId(), newAppointment.getDoctor().getId(), newAppointment.getPatient().getId(), newStartTime, newEndTime);
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.slots.booked", newSlotEvent);

        log.info("Zoho Reschedule sync successful for Zoho ID: {}. New time: {}-{}", payload.getBookingId(), newStartTime, newEndTime);
    }

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

    private String removeLeadingPlus(String phoneNumber) {
        if (phoneNumber == null || phoneNumber.isEmpty()) {
            return phoneNumber;
        }
        if (phoneNumber.startsWith("+")) {
            return phoneNumber.substring(1);
        }
        return phoneNumber;
    }

}