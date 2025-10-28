package com.smartscheduler.appointment.config;

import org.springframework.amqp.core.*;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    @Bean
    public DirectExchange appointmentsExchange() {
        return new DirectExchange("appointments.exchange");
    }

    // --- Queue Definitions (Pluralized for consistency) ---

    @Bean
    public Queue appointmentsSlotsBookedQueue() {
        // Corrected queue name: appointments.slots.booked.queue
        return QueueBuilder.durable("appointments.slots.booked.queue").build();
    }

    @Bean
    public Queue appointmentsSlotsCancelledQueue() {
        // Corrected queue name: appointments.slots.cancelled.queue
        return QueueBuilder.durable("appointments.slots.cancelled.queue").build();
    }

    // --- Binding Definitions ---

    @Bean
    public Binding bookingBinding(Queue appointmentsSlotsBookedQueue, DirectExchange appointmentsExchange) {
        // Routing key is correct: appointments.slots.booked
        return BindingBuilder.bind(appointmentsSlotsBookedQueue)
                .to(appointmentsExchange)
                .with("appointments.slots.booked");
    }

    @Bean
    public Binding cancellationBinding(Queue appointmentsSlotsCancelledQueue, DirectExchange appointmentsExchange) {
        // Routing key is correct: appointments.slots.cancelled
        return BindingBuilder.bind(appointmentsSlotsCancelledQueue)
                .to(appointmentsExchange)
                .with("appointments.slots.cancelled");
    }

    // --- Messaging Configuration ---

    @Bean
    public Jackson2JsonMessageConverter jackson2JsonMessageConverter() {
        return new Jackson2JsonMessageConverter();
    }

    @Bean
    public RabbitTemplate rabbitTemplate(ConnectionFactory connectionFactory) {
        RabbitTemplate template = new RabbitTemplate(connectionFactory);
        template.setMessageConverter(jackson2JsonMessageConverter());
        return template;
    }
}
