
package com.smartscheduler.audit.web;

import com.smartscheduler.audit.model.AuditLog;
import com.smartscheduler.audit.repo.AuditRepository;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ResponseEntity;

import java.util.List;

@RestController
@RequestMapping("/audit")
public class AuditController {
    private final AuditRepository repo;

    public AuditController(AuditRepository repo) {
        this.repo = repo;
    }

    @GetMapping
    public ResponseEntity<?> list() {
        return ResponseEntity.ok(repo.findAll());
    }
}
