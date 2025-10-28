package com.smartscheduler.notification.service;

import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Service
@ConditionalOnProperty(name = "message.provider", havingValue = "demo")
@RequiredArgsConstructor
public class SimulatedWhatsAppMessageService implements MessageService {

    private static final Logger log = LoggerFactory.getLogger(SimulatedWhatsAppMessageService.class);

    @Value("${message.system-phone}")
    private String systemPhone;

    private final RestTemplate restTemplate;

    @Override
    public void sendWhatsAppMessage(List<Map<String, Object>> messages) {
        if (messages == null || messages.isEmpty()) {
            log.warn("No messages to send");
            return;
        }

        final String baseUrl = "http://localhost:9999/demo/chat/external";

        for (Map<String, Object> msg : messages) {
            try {
                String recipientId = (String) msg.get("recipient_id");
                String text = (String) msg.get("text");
                List<Map<String, Object>> buttons = (List<Map<String, Object>>) msg.get("buttons");

                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);

                String payloadJson = buildPayload(recipientId, text, buttons);

                HttpEntity<String> request = new HttpEntity<>(payloadJson, headers);
                ResponseEntity<String> response = restTemplate.postForEntity(baseUrl, request, String.class);

                log.info("✅ Demo WhatsApp message sent to [{}], status={}, response={}",
                        recipientId, response.getStatusCode(), response.getBody());

            } catch (Exception e) {
                log.error("❌ Failed to send Demo WhatsApp message", e);
            }
        }
    }

    private String buildPayload(String recipientId, String text, List<Map<String, Object>> buttons) {
        if (buttons != null && !buttons.isEmpty()) {
            StringBuilder buttonsJson = new StringBuilder("[");
            for (int i = 0; i < buttons.size(); i++) {
                Map<String, Object> btn = buttons.get(i);
                buttonsJson.append(String.format(
                        """
                        {"type":"reply","reply":{"id":"%s","title":"%s"}}
                        """,
                        ((String) btn.get("payload")).replace("/", ""),
                        btn.get("title")
                ));
                if (i < buttons.size() - 1) buttonsJson.append(",");
            }
            buttonsJson.append("]");
            return String.format(
                    """
                    {
                      "from": "whatsapp:+%s",
                      "to": "whatsapp:+%s",
                      "type": "interactive",
                      "interactive": {
                        "type": "button",
                        "body": {"text": "%s"},
                        "action": {"buttons": %s}
                      }
                    }
                    """,
                    systemPhone, recipientId, escapeJson(text), buttonsJson
            );
        } else {
            return String.format(
                    """
                    {
                      "from": "whatsapp:+%s",
                      "to": "whatsapp:+%s",
                      "body": "%s"
                    }
                    """,
                    systemPhone, recipientId, escapeJson(text)
            );
        }
    }

    private String escapeJson(String input) {
        return input == null ? "" : input.replace("\"", "\\\"");
    }
}
