
package com.smartscheduler.audit.model;

import jakarta.persistence.*;

import java.time.LocalDateTime;

@Entity
@Table(name = "audit_logs")
public class AuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String entityType;
    private String action;
    @Column(columnDefinition = "TEXT")
    private String payload;
    private Long userId;
    private LocalDateTime createdAt = LocalDateTime.now();

    // getters/setters
    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getEntityType() {
        return entityType;
    }

    public void setEntityType(String e) {
        this.entityType = e;
    }

    public String getAction() {
        return action;
    }

    public void setAction(String a) {
        this.action = a;
    }

    public String getPayload() {
        return payload;
    }

    public void setPayload(String p) {
        this.payload = p;
    }

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long u) {
        this.userId = u;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime c) {
        this.createdAt = c;
    }
}
