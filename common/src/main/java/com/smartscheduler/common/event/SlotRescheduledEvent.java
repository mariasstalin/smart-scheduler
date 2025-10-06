package com.smartscheduler.common.event;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SlotRescheduledEvent implements Serializable {
    private Long notificationId;
    private Long appointmentId;
    private Long patientId;
    private Long doctorId;
    private Instant startTime;
    private Instant endTime;

    public LocalDateTime getStartTimeLocal() {
        return LocalDateTime.ofInstant(startTime, ZoneId.of("UTC"));
    }

    public LocalDateTime getEndTimeLocal() {
        return LocalDateTime.ofInstant(endTime, ZoneId.of("UTC"));
    }
}

