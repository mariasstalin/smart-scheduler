package com.smartscheduler.appointment.service;

import com.smartscheduler.appointment.exception.AppointmentNotFoundException;
import com.smartscheduler.common.entity.Appointment;
import com.smartscheduler.common.repository.AppointmentRepository;
import com.smartscheduler.common.util.DateUtils;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class ExternalActionService {

    private final AppointmentRepository appointmentRepository;
    private final AppointmentService appointmentService; // Use the core service

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
    public Appointment reschedule(String oldAppointmentId, String newDatetimeString) {
        Long oldId;
        try {
            oldId = Long.valueOf(oldAppointmentId);
        } catch (NumberFormatException e) {
            throw new AppointmentNotFoundException(oldAppointmentId);
        }

        Appointment oldAppointment = appointmentRepository.findById(oldId)
                .orElseThrow(() -> new AppointmentNotFoundException(oldAppointmentId));

        // Calculate new start time from Rasa's string input
        Instant newStartTime = DateUtils.parseDateTime(newDatetimeString);

        // Use the core service logic for conflict check and creation
        Appointment newAppointment = appointmentService.createNewRescheduleAppointment(oldAppointment, newStartTime);

        log.info("Rasa Reschedule successful oldId={} newId={}", oldId, newAppointment.getId());
        return newAppointment;
    }

    @Transactional
    public void cancel(String appointmentId) {
        Long id;
        try {
            id = Long.valueOf(appointmentId);
        } catch (NumberFormatException e) {
            throw new AppointmentNotFoundException(appointmentId);
        }

        Appointment appointment = appointmentRepository.findById(id)
                .orElseThrow(() -> new AppointmentNotFoundException(appointmentId));

        // Use the core service cancellation logic
        appointmentService.performCancellation(appointment);

        log.info("Rasa-driven cancellation processed for ID: {}", id);
    }

    public void denyOfferStatus(String oldId) {
        log.info("Slot offer denied by patient for old appointment ID: {}", oldId);
    }
}