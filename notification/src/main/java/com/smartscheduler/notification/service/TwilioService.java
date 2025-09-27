package com.smartscheduler.notification.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

@Service
public class TwilioService {

    private static final Logger log = LoggerFactory.getLogger(TwilioService.class);

    private final RestTemplate restTemplate;

    private final String ACCOUNT_SID = System.getenv("TWILIO_ACCOUNT_SID");
    private final String AUTH_TOKEN   = System.getenv("TWILIO_AUTH_TOKEN");
    private final String FROM_NUMBER  = "whatsapp:+14155238886"; // Twilio sandbox number

    public TwilioService(RestTemplate restTemplate) {
        this.restTemplate = restTemplate;
    }

    public void sendWhatsAppMessage(String toNumber, String message) {
        String url = "https://api.twilio.com/2010-04-01/Accounts/" + ACCOUNT_SID + "/Messages.json";

        HttpHeaders headers = new HttpHeaders();
        headers.setBasicAuth(ACCOUNT_SID, AUTH_TOKEN);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("From", FROM_NUMBER);
        body.add("To", toNumber);
        body.add("Body", message);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        try {
            ResponseEntity<String> response = restTemplate.postForEntity(url, request, String.class);
            log.info("📤 Sent WhatsApp message to [{}], status={}", toNumber, response.getStatusCode());
        } catch (Exception e) {
            log.error("Failed to send WhatsApp message", e);
        }
    }
}
