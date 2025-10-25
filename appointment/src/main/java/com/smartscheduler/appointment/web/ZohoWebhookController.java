package com.smartscheduler.appointment.web;

import com.smartscheduler.appointment.payload.BookAppointmentPayload;
import com.smartscheduler.appointment.payload.CancelAppointmentPayload;
import com.smartscheduler.appointment.payload.RescheduleAppointmentPayload;
import com.smartscheduler.appointment.service.WebhookSyncService;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/webhook/zoho")
@RequiredArgsConstructor
public class ZohoWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(ZohoWebhookController.class);

    private final WebhookSyncService webhookSyncService;

    @PostMapping("/book")
    public ResponseEntity<String> book(@RequestBody BookAppointmentPayload payload) {
        logger.info("Received Zoho webhook for booking: {}", payload.getBookingId());
        webhookSyncService.handleBooking(payload);
        return ResponseEntity.ok("Booking webhook processed");
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancel(@RequestBody CancelAppointmentPayload payload) {
        logger.info("Received Zoho webhook for cancellation: {}", payload.getBookingId());
        webhookSyncService.handleCancellation(payload);
        return ResponseEntity.ok("Cancellation webhook processed");
    }

    @PostMapping("/reschedule")
    public ResponseEntity<String> reschedule(@RequestBody RescheduleAppointmentPayload payload) {
        logger.info("Received Zoho webhook for reschedule: {}", payload.getBookingId());
        webhookSyncService.handleReschedule(payload);
        return ResponseEntity.ok("Reschedule webhook processed");
    }
}