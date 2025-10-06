package com.smartscheduler.appointment.entity;

import jakarta.persistence.*;
import lombok.*;
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
    private Long rescheduleId;

    @ManyToOne
    @JoinColumn(name = "old_appointment_id", nullable = false)
    private Appointment oldAppointment;

    @ManyToOne
    @JoinColumn(name = "new_appointment_id", nullable = false)
    private Appointment newAppointment;

    private LocalDateTime rescheduledAt;
}

