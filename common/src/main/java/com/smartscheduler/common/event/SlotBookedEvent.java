package com.smartscheduler.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

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
}
