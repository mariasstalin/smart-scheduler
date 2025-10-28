package com.smartscheduler.notification.service;

import com.smartscheduler.common.entity.Patient;

public interface PriorityCalculator {

    double calculateScore(Patient patient);
}
