package com.smartscheduler.common.repository;

import com.smartscheduler.common.entity.Appointment;
import com.smartscheduler.common.entity.Doctor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface AppointmentRepository extends JpaRepository<Appointment, Long> {
    List<Appointment> findByDoctorAndStartTimeAfterAndStatus(Doctor doctor, LocalDateTime after, Appointment.Status status);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status = :status AND ((a.startTime < :end AND a.endTime > :start))")
    List<Appointment> findConflictingAppointments(@Param("doctorId") Long doctorId, @Param("start") Instant start, @Param("end") Instant end, @Param("status") Appointment.Status status);

    Optional<Appointment> findByZohoId(String bookingId);
}

