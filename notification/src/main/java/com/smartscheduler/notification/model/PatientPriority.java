package com.smartscheduler.notification.model;

import com.smartscheduler.common.entity.Patient;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

@Data
public class PatientPriority {

    private Long id;
    private Boolean isVip;
    private Integer severityLevel;
    private Integer totalNotificationsResponded;
    private Integer totalNotificationsSent;
    private List<LocalDateTime> bookingHistory;
    private Double score;

    public PatientPriority() {

    }

    public PatientPriority(Patient patient) {
        this.id = patient.getId();
        this.isVip = patient.getVip();
        this.severityLevel = patient.getSeverityLevel();
        this.totalNotificationsResponded = patient.getTotalNotificationsResponded();
        this.totalNotificationsSent = patient.getTotalNotificationsSent();
    }
}
