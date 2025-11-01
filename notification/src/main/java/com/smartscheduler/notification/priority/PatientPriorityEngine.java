package com.smartscheduler.notification.priority;

import com.smartscheduler.notification.model.PatientPriority;

import java.time.LocalDateTime;
import java.util.List;

public interface PatientPriorityEngine {

    List<PatientPriority> rankPatientsByPriority(LocalDateTime cancelledSlot, List<PatientPriority> patients);
}

