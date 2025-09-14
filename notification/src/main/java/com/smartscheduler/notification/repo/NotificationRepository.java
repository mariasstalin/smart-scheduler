
package com.smartscheduler.notification.repo;

import com.smartscheduler.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;

public interface NotificationRepository extends JpaRepository<Notification, Long> {
}
