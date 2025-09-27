package com.smartscheduler.appointment.web;

import com.smartscheduler.appointment.model.Appointment;
import com.smartscheduler.appointment.service.AppointmentService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class AppointmentController {

    private static final Logger log = LoggerFactory.getLogger(AppointmentController.class);

    private final AppointmentService svc;

    public AppointmentController(AppointmentService svc) {
        this.svc = svc;
    }

    // ✅ Create appointment
    @PostMapping("/book")
    public ResponseEntity<Appointment> create(@RequestBody Appointment appointment) {
        log.info("Received request to book appointment for user_id={} at {}",
                appointment.getUserId(), appointment.getStartTime());
        Appointment saved = svc.create(appointment);
        log.debug("Saved appointment details: {}", saved);
        return ResponseEntity.ok(saved);
    }

    // ✅ Get one appointment
    @GetMapping("/{id}")
    public ResponseEntity<Appointment> get(@PathVariable Long id) {
        log.info("Fetching appointment with id={}", id);
        return svc.findById(id)
                .map(a -> {
                    log.debug("Found appointment: {}", a);
                    return ResponseEntity.ok(a);
                })
                .orElseGet(() -> {
                    log.warn("Appointment with id={} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }

    // ✅ List all appointments
    @GetMapping
    public ResponseEntity<List<Appointment>> list() {
        log.info("Fetching all appointments");
        List<Appointment> all = svc.listAll();
        log.debug("Total appointments found: {}", all.size());
        return ResponseEntity.ok(all);
    }

    // ✅ Cancel appointment
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> cancel(@PathVariable Long id) {
        log.info("Request to cancel appointment with id={}", id);
        boolean deleted = svc.delete(id);
        if (deleted) {
            log.info("Successfully cancelled appointment with id={}", id);
            return ResponseEntity.noContent().build();
        } else {
            log.warn("Attempted to cancel non-existing appointment with id={}", id);
            return ResponseEntity.notFound().build();
        }
    }

    // ✅ Reschedule appointment
    @PutMapping("/{id}/reschedule")
    public ResponseEntity<Appointment> reschedule(
            @PathVariable Long id,
            @RequestBody Appointment updated) {
        log.info("Request to reschedule appointment id={} to start_time={}", id, updated.getStartTime());
        return svc.reschedule(id, updated)
                .map(a -> {
                    log.info("Successfully rescheduled appointment id={}", id);
                    log.debug("Updated appointment: {}", a);
                    return ResponseEntity.ok(a);
                })
                .orElseGet(() -> {
                    log.warn("Failed to reschedule. Appointment id={} not found", id);
                    return ResponseEntity.notFound().build();
                });
    }
}
