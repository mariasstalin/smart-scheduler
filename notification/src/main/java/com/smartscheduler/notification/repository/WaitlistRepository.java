package com.smartscheduler.notification.repository;

import com.smartscheduler.common.entity.Doctor;
import com.smartscheduler.common.entity.Waitlist;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

@Repository
public interface WaitlistRepository extends JpaRepository<Waitlist, Long> {
    List<Waitlist> findByDoctorAndPreferredDateAndNotifyOnSlotOpenOrderByCreatedAtAsc(Doctor doctor, LocalDate preferredDate, Boolean notifyOnSlotOpen);

    List<Waitlist> findByDoctorAndActiveTrue(Doctor doctor);

    List<Waitlist> findByDoctorAndActiveFalse(Doctor doctor);
}


