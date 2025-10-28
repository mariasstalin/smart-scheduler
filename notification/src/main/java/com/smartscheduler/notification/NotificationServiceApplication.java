
package com.smartscheduler.notification;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.retry.annotation.EnableRetry;

@EnableFeignClients
@SpringBootApplication(scanBasePackages = {
        "com.smartscheduler.notification",
        "com.smartscheduler.common"
})
@EnableJpaRepositories(basePackages = {
        "com.smartscheduler.common.repository"
})
@EntityScan(basePackages = {
        "com.smartscheduler.common.entity"
})
public class NotificationServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(NotificationServiceApplication.class, args);
    }
}
