package com.smartscheduler.notification.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

@Service
public class RasaService {

    private static final Logger log = LoggerFactory.getLogger(RasaService.class);

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    public record RasaResult(String intent, String reply) {}

    public RasaService(RestTemplate restTemplate, ObjectMapper objectMapper) {
        this.restTemplate = restTemplate;
        this.objectMapper = objectMapper;
    }

    public RasaResult processMessage(String sender, String message) {
        String rasaUrl = "http://localhost:5005/webhooks/rest/webhook"; // use service name in Docker

        try {
            String payload = String.format("{\"sender\":\"%s\", \"message\":\"%s\"}", sender, message);

            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);

            HttpEntity<String> request = new HttpEntity<>(payload, headers);

            ResponseEntity<String> response = restTemplate.postForEntity(rasaUrl, request, String.class);
            JsonNode root = objectMapper.readTree(response.getBody());

            String reply = root.get(0).get("text").asText();
            String intent = root.get(0).path("intent").path("name").asText("unknown");

            return new RasaResult(intent, reply);
        } catch (Exception e) {
            log.error("Error communicating with Rasa", e);
            return new RasaResult("unknown", "Something went wrong.");
        }
    }
}
