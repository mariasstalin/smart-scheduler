
package com.smartscheduler.notification.web;

import com.smartscheduler.notification.model.Notification;
import com.smartscheduler.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService svc;

    public NotificationController(NotificationService svc) {
        this.svc = svc;
    }

    @PostMapping("/slot/confirmed")
    public ResponseEntity<String> confirmNewSlot(@RequestBody Map<String, String> confirmationRequest) {
        String conversationId = confirmationRequest.get("conversationId");
        String newSlotTime = confirmationRequest.get("newSlotTime");

        // LOGIC:
        // 1. Call Zoho Bookings API to finalize the booking for newSlotTime.
        // 2. Update database/cache with confirmed appointment.
        // 3. Send final confirmation message (via WhatsApp/SMS) using the Notification Service logic.

        System.out.println("Slot CONFIRMED for User: " + conversationId + " at " + newSlotTime);

        // Success response expected by Rasa
        return ResponseEntity.ok("Slot confirmed and user notified.");
    }

    /**
     * Endpoint called by Rasa's ActionHandleSlotConfirmation when the user denies a new slot.
     */
    @PostMapping("/slot/rejected")
    public ResponseEntity<String> rejectNewSlot(@RequestBody Map<String, String> rejectionRequest) {
        String conversationId = rejectionRequest.get("conversationId");
        String newSlotTime = rejectionRequest.get("newSlotTime");

        // LOGIC:
        // 1. Update internal system/Zoho Bookings to release the hold on the slot.
        // 2. Send rejection confirmation to the user.

        System.out.println("Slot REJECTED for User: " + conversationId + " at " + newSlotTime);

        // Success response expected by Rasa
        return ResponseEntity.ok("Slot rejected and hold released.");
    }
}
