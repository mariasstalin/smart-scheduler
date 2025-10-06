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

    @PostMapping("/reschedule")
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
}
