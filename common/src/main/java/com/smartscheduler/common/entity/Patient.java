package com.smartscheduler.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;

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

    // Basic info
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

    // Medical info
    private Integer severityLevel;             // 1–5
    private Boolean chronicCondition;          // true if patient has chronic disease
    private Integer visitCount;                // number of visits in last year
    private LocalDate lastVisitDate;          // last visit date
    private Integer waitingDays;               // days in waiting list

    // Historical responsiveness (number of YES responses / total notifications)
    private Integer totalNotificationsSent;
    private Integer totalNotificationsResponded;
    private Integer consecutiveMisses;

    // Notification / status management
    private LocalDateTime inactiveUntil;       // temporarily inactive
    private LocalDateTime lastNotifiedAt;      // timestamp of last notification

    // Optional: staff notes / remarks
    private String staffNotes;

    public ZoneId getTimeZoneId() {
        return ZoneId.of(timeZone);
    }

    // Dynamic priority score calculation
    @Transient
    public double getPriorityScore() {
        double score = 0.0;

        // 1. Medical urgency
        if (severityLevel != null) score += severityLevel * 8;  // 1–5 → 8–40
        if (Boolean.TRUE.equals(chronicCondition)) score += 15;
        if (Boolean.TRUE.equals(specialMedicalNeeds)) score += 15;

        // 2. Loyalty / history
        if (Boolean.TRUE.equals(frequentVisitor)) score += 10;
        if (visitCount != null) score += Math.min(visitCount / 5.0 * 10, 10);
        if (lastVisitDate != null) {
            long daysSinceLastVisit = java.time.temporal.ChronoUnit.DAYS.between(lastVisitDate, LocalDate.now());
            if (daysSinceLastVisit < 30) score += 5;
        }

        // 3. VIP flags
        if (Boolean.TRUE.equals(highNetWorth)) score += 10;
        if (Boolean.TRUE.equals(highProfile)) score += 10;

        // 4. Responsiveness
        if (totalNotificationsSent != null && totalNotificationsSent > 0) {
            double responseRate = ((double) totalNotificationsResponded / totalNotificationsSent) * 20;
            score += responseRate;
        }
        if (consecutiveMisses != null) score -= Math.min(consecutiveMisses * 5, 15);

        // 5. Waiting time
        if (waitingDays != null) score += Math.min(waitingDays / 3.0 * 10, 10);

        // Cap between 0–100
        return Math.max(0, Math.min(score, 100));
    }
}
