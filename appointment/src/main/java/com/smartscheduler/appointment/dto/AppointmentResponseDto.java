package com.smartscheduler.appointment.dto;

import com.smartscheduler.common.entity.Appointment;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

/**
 * Data Transfer Object (DTO) specifically for responses sent back to the Rasa action server.
 * This ensures the keys and format match Rasa's expectations (e.g., appointment_id, current_time).
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AppointmentResponseDto {

    private String appointmentId;

    private String service;

    private String doctor;

    private String currentTime;

    public AppointmentResponseDto(Appointment appointment) {
        // Use the Entity's ID
        this.appointmentId = String.valueOf(appointment.getId());

        // Assuming 'service' is mapped to the Doctor's specialization
        this.service = appointment.getDoctor().getSpecialization();

        this.doctor = appointment.getDoctor().getName();

        // Crucially, format the time correctly for Rasa
        this.currentTime = appointment.getStartTimeLocal()
                .format(java.time.format.DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"));
    }
}
