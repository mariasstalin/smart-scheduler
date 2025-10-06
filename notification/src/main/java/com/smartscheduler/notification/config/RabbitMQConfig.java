package com.smartscheduler.notification.config;

import org.springframework.amqp.core.Queue;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {
    public static final String SLOT_CANCELLED_QUEUE = "slot_cancelled_queue";
    public static final String PATIENT_RESPONSE_QUEUE = "patient_response_queue";
    public static final String RESCHEDULE_REQUEST_QUEUE = "reschedule_request_queue";

    @Bean
    public Queue slotCancelledQueue() {
        return new Queue(SLOT_CANCELLED_QUEUE, true);
    }

    @Bean
    public Queue patientResponseQueue() {
        return new Queue(PATIENT_RESPONSE_QUEUE, true);
    }

    @Bean
    public Queue rescheduleRequestQueue() {
        return new Queue(RESCHEDULE_REQUEST_QUEUE, true);
    }
}

