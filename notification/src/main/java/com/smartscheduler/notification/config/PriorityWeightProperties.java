package com.smartscheduler.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "notification.patient.priority.weights")
@Data
public class PriorityWeightProperties {

    private double isVip;
    private double severityLevel;
    private double acceptanceRate;
    private double timeDifference;
    private double timeConsistencyStdDev;

}
