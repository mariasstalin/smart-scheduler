
package com.smartscheduler.appointment.zoho;

import com.smartscheduler.appointment.model.ZohoPayload;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;
import java.util.Map;
import java.util.HashMap;
import java.util.Base64;

@Component
public class ZohoClient {

    @Value("${zoho.client.id:${ZOHO_CLIENT_ID:}}")
    private String clientId;

    @Value("${zoho.client.secret:${ZOHO_CLIENT_SECRET:}}")
    private String clientSecret;

    @Value("${zoho.refresh.token:${ZOHO_REFRESH_TOKEN:}}")
    private String refreshToken;

    @Value("${zoho.org.id:${ZOHO_ORG_ID:}}")
    private String orgId;

    private final RestTemplate rest = new RestTemplate();

    public String fetchAccessToken() {
        try {
            String url = "https://accounts.zoho.com/oauth/v2/token?refresh_token=" + refreshToken +
                         "&client_id=" + clientId + "&client_secret=" + clientSecret + "&grant_type=refresh_token";
            ResponseEntity<Map> resp = rest.postForEntity(url, null, Map.class);
            if (resp.getStatusCode() == HttpStatus.OK && resp.getBody()!=null) {
                Object at = resp.getBody().get("access_token");
                return at==null?null:at.toString();
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return null;
    }

    public Map createBooking(ZohoPayload payload) {
        String token = fetchAccessToken();
        if (token==null) throw new IllegalStateException("Zoho access token unavailable");
        String url = "https://www.zohoapis.com/bookings/v1.0/organizations/" + orgId + "/bookings";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        headers.setContentType(MediaType.APPLICATION_JSON);
        HttpEntity<ZohoPayload> req = new HttpEntity<>(payload, headers);
        ResponseEntity<Map> resp = rest.postForEntity(url, req, Map.class);
        if (resp.getStatusCode().is2xxSuccessful()) {
            return resp.getBody();
        } else {
            throw new RuntimeException("Zoho booking failed: " + resp.getStatusCode());
        }
    }

    public Map fetchServices() {
        String token = fetchAccessToken();
        if (token==null) return Map.of("error","no_token");
        String url = "https://www.zohoapis.com/bookings/v1.0/organizations/" + orgId + "/services";
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(token);
        HttpEntity<Void> e = new HttpEntity<>(headers);
        ResponseEntity<Map> resp = rest.exchange(url, HttpMethod.GET, e, Map.class);
        return resp.getBody();
    }
}
