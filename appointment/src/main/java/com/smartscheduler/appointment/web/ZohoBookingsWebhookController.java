package com.smartscheduler.appointment.web;

import com.smartscheduler.appointment.payload.BookAppointmentPayload;
import com.smartscheduler.appointment.payload.CancelAppointmentPayload;
import com.smartscheduler.appointment.payload.RescheduleAppointmentPayload;
import com.smartscheduler.appointment.service.AppointmentService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhooks/zoho-bookings")
@RequiredArgsConstructor
public class ZohoBookingsWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(ZohoBookingsWebhookController.class);

    private final AppointmentService appointmentService;

    @PostMapping("/book")
    public ResponseEntity<String> book(@RequestBody BookAppointmentPayload payload) {
        logger.info("Received Zoho webhook for booking: {}", payload);
        appointmentService.handleBooking(payload);
        return ResponseEntity.ok("Booking webhook processed");
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancel(@RequestBody CancelAppointmentPayload payload) {
        logger.info("Received Zoho webhook for cancel: {}", payload);
        appointmentService.handleCancellation(payload);
        return ResponseEntity.ok("Cancellation webhook processed");
    }

    @PostMapping("/reschedule")
    public ResponseEntity<String> reschedule(@RequestBody RescheduleAppointmentPayload payload) {
        //appointmentService.handleReschedule(payload);
        return ResponseEntity.ok("Reschedule webhook processed");
    }
}
