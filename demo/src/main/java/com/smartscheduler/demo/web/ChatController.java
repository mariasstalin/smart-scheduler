package com.smartscheduler.demo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseBody; // <-- New Import
import org.springframework.web.client.RestTemplate;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

@RequestMapping("/chat")
@Controller
public class ChatController {

    @Value("${message.system-phone}")
    private String systemPhone;

    @Value("${application.base-url}/notification/webhook/whatsapp")
    private String whatsappWebhookUrl;

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @GetMapping("/patients/{id}")
    public String chatByPatient(@PathVariable String id, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("userType", "Patient");
        return "chat";
    }

    @GetMapping("/doctors/{id}")
    public String chatByDoctor(@PathVariable String id, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("userType", "Doctor");
        return "chat";
    }

    @MessageMapping("/chat")
    public void receiveMessage(Map<String, String> message) {
        String fromNumber = message.get("from");
        String textBody = message.get("body");
        String buttonId = message.get("buttonId"); // Capture buttonId if present

        if (fromNumber == null || textBody == null) {
            return;
        }

        try {
            // FIX: Use MultiValueMap to correctly send data as application/x-www-form-urlencoded
            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("Body", textBody);
            formData.add("From", fromNumber);
            formData.add("To", systemPhone);
            if (buttonId != null) {
                formData.add("ButtonId", buttonId);
            }

            // Set content type header
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

            HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(formData, headers);

            // Use exchange or postForLocation, which supports the HttpEntity
            restTemplate.postForLocation(whatsappWebhookUrl, request);

        } catch (Exception e) {
            System.err.println("Error processing incoming chat message: " + e.getMessage());
            // Error response logic remains the same
            Map<String, Object> errorResponse = Map.of(
                    "from", systemPhone,
                    "to", fromNumber,
                    "body", "Error connecting to the messaging service: " + e.getMessage()
            );
            messagingTemplate.convertAndSend("/topic/messages-" + fromNumber, errorResponse);
        }
    }

    @PostMapping("/external")
    @ResponseBody // <-- FIX: This tells Spring NOT to look for a template file
    public void receiveFromService(@RequestBody Map<String, Object> payload) {
        // Strip the "whatsapp:+" prefix from the "from" field for client compatibility
        Object from = payload.get("from");
        if (from != null && from instanceof String) {
            payload.put("from", ((String) from).replace("whatsapp:+", ""));
        }

        // Strip the "whatsapp:+" prefix from the "to" field just in case it's present
        Object to = payload.get("to");
        if (to != null && to instanceof String) {
            payload.put("to", ((String) to).replace("whatsapp:+", ""));
        }

        String recipient = (String) payload.get("to");
        if (recipient != null) {
            // Forward the payload to the specific user's WebSocket topic
            messagingTemplate.convertAndSend("/topic/messages-" + recipient, payload);
        }
    }
}
