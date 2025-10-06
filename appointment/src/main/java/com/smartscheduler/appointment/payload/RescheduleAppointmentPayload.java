package com.smartscheduler.appointment.payload;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class RescheduleAppointmentPayload {

    private String bookingId;   // same Zoho ID, but slot updated

    // New slot details
    private String startTime;
    private String endTime;
    private String isoStartTime;
    private String isoEndTime;
    private String duration;
    private String timeZone;

    private String notes;       // optional reason
}
