package com.smartscheduler.appointment.service;

import com.smartscheduler.appointment.exception.AppointmentNotFoundException;
import com.smartscheduler.common.entity.Appointment;
import com.smartscheduler.common.event.SlotDeniedEvent;
import com.smartscheduler.common.event.SlotReallocatedEvent;
import com.smartscheduler.common.repository.AppointmentRepository;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalActionService {

    private final AppointmentRepository appointmentRepository;
    private final ZohoApiService zohoApiService;
    private final RabbitTemplate rabbitTemplate;

    public List<Appointment> getAppointmentsByPhoneNumber(String phoneNumber) {
        return appointmentRepository.findByPatientPhoneAndStatus(phoneNumber, Appointment.Status.UPCOMING);
    }

    public Appointment findById(String appointmentId) {
        try {
            Long id = Long.valueOf(appointmentId);
            return appointmentRepository.findById(id)
                    .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));
        } catch (NumberFormatException e) {
            throw new AppointmentNotFoundException(appointmentId);
        }
    }

    @Transactional
    public void reschedule(Long notificationId, Long appointmentId, String newDatetimeString) {
        Appointment oldAppointment = appointmentRepository.findById(appointmentId).orElseThrow(() -> new AppointmentNotFoundException(String.valueOf(appointmentId)));

        if (oldAppointment.getSource() == Appointment.Source.ZOHO && oldAppointment.getZohoId() != null) {
            log.info("Attempting Zoho sync reschedule for local ID: {}", oldAppointment);

            try {
                zohoApiService.rescheduleAppointmentInZoho(oldAppointment.getZohoId(), newDatetimeString, oldAppointment.getDoctor().getZohoId());

                rabbitTemplate.convertAndSend("appointments.exchange", "appointments.slots.reallocated", new SlotReallocatedEvent(notificationId));

                log.info("Appointment saved and SlotBookedEvent published: {}", oldAppointment.getId());
            } catch (Exception e) {
                log.error("Failed to call Zoho reschedule API for {}.", oldAppointment.getZohoId(), e);
                // Rollback the transaction if external sync fails
                throw new RuntimeException("Reschedule failed: Could not synchronize with Zoho.");
            }
        }
    }

    @Transactional
    public void cancel(String appointmentId) {
        Long id;
        try {
            id = Long.valueOf(appointmentId);
        } catch (NumberFormatException e) {
            throw new AppointmentNotFoundException(appointmentId);
        }

        Appointment appointment = appointmentRepository.findById(id).orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        if (appointment.getSource() == Appointment.Source.ZOHO && appointment.getZohoId() != null) {
            log.info("Attempting Zoho sync cancellation for local ID: {}", id);
            try {
                zohoApiService.cancelAppointmentInZoho(appointment.getZohoId());
            } catch (Exception e) {
                log.error("Failed to call Zoho cancel API for {}.", appointment.getZohoId(), e);
                throw new RuntimeException("Cancellation failed: Could not synchronize with Zoho.");
            }
        } else {
            log.warn("Skipping Zoho cancellation for non-Zoho or missing Zoho ID appointment: {}", id);
        }
    }

    public void denyOfferStatus(Long notificationId, Long appointmentId) {

        rabbitTemplate.convertAndSend("appointments.exchange", "appointments.slots.denied", new SlotDeniedEvent(notificationId));
        log.info("Slot offer denied by patient for old appointment ID: {}", appointmentId);
    }
}