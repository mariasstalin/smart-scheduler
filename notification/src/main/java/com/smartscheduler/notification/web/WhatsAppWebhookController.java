package com.smartscheduler.notification.web;

import com.smartscheduler.notification.service.RasaService;
import com.smartscheduler.notification.service.TwilioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/webhook/whatsapp")
public class WhatsAppWebhookController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppWebhookController.class);

    private final RasaService rasaService;

    private final TwilioService twilioService;

    public WhatsAppWebhookController(RasaService rasaService, TwilioService twilioService) {
        this.rasaService = rasaService;
        this.twilioService = twilioService;
    }

    @PostMapping
    public ResponseEntity<String> handleMessage(@RequestParam("Body") String message, @RequestParam("From") String fromNumber, @RequestParam("To") String toNumber) {
        log.info("Incoming message from [{}]: {}", fromNumber, message);

        List<Map<String, Object>> rasaResult = rasaService.processMessage(fromNumber, message);
        log.info("Rasa response: {}", rasaResult);
        if(rasaResult.size() != 1) {
            return ResponseEntity.internalServerError().build();
        }
        Map<String, Object> result = rasaResult.getFirst();

        twilioService.sendWhatsAppMessage(fromNumber, String.valueOf(result.get("")));

        return ResponseEntity.ok("Processed");
    }

}
