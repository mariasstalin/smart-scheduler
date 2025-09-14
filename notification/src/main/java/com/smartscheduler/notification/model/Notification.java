
package com.smartscheduler.notification.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "notifications")
public class Notification {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private Long userId;
    private String channel; // whatsapp, sms, email
    private String message;
    private boolean delivered = false;
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters/setters omitted
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long u) {
        this.userId = u;
    }

    public String getChannel() {
        return channel;
    }

    public void setChannel(String c) {
        this.channel = c;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String m) {
        this.message = m;
    }

    public boolean isDelivered() {
        return delivered;
    }

    public void setDelivered(boolean d) {
        this.delivered = d;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime t) {
        this.createdAt = t;
    }
}
