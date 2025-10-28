package com.smartscheduler.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Builder.Default;

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

    private String name;

    private String email;

    private String phone;

    @Default
    private String timeZone = "UTC";

    private Instant createdAt;

    @Default
    private Boolean vip = false;

    @Default
    private Integer severityLevel = 1;         // 1–10

    @Default
    private Integer visitCount = 0;            // number of visits in last year

    @Default
    private Integer totalNotificationsSent = 0;

    @Default
    private Integer totalNotificationsResponded = 0;

    @Default
    private Integer consecutiveMisses = 0;

    private LocalDateTime inactiveUntil;       // temporarily inactive

    private LocalDateTime lastNotifiedAt;      // timestamp of last notification

    private String staffNotes;

    @PrePersist
    protected void onCreate() {
        if (this.createdAt == null) {
            this.createdAt = Instant.now();
        }
    }

    public ZoneId getTimeZoneId() {
        return ZoneId.of(timeZone);
    }

}