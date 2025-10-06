package com.smartscheduler.common.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    @ManyToOne
    @JoinColumn(name = "appointment_id")
    private Appointment appointment;

    @ManyToOne
    @JoinColumn(name = "doctor_id", nullable = false)
    private Doctor doctor;

    @Enumerated(EnumType.STRING)
    private NotificationType notificationType;

    @Enumerated(EnumType.STRING)
    private Status status;

    private Instant sentAt;

    private Instant startTime;

    private Instant endTime;

    @Enumerated(EnumType.STRING)
    private Response response;

    private Instant confirmedAt;

    private Instant expiresAt;

    private Channel channel;

    public LocalDateTime getStartTimeLocal() {
        return LocalDateTime.ofInstant(startTime, ZoneId.of("UTC"));
    }

    public LocalDateTime getEndTimeLocal() {
        return LocalDateTime.ofInstant(endTime, ZoneId.of("UTC"));
    }

    public enum NotificationType {
        SLOT_OPEN, REMINDER, CONFIRMATION
    }

    public enum Status {
        PENDING, SENT, RESPONDED, EXPIRED, CONFIRMED
    }

    public enum Response {
        YES, NO
    }

    private enum Channel {
        WHATSAPP, SMS, EMAIL
    }

}

