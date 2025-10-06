package com.smartscheduler.common.repository;

import com.smartscheduler.common.entity.RescheduleHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RescheduleHistoryRepository extends JpaRepository<RescheduleHistory, Long> {
    Optional<RescheduleHistory> findByNotificationId(Long notificationId);
}

