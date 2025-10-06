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

    @Bean
    public Queue appointmentBookedQueue() {
        return QueueBuilder.durable("appointment.booked.queue").build();
    }

    @Bean
    public Queue appointmentCancelledQueue() {
        return QueueBuilder.durable("appointment.cancelled.queue").build();
    }

    @Bean
    public Binding bookingBinding(Queue appointmentBookedQueue, DirectExchange appointmentsExchange) {
        return BindingBuilder.bind(appointmentBookedQueue)
                .to(appointmentsExchange)
                .with("appointments.booked");
    }

    @Bean
    public Binding cancellationBinding(Queue appointmentCancelledQueue, DirectExchange appointmentsExchange) {
        return BindingBuilder.bind(appointmentCancelledQueue)
                .to(appointmentsExchange)
                .with("appointments.cancelled");
    }

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
