package com.smartscheduler.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    @Bean
    public Queue appointmentsQueue() {
        return new Queue("appointments.queue", true);
    }
}

