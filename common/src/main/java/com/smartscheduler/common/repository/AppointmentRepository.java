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
    List<Appointment> findByDoctorAndStartTimeAfterAndStatus(Doctor doctor, Instant after, Appointment.Status status);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status = :status AND ((a.startTime < :end AND a.endTime > :start))")
    List<Appointment> findConflictingAppointments(@Param("doctorId") Long doctorId, @Param("start") Instant start, @Param("end") Instant end, @Param("status") Appointment.Status status);

    Optional<Appointment> findByZohoId(String bookingId);

    @Query("SELECT a FROM Appointment a JOIN a.patient p WHERE p.phone = :phone AND a.status = :status")
    List<Appointment> findByPatientPhoneAndStatus(@Param("phone") String phone, @Param("status") Appointment.Status status);

    @Query("SELECT a FROM Appointment a WHERE a.doctor.id = :doctorId AND a.status = :status AND (" +
            " (a.startTime < :newEndTime AND a.endTime > :newStartTime) " + // Overlap condition
            ")")
    List<Appointment> findConflictingAppointmentsByDoctorId(@Param("doctorId") Long doctorId,
                                                            @Param("newStartTime") Instant newStartTime,
                                                            @Param("newEndTime") Instant newEndTime,
                                                            @Param("status") Appointment.Status status);

    List<Appointment> findByDoctorAndStartTimeAfterAndStartTimeBeforeAndStatus(Doctor doctor, Instant instant, Instant futureSearchLimit, Appointment.Status status);
}

