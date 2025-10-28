package com.smartscheduler.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.patient.priority.weights")
@Data
public class ScoringProperties {

    private double severityLevel = 8.0;

    private double visitCountMaxScore = 10.0;
    private double visitCountDivisor = 5.0;

    private double vip = 20;

    private double responseRateWeight = 20.0;
    private double consecutiveMissPenalty = 5.0;
    private double consecutiveMissMaxPenalty = 15.0;

}
