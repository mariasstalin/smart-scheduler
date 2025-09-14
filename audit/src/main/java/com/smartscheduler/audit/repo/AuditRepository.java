
package com.smartscheduler.audit.repo;

import com.smartscheduler.audit.model.AuditLog;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuditRepository extends JpaRepository<AuditLog, Long> {
}
