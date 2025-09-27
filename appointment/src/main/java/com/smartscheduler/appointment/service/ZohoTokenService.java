package com.smartscheduler.appointment.service;

import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;

@Service
public class ZohoTokenService {

    private static final String TOKEN_URL = "https://accounts.zoho.in/oauth/v2/token";

    private final RestTemplate restTemplate = new RestTemplate();

    private final String clientId = "1000.RMOOGMGPQII9GYSJVD7ZY3S8PJWF4E";

    private final String clientSecret = "05fc217a0cbb4a93dd7e0c8cccf15cac43b025aaf2";

    private final String refreshToken = "1000.your_refresh_token_here";

    private String accessToken;

    private Instant expiryTime;

    public synchronized String getAccessToken() {
        if (accessToken == null || Instant.now().isAfter(expiryTime)) {
            refreshAccessToken();
        }
        return accessToken;
    }

    private void refreshAccessToken() {
        MultiValueMap<String, String> body = new LinkedMultiValueMap<>();
        body.add("refresh_token", refreshToken);
        body.add("client_id", clientId);
        body.add("client_secret", clientSecret);
        body.add("grant_type", "refresh_token");

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);

        HttpEntity<MultiValueMap<String, String>> request = new HttpEntity<>(body, headers);

        ResponseEntity<ZohoTokenResponse> response = restTemplate.postForEntity(TOKEN_URL, request, ZohoTokenResponse.class);

        if (response.getStatusCode().is2xxSuccessful() && response.getBody() != null) {
            this.accessToken = response.getBody().getAccessToken();
            this.expiryTime = Instant.now().plusSeconds(response.getBody().getExpiresIn() - 30);
        } else {
            throw new RuntimeException("Failed to refresh Zoho access token: " + response);
        }
    }

    public static class ZohoTokenResponse {

        private String access_token;

        private int expires_in;

        private String api_domain;

        private String token_type;

        public String getAccessToken() { return access_token; }

        public int getExpiresIn() { return expires_in; }
    }

}

