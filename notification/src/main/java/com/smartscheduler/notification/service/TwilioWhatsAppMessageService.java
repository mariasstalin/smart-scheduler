package com.smartscheduler.notification.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.List;
import java.util.Map;

/**
 * Sends messages using the actual Twilio WhatsApp API.
 * This service uses HTTP Basic Auth and application/x-www-form-urlencoded payload.
 * It implements a fallback for interactive buttons by converting them into numbered text options
 * to ensure dynamic appointment lists are functional within Twilio's API constraints.
 */
@Service
@ConditionalOnProperty(name = "message.provider", havingValue = "twilio")
@RequiredArgsConstructor
public class TwilioWhatsAppMessageService implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(TwilioWhatsAppMessageService.class);

    // --- Twilio Configuration ---
    @Value("${twilio.account-sid}")
    private String accountSid;

    @Value("${twilio.auth-token}")
    private String authToken;

    @Value("${message.system-phone}")
    private String systemPhone; // This is the Twilio sender number (e.g., "whatsapp:+14155552671")

    private final RestTemplate restTemplate;

    @Override
    public void sendWhatsAppMessage(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("No messages to send");
            return;
        }

        // Twilio API URL for sending messages
        final String twilioUrl = String.format(
                "https://api.twilio.com/2010-04-01/Accounts/%s/Messages.json",
                accountSid
        );

        // Basic Authentication Header
        String auth = accountSid + ":" + authToken;
        String encodedAuth = Base64.getEncoder().encodeToString(auth.getBytes(StandardCharsets.UTF_8));

        HttpHeaders headers = new HttpHeaders();
        // Twilio expects form data for simple messages
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        // Set Basic Auth
        headers.setBasicAuth(encodedAuth);


        for (Map<String, Object> msg : messages) {
            String recipientId = (String) msg.get("recipient_id");

            // Twilio requires phone numbers to be prefixed with 'whatsapp:'
            String to = "whatsapp:+" + recipientId;
            String from = "whatsapp:+" + systemPhone;

            try {
                // Build the form data payload
                MultiValueMap<String, String> payload = buildPayload(from, to, msg);

                HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(payload, headers);

                ResponseEntity<String> response = restTemplate.postForEntity(twilioUrl, request, String.class);

                log.info("✅ Twilio WhatsApp message sent to [{}], status={}",
                        recipientId, response.getStatusCode());

            } catch (Exception e) {
                log.error("❌ Failed to send Twilio WhatsApp message to {}", recipientId, e);
            }
        }
    }

    /**
     * Builds the MultiValueMap required for Twilio's form-urlencoded API.
     * * IMPORTANT NOTE ON BUTTONS:
     * Twilio's standard WhatsApp API does not support dynamically generated interactive buttons
     * (like those from Rasa) via the /Messages.json endpoint.
     * * To maintain functionality and allow the patient to select an appointment,
     * the buttons are converted into a numbered text list. The patient must reply
     * with the corresponding number (e.g., '1') for Rasa to process the selection.
     * To use true interactive buttons, the Twilio Content API and pre-approved
     * templates must be used, which is a significant change in architecture.
     */
    private MultiValueMap<String, String> buildPayload(String from, String to, Map<String, Object> msg) {
        String text = (String) msg.get("text");
        List<Map<String, Object>> buttons = (List<Map<String, Object>>) msg.get("buttons");

        MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
        formData.add("To", to);
        formData.add("From", from);

        // Use a StringBuilder to construct the full message body
        StringBuilder body = new StringBuilder(text);

        if (buttons != null && !buttons.isEmpty()) {
            body.append("\n\n----------------------------");
            body.append("\n**Select an Option by Number:**");
            for (int i = 0; i < buttons.size(); i++) {
                Map<String, Object> btn = buttons.get(i);
                // Note: We use i+1 as the number to reply with. The Rasa action will process this.
                body.append(String.format("\n%d. %s", (i + 1), btn.get("title")));
            }
        }

        formData.add("Body", body.toString());

        return formData;
    }
}
