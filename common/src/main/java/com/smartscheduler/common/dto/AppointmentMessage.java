
package com.smartscheduler.common.dto;

import java.io.Serializable;
import java.time.LocalDateTime;

public class AppointmentMessage implements Serializable {
    public Long appointmentId;
    public Long userId;
    public String action; // CREATED, CANCELLED, RESCHEDULED
    public LocalDateTime startTime;
    public LocalDateTime endTime;

    public AppointmentMessage() {
    }

    public AppointmentMessage(Long appointmentId, Long userId, String action, LocalDateTime startTime, LocalDateTime endTime) {
        this.appointmentId = appointmentId;
        this.userId = userId;
        this.action = action;
        this.startTime = startTime;
        this.endTime = endTime;
    }
}
