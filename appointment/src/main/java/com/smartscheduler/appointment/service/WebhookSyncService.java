package com.smartscheduler.appointment.service;

import com.smartscheduler.appointment.exception.AppointmentNotFoundException;
import com.smartscheduler.appointment.payload.BookAppointmentPayload;
import com.smartscheduler.appointment.payload.CancelAppointmentPayload;
import com.smartscheduler.appointment.payload.RescheduleAppointmentPayload;
import com.smartscheduler.common.entity.*;
import com.smartscheduler.common.event.SlotBookedEvent;
import com.smartscheduler.common.event.SlotCancelledEvent;
import com.smartscheduler.common.repository.AppointmentRepository;
import com.smartscheduler.common.repository.RescheduleHistoryRepository;
import com.smartscheduler.common.repository.WaitlistRepository;
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

    private final AppointmentRepository appointmentRepository;
    private final RescheduleHistoryRepository rescheduleHistoryRepository;
    private final WaitlistRepository waitlistRepository;
    private final RabbitTemplate rabbitTemplate;
    private final AppointmentService appointmentService;

    @Transactional
    public void handleBooking(BookAppointmentPayload payload) {
        Doctor doctor = appointmentService.findOrCreateDoctor(
                Long.valueOf(payload.getStaffId()), payload.getStaffName(), payload.getStaffEmail(),
                removeLeadingPlus(payload.getStaffPhone()), payload.getStaffSpecialization()
        );
        Patient patient = appointmentService.findOrCreatePatient(
                payload.getPatientName(), payload.getPatientEmail(), payload.getPatientPhone(),
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
                    .active(true)
                    .createdAt(Instant.now())
                    .preferredDates(new ArrayList<>()) // Using ArrayList constructor safety
                    .build();

            slotAlertPreferredDates.forEach(wl::addPreferredDate);
            waitlistRepository.save(wl);
            log.info("Added waitlist entry for patient={} doctor={}", patient.getId(), doctor.getId());
        }

        // Publish Slot Booked Event
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.booked", new SlotBookedEvent(appointment.getId(), appointment.getZohoId(), doctor.getId(), patient.getId(), startTime, endTime));

        log.info("Appointment saved and SlotBookedEvent published: {}", appointment.getId());
    }

    @Transactional
    public void handleCancellation(CancelAppointmentPayload payload) {
        Appointment appointmentToCancel = appointmentRepository.findByZohoId(payload.getBookingId()).orElseThrow(() -> {
            log.warn("Appointment not found for Zoho cancellation: {}", payload.getBookingId());
            return new AppointmentNotFoundException(payload.getBookingId());
        });

        appointmentService.performCancellation(appointmentToCancel);
        log.info("Zoho cancellation sync processed for Zoho ID: {}", payload.getBookingId());
    }

    @Transactional
    public void handleReschedule(RescheduleAppointmentPayload payload) {
        Appointment appointmentToUpdate = appointmentRepository.findByZohoId(payload.getBookingId()).orElseThrow(() -> {
            log.warn("Appointment not found during Zoho reschedule sync: {}", payload.getBookingId());
            return new AppointmentNotFoundException(payload.getBookingId());
        });

        // Store old times for the event
        Instant oldStartTime = appointmentToUpdate.getStartTime();
        Instant oldEndTime = appointmentToUpdate.getEndTime();

        // Parse new times
        Instant newStartTime = DateUtils.parseDateTimeIso(payload.getIsoStartTime());
        Instant newEndTime = DateUtils.parseDateTimeIso(payload.getIsoEndTime());

        // 1. Update the existing Appointment record (SYNC)
        appointmentToUpdate.setStartTime(newStartTime);
        appointmentToUpdate.setEndTime(newEndTime);
        appointmentToUpdate.setStatus(Appointment.Status.UPCOMING);
        appointmentToUpdate.setUpdatedAt(Instant.now());
        appointmentRepository.save(appointmentToUpdate);

        // 2. Record Reschedule History
        RescheduleHistory rh = RescheduleHistory.builder()
                .oldAppointment(appointmentToUpdate)
                .newAppointment(appointmentToUpdate)
                .rescheduledAt(Instant.now())
                .build();
        rescheduleHistoryRepository.save(rh);

        // 3. Publish Events (old slot cancelled, new slot booked)
        SlotCancelledEvent oldSlotEvent = new SlotCancelledEvent(appointmentToUpdate.getId(), appointmentToUpdate.getDoctor().getId(), oldStartTime, oldEndTime);
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.cancelled", oldSlotEvent);

        SlotBookedEvent newSlotEvent = new SlotBookedEvent(appointmentToUpdate.getId(), appointmentToUpdate.getZohoId(), appointmentToUpdate.getDoctor().getId(), appointmentToUpdate.getPatient().getId(), newStartTime, newEndTime);
        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.booked", newSlotEvent);

        log.info("Zoho Reschedule sync successful for Zoho ID: {}. New time: {}-{}", payload.getBookingId(), newStartTime, newEndTime);
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