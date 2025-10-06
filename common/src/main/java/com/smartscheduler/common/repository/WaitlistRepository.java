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

public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    List<Waitlist> findByDoctorAndActiveTrue(Doctor doctor);
    List<Waitlist> findByDoctorAndActiveFalse(Doctor doctor);
    List<Waitlist> findByPatientAndActiveTrue(Patient patient);
}


