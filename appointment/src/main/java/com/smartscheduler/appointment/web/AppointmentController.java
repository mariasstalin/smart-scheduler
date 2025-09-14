
package com.smartscheduler.appointment.web;

import com.smartscheduler.appointment.model.Appointment;
import com.smartscheduler.appointment.service.AppointmentService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/appointments")
public class AppointmentController {
    private final AppointmentService svc;

    public AppointmentController(AppointmentService svc) {
        this.svc = svc;
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody Appointment ap) {
        Appointment out = svc.create(ap);
        return ResponseEntity.ok(out);
    }

    @GetMapping("/{id}")
    public ResponseEntity<?> get(@PathVariable Long id) {
        return svc.findById(id).map(a -> ResponseEntity.ok(a)).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @GetMapping
    public ResponseEntity<List<Appointment>> list() {
        return ResponseEntity.ok(svc.listAll());
    }
}
