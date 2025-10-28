package com.smartscheduler.appointment.dto;

import com.smartscheduler.common.entity.Appointment;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

@Data
@NoArgsConstructor
public class RescheduleResponseDto {

    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

    // Status can be "SUCCESS", "SLOT_TAKEN", or other error codes
    private String status;

    private String newAppointmentTime;

    public RescheduleResponseDto(String status) {
        this.status = status;
    }
}
