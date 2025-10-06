package com.smartscheduler.rescheduler.service;

import com.smartscheduler.common.event.SlotRescheduledEvent;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReschedulerService {

    private final RestTemplate restTemplate;
    private final RabbitTemplate rabbitTemplate;

    // AppointmentService HTTP reschedule endpoint (adjust to your environment)
    private final String appointmentServiceUrl = "http://appointment-service/api/reschedule";

    @RabbitListener(queues = "reschedule_request_queue")
    public void onRescheduleRequest(SlotRescheduledEvent event) {
        log.info("Received SlotRescheduledEvent: {}", event);
        try {
            rescheduleWithRetry(event);
        } catch (Exception ex) {
            log.error("Reschedule failed after retries, sending to DLQ. Event={}", event, ex);
            // publish to DLQ exchange/routing for later inspection
            rabbitTemplate.convertAndSend("reschedule.dlx.exchange", "reschedule.dlq", event);
        }
    }

    @Retryable(maxAttempts = 5, backoff = @Backoff(delay = 2000, multiplier = 2))
    public void rescheduleWithRetry(SlotRescheduledEvent event) {
        Map<String, Object> payload = Map.of(
                "notificationId", event.getNotificationId(),
                "oldAppointmentId", event.getAppointmentId(),
                "patientId", event.getPatientId(),
                "doctorId", event.getDoctorId(),
                "requestedStartIso", event.getStartTime(),
                "requestedEndIso", event.getEndTime()
        );

        try {
            restTemplate.postForEntity(appointmentServiceUrl, payload, Void.class);
            log.info("Reschedule request posted to AppointmentService notificationId={}", event.getNotificationId());
        } catch (Exception ex) {
            log.warn("Error posting reschedule to AppointmentService notificationId={}. Will retry...", event.getNotificationId(), ex);
            throw ex; // trigger retry
        }
    }

    @RabbitListener(queues = "reschedule.dlq.queue")
    public void onRescheduleDLQ(SlotRescheduledEvent event) {
        log.error("Received event on reschedule.dlq.queue. Manual intervention required. Event={}", event);
        // TODO: persist to audit table or push to ops channel (Slack/email) for human handling
    }
}
