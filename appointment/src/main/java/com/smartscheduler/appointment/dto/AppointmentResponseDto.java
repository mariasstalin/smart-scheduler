package com.smartscheduler.appointment.dto;

import com.smartscheduler.common.entity.Appointment;
import com.smartscheduler.common.util.DateUtils;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;

/**
 * Data Transfer Object (DTO) specifically for responses sent back to the Rasa action server.
 * This ensures the keys and format match Rasa's expectations (e.g., appointment_id, current_time).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDto {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    private String appointmentId;

    private String service;

    private String doctor;

    private String currentTime;

    public AppointmentResponseDto(Appointment appointment) {
        this.appointmentId = String.valueOf(appointment.getId());

        this.service = appointment.getDoctor().getSpecialization();

        this.doctor = appointment.getDoctor().getName();

        ZonedDateTime zdt = ZonedDateTime.ofInstant(appointment.getStartTime(), appointment.getPatient().getTimeZoneId());
        this.currentTime = zdt.format(DATE_TIME_FORMATTER);
    }
}
