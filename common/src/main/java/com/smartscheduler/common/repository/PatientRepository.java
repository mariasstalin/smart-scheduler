package com.smartscheduler.common.repository;

import com.smartscheduler.common.entity.Patient;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface PatientRepository extends JpaRepository<Patient, Long> {
    Optional<Patient> findByEmail(String email);

    Optional<Patient> findByPhone(String phone);

    @Modifying
    @Query("UPDATE Patient p SET p.totalNotificationsSent = p.totalNotificationsSent + 1 WHERE p.id = :patientId")
    void incrementTotalNotificationsSent(@Param("patientId") Long patientId);
}

