package com.smartscheduler.notification.service;

import com.smartscheduler.notification.client.feign.RasaClient;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Collections;
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

    public List<Map<String, Object>> sendExternalSlotOffer(String senderId, String newSlotDatetime, String oldAppointmentId, String slotOfferId) {

        log.info("📤 Sending /EXTERNAL_SLOT_OFFER to Rasa for sender [{}], slotOfferId [{}]", senderId, slotOfferId);

        try {
            // Metadata payload
            Map<String, Object> metadata = new HashMap<>();
            metadata.put("new_slot_datetime", newSlotDatetime);
            metadata.put("old_appointment_id", oldAppointmentId);
            metadata.put("slot_offer_id", slotOfferId);

            // Full payload
            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", senderId);
            payload.put("message", "/EXTERNAL_SLOT_OFFER");
            payload.put("metadata", metadata);

            List<Map<String, Object>> response = rasaClient.sendMessage(payload);

            if (response != null && !response.isEmpty()) {
                log.info("✅ Rasa response received for sender [{}]: {}", senderId, response);
                return response;
            }

            log.warn("⚠️ Empty or null Rasa response for sender [{}]", senderId);
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Failed to send /EXTERNAL_SLOT_OFFER to Rasa for sender [{}]", senderId, e);
            return Collections.emptyList();
        }
    }

    public List<Map<String, Object>> processMessage(String senderId, String message) {
        log.info("📨 Sending message to Rasa from [{}]: {}", senderId, message);

        try {
            Map<String, Object> payload = new HashMap<>();
            payload.put("sender", senderId);
            payload.put("message", message);

            List<Map<String, Object>> response = rasaClient.sendMessage(payload);

            if (response == null || response.isEmpty()) {
                response = rasaClient.sendMessage(payload);
                if (response != null && !response.isEmpty()) {
                    log.info("✅ Rasa response for [{}]: {}", senderId, response);
                    return response;
                }
            } else {
                return response;
            }

            log.warn("⚠️ Rasa returned no response for [{}]", senderId);
            return Collections.emptyList();

        } catch (Exception e) {
            log.error("❌ Failed to send message to Rasa for sender [{}]", senderId, e);
            return Collections.emptyList();
        }
    }
}
