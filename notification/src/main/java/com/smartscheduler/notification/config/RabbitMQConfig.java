package com.smartscheduler.notification.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
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

    // --- 1. SLOTS CANCELLED (Input, triggers the cascade) ---

    // The queue that the SlotAllocationService listens to
    @Bean
    public Queue appointmentSlotsCancelledQueue() {
        // Uses plural naming convention
        return new Queue("appointments.slots.cancelled.queue", true);
    }

    @Bean
    public Binding cancellationBinding(Queue appointmentSlotsCancelledQueue, DirectExchange appointmentsExchange) {
        // Binds to the plural routing key
        return BindingBuilder.bind(appointmentSlotsCancelledQueue)
                .to(appointmentsExchange)
                .with("appointments.slots.cancelled");
    }

    // --- 2. SLOTS REALLOCATED (Input, triggered by Rasa response 'YES') ---

    @Bean
    public Queue appointmentsSlotsReallocatedQueue() {
        return new Queue("appointments.slots.reallocated.queue", true);
    }

    @Bean
    public Binding reallocatedBinding(Queue appointmentsSlotsReallocatedQueue, DirectExchange appointmentsExchange) {
        return BindingBuilder.bind(appointmentsSlotsReallocatedQueue)
                .to(appointmentsExchange)
                .with("appointments.slots.reallocated");
    }

    // --- 3. SLOTS DENIED (Input, triggered by Rasa response 'NO') ---

    @Bean
    public Queue appointmentsSlotsDeniedQueue() {
        return new Queue("appointments.slots.denied.queue", true);
    }

    @Bean
    public Binding deniedBinding(Queue appointmentsSlotsDeniedQueue, DirectExchange appointmentsExchange) {
        return BindingBuilder.bind(appointmentsSlotsDeniedQueue)
                .to(appointmentsExchange)
                .with("appointments.slots.denied");
    }


    // --- 4. SLOTS RESCHEDULED (Output, command to core scheduler) ---

    @Bean
    public Queue appointmentsSlotsRescheduledQueue() {
        // Uses plural naming convention
        return new Queue("appointments.slots.rescheduled.queue", true);
    }

    @Bean
    public Binding rescheduledBinding(Queue appointmentsSlotsRescheduledQueue, DirectExchange appointmentsExchange) {
        return BindingBuilder.bind(appointmentsSlotsRescheduledQueue)
                .to(appointmentsExchange)
                .with("appointments.slots.rescheduled");
    }

    // --- 5. SLOTS OPENED (Output, command to public booking system) ---

    @Bean
    public Queue appointmentsSlotsOpenedQueue() {
        // Uses plural naming convention
        return new Queue("appointments.slots.opened.queue", true);
    }

    @Bean
    public Binding openedBinding(Queue appointmentsSlotsOpenedQueue, DirectExchange appointmentsExchange) {
        return BindingBuilder.bind(appointmentsSlotsOpenedQueue)
                .to(appointmentsExchange)
                .with("appointments.slots.opened");
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
