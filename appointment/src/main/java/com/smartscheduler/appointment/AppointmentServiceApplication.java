
package com.smartscheduler.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.TimeZone;

@EntityScan(basePackages = "com.smartscheduler.common.entity")
@EnableJpaRepositories(basePackages = "com.smartscheduler.common.repository")
@SpringBootApplication
public class AppointmentServiceApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }
}
