
package com.smartscheduler.appointment.rasa;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

@Component
public class RasaClient {
    @Value("${rasa.url:http://rasa:5005}")
    private String rasaUrl;
    private final RestTemplate rest = new RestTemplate();

    // Suggest best slot (stub) - in real system, call Rasa model endpoints
    public String suggestBestSlot(String context) {
        try {
            var resp = rest.getForObject(rasaUrl + "/status", String.class);
            return resp;
        } catch (Exception ex) {
            return "rasa-unavailable";
        }
    }
}
