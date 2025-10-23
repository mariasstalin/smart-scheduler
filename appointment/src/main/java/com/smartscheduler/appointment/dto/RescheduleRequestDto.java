package com.smartscheduler.appointment.dto;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class RescheduleRequestDto {
    private String oldAppointmentId;
    private String newDatetime;
    private String action;
}
