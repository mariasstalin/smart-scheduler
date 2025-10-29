package com.smartscheduler.notification.web;

import com.smartscheduler.notification.service.MessageService;
import com.smartscheduler.notification.service.RasaService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.Objects;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final RasaService rasaService;
    private final MessageService messageService;

    public WhatsAppWebhookController(RasaService rasaService, MessageService messageService) {
        this.rasaService = rasaService;
        this.messageService = messageService;
    }

    @PostMapping
    public ResponseEntity<String> handleMessage(
            @RequestParam("Body") String message,
            @RequestParam("From") String fromNumber,
            @RequestParam("To") String toNumber,
            @RequestParam(name = "ButtonId", required=false) String buttonId) {

        log.info("📩 Incoming message from [{}] → [{}]: {}", fromNumber, toNumber, message);

        try {
            if(Objects.nonNull(buttonId)) {
                if("confirm_slot_offer".equalsIgnoreCase(buttonId)) {
                    message = "/confirm_slot_offer";
                } else if("deny_slot_offer".equalsIgnoreCase(buttonId)) {
                    message = "/deny_slot_offer";
                } else if(buttonId.startsWith("provide_selection")) {
                    message = "/" + buttonId;
                }
            }

            // Process user message via Rasa
            List<Map<String, Object>> rasaResponses = rasaService.processMessage(fromNumber, message);
            log.info("🤖 Rasa response: {}", rasaResponses);

            if (rasaResponses == null || rasaResponses.isEmpty()) {
                log.warn("⚠️ Rasa returned no response for [{}]", fromNumber);
                return ResponseEntity.ok("No response generated");
            }

            // Send response(s) to WhatsApp
            messageService.sendWhatsAppMessage(rasaResponses);
            log.info("✅ WhatsApp response sent successfully to [{}]", fromNumber);

            return ResponseEntity.ok("Processed");

        } catch (Exception e) {
            log.error("❌ Error while processing message from [{}]", fromNumber, e);
            return ResponseEntity.internalServerError().body("Error processing message");
        }
    }
}
