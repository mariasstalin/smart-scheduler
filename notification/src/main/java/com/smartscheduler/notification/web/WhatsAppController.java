package com.smartscheduler.notification.web;

import com.smartscheduler.notification.service.RasaService;
import com.smartscheduler.notification.service.TwilioService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/whatsapp")
public class WhatsAppController {

    private static final Logger log = LoggerFactory.getLogger(WhatsAppController.class);

    private final RasaService rasaService;

    private final TwilioService twilioService;

    public WhatsAppController(RasaService rasaService, TwilioService twilioService) {
        this.rasaService = rasaService;
        this.twilioService = twilioService;
    }

    @PostMapping("/incoming")
    public ResponseEntity<String> incoming(
            @RequestParam("Body") String message,
            @RequestParam("From") String fromNumber, @RequestParam("To") String toNumber) {

        log.info("Incoming message from [{}]: {}", fromNumber, message);

        var rasaResult = rasaService.processMessage(fromNumber, message);

        log.info("Rasa intent: {}, botReply: {}", rasaResult.intent(), rasaResult.reply());

        String actionResult = switch (rasaResult.intent()) {
            case "book_appointment" -> "Appointment Scheduled"; //appointmentService.book(fromNumber, message);
            case "cancel_appointment" -> "Appointment Cancelled"; //appointmentService.cancel(fromNumber, message);
            case "reschedule_appointment" -> "Appointment Re-Scheduled"; //appointmentService.reschedule(fromNumber, message);
            default -> "Sorry, I didn’t understand. Could you rephrase?";
        };

        String finalReply = (actionResult != null && !actionResult.isBlank()) ? actionResult : rasaResult.reply();
        twilioService.sendWhatsAppMessage(fromNumber, finalReply);

        return ResponseEntity.ok("Processed");
    }
}
