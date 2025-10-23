package com.smartscheduler.notification.service;

import com.smartscheduler.notification.client.feign.RasaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class RasaService {

    private static final Logger log = LoggerFactory.getLogger(RasaService.class);

    private final RasaClient rasaClient;

    public RasaService(RasaClient rasaClient) {
        this.rasaClient = rasaClient;
    }

    @Retryable(value = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public Map<String, Object> sendSlotAndGetResponse(String userId, LocalDateTime newSlot, ZoneId zoneId) {
        ZonedDateTime zonedNewSlot = newSlot.atZone(ZoneOffset.UTC).withZoneSameInstant(zoneId);

        Map<String, Object> payload = new HashMap<>();
        payload.put("event", "slot");
        payload.put("name", "temp_new_slot_datetime");
        payload.put("value", zonedNewSlot.format(DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZ")));

        return rasaClient.sendTrackerEvent(userId, payload);
    }

    @Recover
    public void fallbackSendSlotAndGetResponse(Exception e, String userId, String newSlot, ZoneId zoneId) {
        log.error("Failed to send slot and get response for user {} after retries: {}", userId, e.getMessage(), e);
        // Optional: queue for retry, alert admin, or store in DB
    }

    @Retryable(value = { Exception.class }, maxAttempts = 3, backoff = @Backoff(delay = 2000))
    public List<Map<String, Object>> processMessage(String sender, String message) {
        Map<String, String> payload = new HashMap<>();
        payload.put("sender", sender);
        payload.put("message", message);
        return rasaClient.sendMessage(payload);
    }

    @Recover
    public void fallbackProcessMessage(Exception e, String sender, String message) {
        log.error("Failed to send slot and get response for user {} after retries: {}", sender, e.getMessage(), e);
        // Optional: queue for retry, alert admin, or store in DB
    }

}
