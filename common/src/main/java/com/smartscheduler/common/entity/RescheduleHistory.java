package com.smartscheduler.common.entity;

import jakarta.persistence.*;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.Instant;
import java.time.LocalDateTime;

@Entity
@Table(name = "reschedule_history")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RescheduleHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "old_appointment_id", nullable = false)
    private Appointment oldAppointment;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "new_appointment_id", nullable = false)
    private Appointment newAppointment;

    private Instant rescheduledAt;
}

