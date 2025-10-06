package com.smartscheduler.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SlotBookedEvent {
    private Long appointmentId;
    private String zohoId;
    private Long doctorId;
    private Long patientId;
    private Instant startTime;
    private Instant endTime;

    public LocalDateTime getStartTimeLocal() {
        return LocalDateTime.ofInstant(startTime, ZoneId.of("UTC"));
    }

    public LocalDateTime getEndTimeLocal() {
        return LocalDateTime.ofInstant(endTime, ZoneId.of("UTC"));
    }
}
