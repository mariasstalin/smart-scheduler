package com.smartscheduler.demo.web;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;

import java.util.HashMap;
import java.util.Map;

@RequestMapping("/chat")
@Controller
public class ChatController {

    private static final String SYSTEM_NAME = "ClinicBot";

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    // Chat page for patient
    @GetMapping("/patients/{id}")
    public String chatByPatient(@PathVariable String id, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("userType", "Patient");
        return "chat";
    }

    // Chat page for doctor
    @GetMapping("/doctors/{id}")
    public String chatByDoctor(@PathVariable String id, Model model) {
        model.addAttribute("id", id);
        model.addAttribute("userType", "Doctor");
        return "chat";
    }

    // Receive user message and respond
    @MessageMapping("/send")
    public void handleMessage(Map<String, String> message) throws InterruptedException {
        String fromUserId = message.get("from");
        String text = message.get("body");

        // Create bot response
        Map<String, String> response = new HashMap<>();
        response.put("from", SYSTEM_NAME);
        response.put("to", fromUserId);
        response.put("body", text);

        // Send to user's private topic
        messagingTemplate.convertAndSend("/topic/messages-" + fromUserId, response);

        System.out.println("Sent to " + fromUserId + ": " + response.get("body"));
    }
}
