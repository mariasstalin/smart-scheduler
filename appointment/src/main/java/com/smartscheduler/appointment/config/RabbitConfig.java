
package com.smartscheduler.appointment.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitConfig {

    public static final String APPOINTMENTS_EXCHANGE = "appointments-exchange";
    public static final String APPOINTMENT_CREATED_ROUTING = "appointment.created";
    public static final String APPOINTMENTS_QUEUE = "appointments.queue";

    @Bean
    public TopicExchange appointmentsExchange() {
        return new TopicExchange(APPOINTMENTS_EXCHANGE);
    }

    @Bean
    public Queue appointmentsQueue() {
        return new Queue(APPOINTMENTS_QUEUE, true);
    }

    @Bean
    public Binding binding(Queue appointmentsQueue, TopicExchange appointmentsExchange) {
        return BindingBuilder.bind(appointmentsQueue).to(appointmentsExchange).with(APPOINTMENT_CREATED_ROUTING);
    }
}
