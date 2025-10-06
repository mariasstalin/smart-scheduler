package com.smartscheduler.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotCancelledEvent implements Serializable {
    private Long appointmentId;
    private Long doctorId;
    private Instant startTime;
    private Instant endTime;
    private ZoneId timeZoneId;

    public LocalDateTime getStartTimeLocal() {
        return LocalDateTime.ofInstant(startTime, ZoneId.of("UTC"));
    }

    public LocalDateTime getEndTimeLocal() {
        return LocalDateTime.ofInstant(endTime, ZoneId.of("UTC"));
    }
}

