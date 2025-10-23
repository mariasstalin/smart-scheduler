package com.smartscheduler.notification.client.messaging;

import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import com.twilio.type.PhoneNumber;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class TwilioClient implements MessagingClient {

    private final String accountSid;
    private final String authToken;
    private final String fromNumber;

    public TwilioClient() {
        this.accountSid = System.getenv("TWILIO_SID");
        this.authToken = System.getenv("TWILIO_AUTH");
        this.fromNumber = System.getenv("TWILIO_FROM"); // e.g. "whatsapp:+1415..."
    }

    @PostConstruct
    public void init() {
        Twilio.init(accountSid, authToken);
    }

    @Override
    public String sendMessage(String to, String body) {
        try {
            Message message = Message.creator(
                    new PhoneNumber(to),
                    new PhoneNumber(fromNumber),
                    body
            ).create();
            log.info("Sent message SID={}, to={}, body={}", message.getSid(), to, body);
            return message.getSid();
        } catch (Exception e) {
            log.error("Twilio send error", e);
            throw new RuntimeException(e);
        }
    }
}

