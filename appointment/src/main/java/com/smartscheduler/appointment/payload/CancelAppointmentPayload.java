package com.smartscheduler.appointment.payload;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class CancelAppointmentPayload {
    private String bookingId;
    private String notes;
}
