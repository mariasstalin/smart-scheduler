package com.smartscheduler.common.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "patients")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Patient {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String email;
    private String phone;
    private Instant createdAt;
    private String timeZone;

    // VIP flags
    private Boolean highNetWorth;
    private Boolean highProfile;
    private Boolean frequentVisitor;
    private Boolean specialMedicalNeeds;

    // Historical responsiveness (number of YES responses / total notifications)
    private Integer totalNotificationsSent;
    private Integer totalNotificationsResponded;

    @Transient
    public double getPriorityScore() {
        double score = 0.0;

        // VIP attributes weight
        if (highNetWorth) score += 30;
        if (highProfile) score += 25;
        if (frequentVisitor) score += 20;
        if (specialMedicalNeeds) score += 25;

        // Responsiveness weight
        double responsivenessScore = 0.0;
        if (totalNotificationsSent > 0) {
            responsivenessScore = ((double) totalNotificationsResponded / totalNotificationsSent) * 20;
        }
        score += responsivenessScore;

        // Ensure score is capped at 100
        return Math.min(score, 100);
    }

    public double getConsecutiveMisses() {

    }
}

