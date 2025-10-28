package com.smartscheduler.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "message.provider", havingValue = "twilio")
public class TwilioMessageService implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(TwilioMessageService.class);

    @Value("${message.system-phone}")
    private String systemPhone;

    private static final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private static final String AUTH_TOKEN  = System.getenv("TWILIO_AUTH_TOKEN");

    private final RestTemplate restTemplate;

    public TwilioMessageService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    @Override
    public void sendWhatsAppMessage(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("No messages to send");
            return;
        }

        final String baseUrl = String.format(
                "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json",
                ACCOUNT_SID
        );

        for (Map<String, Object> msg : messages) {
            try {
                String recipientId = (String) msg.get("recipient_id");
                String text = (String) msg.get("text");
                List<Map<String, Object>> buttons = (List<Map<String, Object>>) msg.get("buttons");

                HttpHeaders headers = new HttpHeaders();
                headers.setBasicAuth(ACCOUNT_SID, AUTH_TOKEN);
                headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

                MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
                formData.add("From", "whatsapp:+" + systemPhone);
                formData.add("To", "whatsapp:+" + recipientId);

                if (buttons != null && !buttons.isEmpty()) {
                    // Twilio interactive message
                    String buttonsJson = buttonsToJson(buttons);
                    formData.add("PersistentAction", buttonsJson);
                    formData.add("Body", text);
                } else {
                    // Simple text message
                    formData.add("Body", text);
                }

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

                log.info("✅ Twilio WhatsApp message sent to [{}], status={}, response={}",
                        recipientId, response.getStatusCode(), response.getBody());

            } catch (Exception e) {
                log.error("❌ Failed to send Twilio WhatsApp message", e);
            }
        }
    }

    private String buttonsToJson(List<Map<String, Object>> buttons) {
        StringBuilder sb = new StringBuilder("[");
        for (int i = 0; i < buttons.size(); i++) {
            Map<String, Object> btn = buttons.get(i);
            sb.append(String.format("{\"type\":\"reply\",\"reply\":{\"id\":\"%s\",\"title\":\"%s\"}}",
                    ((String) btn.get("payload")).replace("/", ""),
                    btn.get("title")));
            if (i < buttons.size() - 1) sb.append(",");
        }
        sb.append("]");
        return sb.toString();
    }
}
