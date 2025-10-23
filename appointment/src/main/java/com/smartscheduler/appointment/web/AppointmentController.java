package com.smartscheduler.appointment.web;

import com.smartscheduler.appointment.service.AppointmentService;
import com.smartscheduler.common.event.SlotRescheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequiredArgsConstructor
@Slf4j
@RequestMapping("/api")
public class AppointmentController {

    private final AppointmentService appointmentService;

    @PostMapping("/reschedule-old")
    public ResponseEntity<?> reschedule(@RequestBody SlotRescheduledEvent rescheduledEvent) {
        try {
            //appointmentService.performReschedule(rescheduledEvent.getNotificationId(), rescheduledEvent.getOldAppointmentId(), rescheduledEvent.getPatientId(), rescheduledEvent.getDoctorId(), rescheduledEvent.getRequestedStartIso(), rescheduledEvent.getRequestedEndIso());
            return ResponseEntity.ok().build();
        } catch (IllegalStateException e) {
            // slot unavailable
            return ResponseEntity.status(HttpStatus.CONFLICT).body(Map.of("error", "slot-unavailable", "message", e.getMessage()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(Map.of("error", "bad-request", "message", e.getMessage()));
        } catch (Exception e) {
            log.error("Reschedule failed", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(Map.of("error", "server_error"));
        }
    }

    @PostMapping("/cancel")
    public ResponseEntity<String> cancelAppointment(@RequestBody Map<String, String> cancellationRequest) {
        String conversationId = cancellationRequest.get("conversationId");
        String appointmentTime = cancellationRequest.get("appointmentTime");

        // LOGIC:
        // 1. Validate if the appointment (identified by user/time) exists.
        // 2. If valid, call Zoho Bookings API to cancel the booking.
        // 3. Trigger Proactive Notification logic to fill the new empty slot.

        if (appointmentTime.contains("invalid")) {
            // Mocking a business failure (e.g., Appointment Not Found)
            return ResponseEntity.status(404).body("Appointment not found for the given criteria.");
        }

        System.out.println("Appointment CANCELLED for User: " + conversationId + " at " + appointmentTime);

        // Success response expected by Rasa
        return ResponseEntity.ok("Appointment successfully cancelled.");
    }

    @PostMapping("/reschedule")
    public ResponseEntity<String> rescheduleAppointment(@RequestBody Map<String, String> rescheduleRequest) {
        String conversationId = rescheduleRequest.get("conversationId");
        String oldTime = rescheduleRequest.get("oldAppointmentTime");
        String newTime = rescheduleRequest.get("newAppointmentTime");

        // LOGIC:
        // 1. Check New Slot Availability using Zoho Bookings API.
        // 2. If available, call Zoho Bookings API to perform the reschedule/update.
        // 3. Handle payment/policy checks if required.

        if (newTime.contains("Tuesday") && newTime.contains("4pm")) {
            // Mocking a business failure (e.g., Slot not available)
            Map<String, String> error = Map.of(
                    "error", "SLOT_UNAVAILABLE",
                    "message", "the requested slot on Tuesday at 4pm is currently unavailable."
            );
            return ResponseEntity.status(400).body(error.toString());
        }

        System.out.printf("Appointment RESCHEDULED for User %s from %s to %s%n",
                conversationId, oldTime, newTime);

        // Success response expected by Rasa
        return ResponseEntity.ok("Appointment successfully rescheduled.");
    }

}
