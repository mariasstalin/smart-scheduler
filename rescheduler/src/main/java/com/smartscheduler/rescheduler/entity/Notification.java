package com.smartscheduler.appointment.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long notificationId;

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

    private LocalDateTime sentAt;

    @Enumerated(EnumType.STRING)
    private Response response;

    private LocalDateTime expiresAt;

    private Channel channel;

    public enum NotificationType {
        SLOT_OPEN, REMINDER, CONFIRMATION
    }

    public enum Status {
        PENDING, SENT, RESPONDED, EXPIRED
    }

    public enum Response {
        YES, NO
    }

    private enum Channel {
        WHATSAPP, SMS, EMAIL
    }

}

