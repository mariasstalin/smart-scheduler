package com.smartscheduler.notification.repository;

import com.smartscheduler.common.entity.Appointment;
import com.smartscheduler.common.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    // future appointments for same day: startTime > cancelledSlot and startTime on same date
    List<Appointment> findByDoctorAndStartTimeAfterAndStatus(Doctor doctor, LocalDateTime startTime, Appointment.Status status);
}


