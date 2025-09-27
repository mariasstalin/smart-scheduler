package com.smartscheduler.appointment.service;

import com.smartscheduler.appointment.model.Appointment;
import com.smartscheduler.appointment.repo.AppointmentRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class AppointmentService {

    private static final Logger log = LoggerFactory.getLogger(AppointmentService.class);

    private final AppointmentRepository repo;

    public AppointmentService(AppointmentRepository repo) {
        this.repo = repo;
    }

    // Create a new appointment
    public Appointment create(Appointment appointment) {
        log.info("Creating appointment for user_id={} at {}", appointment.getUserId(), appointment.getStartTime());
        Appointment saved = repo.save(appointment);
        log.debug("Appointment created: {}", saved);
        return saved;
    }

    // Find appointment by ID
    public Optional<Appointment> findById(Long id) {
        log.info("Looking up appointment with id={}", id);
        Optional<Appointment> found = repo.findById(id);
        if (found.isPresent()) {
            log.debug("Appointment found: {}", found.get());
        } else {
            log.warn("Appointment with id={} not found", id);
        }
        return found;
    }

    // List all appointments
    public List<Appointment> listAll() {
        log.info("Fetching all appointments from database");
        List<Appointment> all = repo.findAll();
        log.debug("Total appointments retrieved: {}", all.size());
        return all;
    }

    // ✅ Delete/cancel appointment
    public boolean delete(Long id) {
        log.info("Attempting to cancel appointment with id={}", id);
        if (repo.existsById(id)) {
            repo.deleteById(id);
            log.info("Successfully cancelled appointment with id={}", id);
            return true;
        } else {
            log.warn("Appointment with id={} not found for cancellation", id);
            return false;
        }
    }

    // ✅ Reschedule appointment
    public Optional<Appointment> reschedule(Long id, Appointment updated) {
        log.info("Rescheduling appointment with id={}", id);
        return repo.findById(id).map(existing -> {
            log.debug("Existing appointment before reschedule: {}", existing);

            // Update fields
            existing.setStartTime(updated.getStartTime());
            existing.setEndTime(updated.getEndTime());
            //existing.setTitle(updated.getTitle());
            //existing.setDescription(updated.getDescription());
            existing.setStatus("RESCHEDULED");

            Appointment saved = repo.save(existing);
            log.info("Rescheduled appointment id={} to start_time={}", id, saved.getStartTime());
            log.debug("Updated appointment details: {}", saved);
            return saved;
        });
    }
}
