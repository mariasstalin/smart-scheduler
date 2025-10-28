
package com.smartscheduler.appointment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import java.util.TimeZone;

@SpringBootApplication(scanBasePackages = {
        "com.smartscheduler.appointment",
        "com.smartscheduler.common"
})
@EnableJpaRepositories(basePackages = {
        "com.smartscheduler.appointment.repository",
        "com.smartscheduler.common.repository"
})
@EntityScan(basePackages = {
        "com.smartscheduler.appointment.entity",
        "com.smartscheduler.common.entity"
})
public class AppointmentServiceApplication {
    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"));
        SpringApplication.run(AppointmentServiceApplication.class, args);
    }
}
