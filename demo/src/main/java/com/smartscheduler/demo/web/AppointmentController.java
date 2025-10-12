package com.smartscheduler.demo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@RestController
@RequestMapping("/appointment")
public class AppointmentController {

    // Zoho credentials
    private static final String ACCESS_TOKEN = "YOUR_ACCESS_TOKEN";
    private static final String SERVICE_ID = "YOUR_SERVICE_ID";
    private static final String STAFF_ID = "YOUR_STAFF_ID";
    private static final String TIMEZONE = "Asia/Kolkata";

    private static final int DURATION_MIN = 30;
    private static final int SLOTS_PER_DAY = 12; // 6 hours / 30-min slots
    private static final int TOTAL_DAYS = 3;

    private final RestTemplate restTemplate = new RestTemplate();
    private final DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
    private final ObjectMapper objectMapper = new ObjectMapper();

    /**
     * Create demo appointments starting from next 30-min slot from now
     */
    @PostMapping
    public List<Map<String, Object>> createAppointments() {
        List<Map<String, Object>> results = new ArrayList<>();

        LocalDateTime now = LocalDateTime.now();

        // Round to next 30-min slot
        int minute = now.getMinute();
        int roundedMin = (minute / DURATION_MIN + 1) * DURATION_MIN;
        if (roundedMin == 60) {
            now = now.plusHours(1).withMinute(0).truncatedTo(ChronoUnit.MINUTES);
        } else {
            now = now.withMinute(roundedMin).truncatedTo(ChronoUnit.MINUTES);
        }

        // Fetch already booked slots
        Set<String> bookedSlots = fetchExistingAppointments();

        for (int day = 0; day < TOTAL_DAYS; day++) {
            LocalDateTime dayStart = now.plusDays(day);
            for (int slot = 0; slot < SLOTS_PER_DAY; slot++) {
                LocalDateTime appointmentTime = dayStart.plusMinutes(slot * DURATION_MIN);
                String fromTime = appointmentTime.format(formatter);

                // Skip if slot already booked
                if (bookedSlots.contains(fromTime)) continue;

                Map<String, String> customer = new HashMap<>();
                customer.put("name", "Demo User " + (day * SLOTS_PER_DAY + slot + 1));
                customer.put("email", "demouser" + (day * SLOTS_PER_DAY + slot + 1) + "@example.com");
                customer.put("phoneNumber", "+9199000000" + (day * SLOTS_PER_DAY + slot + 1));

                try {
                    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
                    form.add("service_id", SERVICE_ID);
                    form.add("staff_id", STAFF_ID);
                    form.add("from_time", fromTime);
                    form.add("timezone", TIMEZONE);
                    form.add("customer_details", objectMapper.writeValueAsString(customer));
                    form.add("notes", "Hackathon demo appointment");

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                    headers.set("Authorization", "Zoho-oauthtoken " + ACCESS_TOKEN);

                    HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(form, headers);

                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            "https://www.zohoapis.in/bookings/v1/json/appointment",
                            request,
                            Map.class
                    );

                    Map<String, Object> result = new HashMap<>();
                    result.put("customer", customer);
                    result.put("fromTime", fromTime);
                    result.put("response", response.getBody());
                    results.add(result);

                } catch (Exception e) {
                    Map<String, Object> result = new HashMap<>();
                    result.put("customer", customer);
                    result.put("fromTime", fromTime);
                    result.put("error", e.getMessage());
                    results.add(result);
                }
            }
        }

        return results;
    }

    @DeleteMapping
    public Map<String, Object> deleteAppointments() {
        Map<String, Object> result = new HashMap<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Zoho-oauthtoken " + ACCESS_TOKEN);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.zohoapis.in/bookings/v1/json/appointments?service_id=" + SERVICE_ID,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            List<Map<String, Object>> appointments = (List<Map<String, Object>>) response.getBody().get("appointments");
            List<String> deletedIds = new ArrayList<>();

            if (appointments != null) {
                for (Map<String, Object> app : appointments) {
                    String appointmentId = app.get("appointment_id").toString();
                    restTemplate.exchange(
                            "https://www.zohoapis.in/bookings/v1/json/appointment/" + appointmentId,
                            HttpMethod.DELETE,
                            entity,
                            String.class
                    );
                    deletedIds.add(appointmentId);
                }
            }

            result.put("deleted_appointments", deletedIds);
            result.put("status", "success");
        } catch (Exception e) {
            result.put("status", "error");
            result.put("message", e.getMessage());
        }

        return result;
    }

    /**
     * Fetch existing appointments and return booked from_time strings
     */
    private Set<String> fetchExistingAppointments() {
        Set<String> bookedSlots = new HashSet<>();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Zoho-oauthtoken " + ACCESS_TOKEN);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    "https://www.zohoapis.in/bookings/v1/json/appointments?service_id=" + SERVICE_ID,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            List<Map<String, Object>> appointments = (List<Map<String, Object>>) response.getBody().get("appointments");
            if (appointments != null) {
                for (Map<String, Object> app : appointments) {
                    bookedSlots.add(app.get("from_time").toString());
                }
            }
        } catch (Exception ignored) {
        }
        return bookedSlots;
    }
}
