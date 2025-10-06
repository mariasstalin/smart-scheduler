package com.smartscheduler.common.event;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SlotCancelledEvent implements Serializable {
    private Long appointmentId;
    private Long doctorId;
    private Instant startTime;
    private Instant endTime;
}

