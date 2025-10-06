package com.smartscheduler.rescheduler.config;

import org.springframework.amqp.core.*;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RabbitMQConfig {

    public static final String RESCHEDULE_EXCHANGE = "reschedule.exchange";
    public static final String RESCHEDULE_QUEUE = "reschedule_request_queue";
    public static final String RESCHEDULE_ROUTING_KEY = "reschedule.request";

    public static final String DLX_EXCHANGE = "reschedule.dlx.exchange";
    public static final String DLQ_QUEUE = "reschedule.dlq.queue";
    public static final String DLQ_ROUTING_KEY = "reschedule.dlq";

    @Bean
    public DirectExchange rescheduleExchange() {
        return new DirectExchange(RESCHEDULE_EXCHANGE);
    }

    @Bean
    public DirectExchange rescheduleDlxExchange() {
        return new DirectExchange(DLX_EXCHANGE);
    }

    @Bean
    public Queue rescheduleQueue() {
        return QueueBuilder.durable(RESCHEDULE_QUEUE)
                .withArgument("x-dead-letter-exchange", DLX_EXCHANGE)
                .withArgument("x-dead-letter-routing-key", DLQ_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue rescheduleDlqQueue() {
        return QueueBuilder.durable(DLQ_QUEUE).build();
    }

    @Bean
    public Binding rescheduleBinding(Queue rescheduleQueue, DirectExchange rescheduleExchange) {
        return BindingBuilder.bind(rescheduleQueue)
                .to(rescheduleExchange)
                .with(RESCHEDULE_ROUTING_KEY);
    }

    @Bean
    public Binding dlqBinding(Queue rescheduleDlqQueue, DirectExchange rescheduleDlxExchange) {
        return BindingBuilder.bind(rescheduleDlqQueue)
                .to(rescheduleDlxExchange)
                .with(DLQ_ROUTING_KEY);
    }
}
