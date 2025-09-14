
package com.smartscheduler.notification.service;

import com.smartscheduler.notification.model.Notification;
import com.smartscheduler.notification.repo.NotificationRepository;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class NotificationService {
    private final NotificationRepository repo;

    public NotificationService(NotificationRepository repo) {
        this.repo = repo;
    }

    public Notification save(Notification n) {
        return repo.save(n);
    }

    @Cacheable(value = "notifications", key = "#userId")
    public List<Notification> findByUserId(Long userId) {
        // naive implementation: fetch all and filter
        return repo.findAll().stream().filter(n -> n.getUserId() != null && n.getUserId().equals(userId)).toList();
    }
}
