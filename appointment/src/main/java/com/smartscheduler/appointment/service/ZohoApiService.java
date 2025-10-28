package com.smartscheduler.appointment.service;

import com.smartscheduler.common.service.ZohoTokenService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.HttpClientErrorException;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Service
@RequiredArgsConstructor
@Slf4j
public class ZohoApiService {

    private final ZohoTokenService zohoTokenService;
    private final RestClient restClient;

    @Value("${zoho.api.base-url:https://www.zohoapis.com}/bookings/v1/json")
    private String zohoApiUrl;

    private static final DateTimeFormatter ZOHO_DATETIME_FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss").withZone(ZoneId.of("UTC"));

    private HttpHeaders createAuthHeaders() {
        String accessToken = zohoTokenService.getAccessToken();
        HttpHeaders headers = new HttpHeaders();
        headers.set(HttpHeaders.AUTHORIZATION, "Zoho-oauthtoken " + accessToken);
        headers.setContentType(MediaType.APPLICATION_FORM_URLENCODED);
        return headers;
    }

    public void cancelAppointmentInZoho(String zohoBookingId) {
        log.info("Attempting to cancel Zoho Booking ID: {}", zohoBookingId);

        try {
            String url = zohoApiUrl + "/updateappointment";

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("booking_id", zohoBookingId);
            formData.add("action", "cancel");

            restClient.post()
                    .uri(url)
                    .headers(h -> h.addAll(createAuthHeaders()))
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully cancelled Zoho Booking ID: {}", zohoBookingId);
        } catch (HttpClientErrorException e) {
            log.error("Zoho API Error during cancellation of {}: Status={}, Body={}", zohoBookingId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to cancel appointment in Zoho: " + e.getMessage());
        }
    }

    public void rescheduleAppointmentInZoho(String zohoBookingId, String newStartTime, Long staffId) {
        log.info("Attempting to reschedule Zoho Booking ID: {} to {}", zohoBookingId, newStartTime);

        String staffIdString = String.valueOf(staffId);

        try {
            String url = zohoApiUrl + "/rescheduleappointment";

            MultiValueMap<String, String> formData = new LinkedMultiValueMap<>();
            formData.add("booking_id", zohoBookingId);
            formData.add("staff_id", staffIdString);
            formData.add("start_time", newStartTime + ":00");

            restClient.post()
                    .uri(url)
                    .headers(h -> h.addAll(createAuthHeaders()))
                    .body(formData)
                    .retrieve()
                    .toBodilessEntity();

            log.info("Successfully rescheduled Zoho Booking ID: {}", zohoBookingId);
        } catch (HttpClientErrorException e) {
            log.error("Zoho API Error during reschedule of {}: Status={}, Body={}", zohoBookingId, e.getStatusCode(), e.getResponseBodyAsString(), e);
            throw new RuntimeException("Failed to reschedule appointment in Zoho: " + e.getMessage());
        }
    }
}