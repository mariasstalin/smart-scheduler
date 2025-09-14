package com.smartscheduler.notification.rabbit;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartscheduler.common.dto.AppointmentMessage;
import com.smartscheduler.notification.model.Notification;
import com.smartscheduler.notification.repo.NotificationRepository;
import com.twilio.Twilio;
import com.twilio.rest.api.v2010.account.Message;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class AppointmentConsumer {

    private final NotificationRepository repo;
    private final RestTemplate rest = new RestTemplate();
    private final ObjectMapper mapper = new ObjectMapper();

    @Value("${twilio.accountSid:}")
    private String twAccountSid;

    @Value("${twilio.authToken:}")
    private String twAuthToken;

    @Value("${twilio.whatsappFrom:whatsapp:+14155238886}")
    private String whatsappFrom;

    @Value("${user.service.url:http://user-service:8081}")
    private String userServiceUrl;

    public AppointmentConsumer(NotificationRepository repo) {
        this.repo = repo;
    }

    @RabbitListener(queues = "appointments.queue")
    public void receive(AppointmentMessage msg) {
        try {
            // Save notification in DB
            Notification n = new Notification();
            n.setUserId(msg.userId);
            n.setChannel("whatsapp");
            n.setMessage("Your appointment " + msg.action + " at " + msg.startTime);
            repo.save(n);

            // Lookup user details from User Service
            String url = userServiceUrl + "/users/" + msg.userId;
            String phone = null;

            try {
                String resp = rest.getForObject(url, String.class);

                if (resp != null) {
                    JsonNode node = mapper.readTree(resp);
                    if (node.has("phone")) {
                        phone = node.get("phone").asText();
                    }
                }

                // If phone + Twilio credentials are present, send WhatsApp message
                if (phone != null && !phone.isBlank()
                        && twAccountSid != null && !twAccountSid.isBlank()) {

                    Twilio.init(twAccountSid, twAuthToken);

                    Message.creator(
                            new com.twilio.type.PhoneNumber("whatsapp:" + phone),
                            new com.twilio.type.PhoneNumber(whatsappFrom),
                            n.getMessage()
                    ).create();

                    n.setDelivered(true);
                    repo.save(n);
                }

            } catch (Exception ex) {
                ex.printStackTrace(); // TODO: replace with logger.warn()
            }

        } catch (Exception ex) {
            ex.printStackTrace(); // TODO: replace with logger.error()
        }
    }
}
