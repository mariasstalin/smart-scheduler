package com.smartscheduler.common.repository;

import com.smartscheduler.common.entity.Appointment;
import com.smartscheduler.common.entity.Doctor;
import com.smartscheduler.common.entity.Patient;
import com.smartscheduler.common.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {

    @Query("SELECT w FROM Waitlist w JOIN FETCH w.preferredDates WHERE w.doctor = :doctor AND w.active = true")
    List<Waitlist> findByDoctorAndActiveTrueWithPreferredDates(@Param("doctor") Doctor doctor);

    @Query("SELECT w FROM Waitlist w JOIN FETCH w.preferredDates WHERE w.doctor = :doctor AND w.active = false")
    List<Waitlist> findByDoctorAndActiveFalseWithPreferredDates(@Param("doctor") Doctor doctor);

    //List<Waitlist> findByPatientAndActiveTrue(Patient patient);

    @Query("SELECT w FROM Waitlist w JOIN FETCH w.preferredDates WHERE w.patient = :patient AND w.active = true")
    List<Waitlist> findByPatientAndActiveTrueWithPreferredDates(@Param("patient") Patient patient);

}


