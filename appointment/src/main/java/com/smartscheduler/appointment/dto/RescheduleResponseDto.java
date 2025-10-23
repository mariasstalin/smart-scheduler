package com.smartscheduler.appointment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleResponseDto {

    // Status can be "SUCCESS", "SLOT_TAKEN", or other error codes
    private String status;

    private String newAppointmentId;
    private String newAppointmentTime;
}
