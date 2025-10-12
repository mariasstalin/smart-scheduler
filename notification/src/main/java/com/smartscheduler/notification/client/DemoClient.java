package com.smartscheduler.notification.client;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
@Slf4j
public class DemoClient implements MessagingClient {

    @Value("messaging.system-phone")
    private String systemPhone;

    @Autowired
    private SimpMessagingTemplate messagingTemplate;

    @Override
    public String sendMessage(String to, String body) {
        try {
            Map<String, String> response = new HashMap<>();
            response.put("from", systemPhone);
            response.put("to", to);
            response.put("body", body);

            messagingTemplate.convertAndSend("/topic/messages-" + to, response);

            System.out.println("Sent to " + to + ": " + response.get("body"));
            return null;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}

