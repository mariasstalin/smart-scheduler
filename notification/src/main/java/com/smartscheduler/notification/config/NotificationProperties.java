package com.smartscheduler.notification.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "notification.scheduling")
@Data
public class NotificationProperties {

    private long responseWindowMinutes = 1;

    private int maxConsecutiveMisses = 3;

    private Duration optOutDuration = Duration.ofHours(24);


    public Duration getResponseWindowDuration() {
        return Duration.ofMinutes(this.responseWindowMinutes);
    }
}