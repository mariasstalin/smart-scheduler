
package com.smartscheduler.notification.web;

import com.smartscheduler.notification.model.Notification;
import com.smartscheduler.notification.service.NotificationService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/notifications")
public class NotificationController {
    private final NotificationService svc;

    public NotificationController(NotificationService svc) {
        this.svc = svc;
    }

    @GetMapping("/user/{userId}")
    public ResponseEntity<?> getForUser(@PathVariable Long userId) {
        List<Notification> list = svc.findByUserId(userId);
        return ResponseEntity.ok(list);
    }
}
