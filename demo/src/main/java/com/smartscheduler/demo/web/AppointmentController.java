package com.smartscheduler.demo.web;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.smartscheduler.demo.service.ZohoTokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestTemplate;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;

// Helper class to map the incoming JSON structure for creating appointments
class DailyScheduleRequest {
    private List<String> staff_ids;
    private List<String> daily_slot_times; // Now accepts only time strings (e.g., "10:00:00")
    private int total_days; // New parameter: how many days to schedule for

    // Standard getters and setters
    public List<String> getStaff_ids() { return staff_ids; }
    public void setStaff_ids(List<String> staff_ids) { this.staff_ids = staff_ids; }
    public List<String> getDaily_slot_times() { return daily_slot_times; }
    public void setDaily_slot_times(List<String> daily_slot_times) { this.daily_slot_times = daily_slot_times; }
    public int getTotal_days() { return total_days; }
    public void setTotal_days(int total_days) { this.total_days = total_days; }
}

// Helper class for the deletion request (only needs service ID) - kept from previous version
class DeleteAppointmentRequest {
    private String service_id;

    public String getService_id() { return service_id; }
    public void setService_id(String service_id) { this.service_id = service_id; }
}


@RestController
@RequestMapping("/appointment")
public class AppointmentController {

    private static final String TIMEZONE = "Asia/Kolkata";
    private static final String ZOHO_BOOKING_API_BASE = "https://www.zohoapis.in/bookings/v1/json";

    // Zoho Bookings API requires this specific format: dd-MMM-yyyy HH:mm:ss
    private final DateTimeFormatter apiFormatter = DateTimeFormatter.ofPattern("dd-MMM-yyyy HH:mm:ss");
    // Formatter for parsing incoming time strings
    private final DateTimeFormatter timeFormatter = DateTimeFormatter.ofPattern("HH:mm:ss");

    private final RestTemplate restTemplate = new RestTemplate();
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final Random random = new Random();

    @Autowired
    private ZohoTokenService zohoTokenService;

    // --- METHOD FOR CREATING APPOINTMENTS ---
    @PostMapping("/services/{service-id}")
    public List<Map<String, Object>> createAppointments(@PathVariable("service-id") String serviceId, @RequestBody DailyScheduleRequest request) {

        List<Map<String, Object>> results = new ArrayList<>();
        List<String> staffIds = request.getStaff_ids();
        List<String> dailySlotTimes = request.getDaily_slot_times();
        int totalDays = request.getTotal_days();

        // 1. Basic validation
        if (staffIds == null || staffIds.isEmpty()) {
            results.add(Map.of("status", "error", "message", "No staff IDs provided."));
            return results;
        }
        if (dailySlotTimes == null || dailySlotTimes.isEmpty()) {
            results.add(Map.of("status", "error", "message", "No daily slot times provided."));
            return results;
        }
        if (totalDays <= 0) {
            results.add(Map.of("status", "error", "message", "Total days must be a positive number."));
            return results;
        }

        String accessToken = zohoTokenService.getAccessToken();

        // 2. Fetch already booked slots (optional optimization)
        Set<String> bookedSlots = fetchExistingAppointments(serviceId);

        // 3. Start scheduling from tomorrow
        LocalDate startDate = LocalDate.now().plusDays(1);

        // 4. Loop through the required number of days
        for (int dayOffset = 0; dayOffset < totalDays; dayOffset++) {
            LocalDate currentDay = startDate.plusDays(dayOffset);

            // 5. Loop through the desired slot times for the current day
            for (String timeString : dailySlotTimes) {
                String staffId = staffIds.get(random.nextInt(staffIds.size())); // Random staff selection

                try {
                    LocalTime slotTime = LocalTime.parse(timeString, timeFormatter);
                    LocalDateTime appointmentDateTime = currentDay.atTime(slotTime);
                    String zohoFormattedSlot = appointmentDateTime.format(apiFormatter);

                    // 6. Check for existing booking
                    if (bookedSlots.contains(zohoFormattedSlot)) {
                        results.add(Map.of("status", "skipped", "slot", zohoFormattedSlot, "staff_id", staffId, "reason", "Slot already booked."));
                        continue;
                    }

                    // 7. Prepare Customer Details (unique per appointment)
                    String customerKey = staffId + "_" + zohoFormattedSlot.replace(" ", "_").replace("-", "_").replace(":", "");
                    String uniquePhoneSuffix = String.valueOf(Math.abs(customerKey.hashCode() % 90000000) + 1000000000);

                    Map<String, String> customer = new HashMap<>();
                    customer.put("name", "Demo User " + customerKey.substring(0, 10)); // Truncate name for simplicity
                    customer.put("email", "demo." + customerKey + "@example.com");
                    customer.put("phone_number", "+91" + uniquePhoneSuffix);

                    // --- Zoho API Request Setup ---
                    MultiValueMap<String, Object> form = new LinkedMultiValueMap<>();
                    form.add("service_id", serviceId);
                    form.add("staff_id", staffId);
                    form.add("from_time", zohoFormattedSlot);
                    form.add("timezone", TIMEZONE);
                    form.add("customer_details", objectMapper.writeValueAsString(customer));
                    form.add("notes", "Automated schedule, Staff: " + staffId);

                    HttpHeaders headers = new HttpHeaders();
                    headers.setContentType(MediaType.MULTIPART_FORM_DATA);
                    headers.set("Authorization", "Zoho-oauthtoken " + accessToken);

                    HttpEntity<MultiValueMap<String, Object>> httpEntity = new HttpEntity<>(form, headers);

                    // API Call to Book Appointment
                    ResponseEntity<Map> response = restTemplate.postForEntity(
                            ZOHO_BOOKING_API_BASE + "/appointment",
                            httpEntity,
                            Map.class
                    );
                    // --- End Zoho API Request Setup ---

                    results.add(Map.of(
                            "status", "success",
                            "staff_id", staffId,
                            "slot", zohoFormattedSlot,
                            "customer_email", customer.get("email"),
                            "response_code", response.getStatusCodeValue()
                    ));

                } catch (DateTimeParseException e) {
                    results.add(Map.of("status", "failed", "time", timeString, "error", "Invalid time format. Expected HH:mm:ss."));
                } catch (HttpClientErrorException e) {
                    results.add(Map.of("status", "failed", "staff_id", staffId, "slot", timeString, "error_message", "HTTP Error: " + e.getRawStatusCode(), "error_body", e.getResponseBodyAsString()));
                } catch (Exception e) {
                    results.add(Map.of("status", "failed", "staff_id", staffId, "slot", timeString, "error", "General Exception: " + e.getMessage()));
                }
            }
        }
        return results;
    }


    @DeleteMapping("/services/{service-id}")
    public Map<String, Object> deleteAppointments(@PathVariable("service-id") String serviceId) {
        if (serviceId == null || serviceId.isEmpty()) {
            return Map.of("status", "error", "message", "Service ID must be provided in the path for deletion.");
        }

        Map<String, Object> result = new HashMap<>();
        List<String> deletedIds = new ArrayList<>();
        String accessToken = zohoTokenService.getAccessToken();

        // This is where the error originates, due to a bad access token or server issue
        String fetchAppointmentsUrl = ZOHO_BOOKING_API_BASE + "/appointments?service_id=" + serviceId;

        try {
            // 1. Fetch all appointments for the service
            HttpHeaders headers = new HttpHeaders();
            headers.set("Authorization", "Zoho-oauthtoken " + accessToken);
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    fetchAppointmentsUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            // ... (omitted logic to parse appointments)
            Map responseBody = response.getBody();
            List<Map<String, Object>> appointments = null;
            if (responseBody != null && responseBody.containsKey("response") && ((Map)responseBody.get("response")).containsKey("returnvalue")) {
                appointments = (List<Map<String, Object>>) ((Map)responseBody.get("response")).get("returnvalue");
            }
            // ... (omitted deletion loop)
            if (appointments != null) {
                // 2. Loop and delete each appointment
                for (Map<String, Object> app : appointments) {
                    String appointmentId = app.get("appointment_id").toString();
                    try {
                        restTemplate.exchange(
                                ZOHO_BOOKING_API_BASE + "/appointment/" + appointmentId,
                                HttpMethod.DELETE,
                                entity,
                                String.class
                        );
                        deletedIds.add(appointmentId);
                    } catch (HttpClientErrorException deleteException) {
                        System.err.println("Failed to delete appointment " + appointmentId + ": " + deleteException.getResponseBodyAsString());
                        result.put("error_on_" + appointmentId, deleteException.getResponseBodyAsString());
                    }
                }
            }


            result.put("deleted_appointments_count", deletedIds.size());
            result.put("deleted_appointments_ids", deletedIds);
            result.put("status", "success");
        } catch (HttpClientErrorException e) {
            // Catch 4xx errors (e.g., 401 Unauthorized due to bad token)
            result.put("status", "error");
            result.put("message", "Error fetching appointments (Client Error): " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        } catch (HttpServerErrorException e) {
            // Catch 5xx errors (e.g., 500 Internal Server Error from Zoho)
            result.put("status", "error");
            result.put("message", "Error fetching appointments (Server Error): " + e.getRawStatusCode() + " " + e.getResponseBodyAsString());
        } catch (Exception e) {
            // Catch all other exceptions (e.g., I/O issues, JSON mapping issues)
            result.put("status", "error");
            result.put("message", "Error fetching/deleting appointments: " + e.getMessage());
        }

        return result;
    }

    /**
     * Fetches all existing appointments for a given service and returns a Set of booked 'from_time' strings.
     * (Fixed Error Handling)
     */
    private Set<String> fetchExistingAppointments(String serviceId) {
        Set<String> bookedSlots = new HashSet<>();
        String fetchAppointmentsUrl = ZOHO_BOOKING_API_BASE + "/appointments?service_id=" + serviceId;

        try {
            HttpHeaders headers = new HttpHeaders();
            // The getAccessToken() call is the most common failure point
            headers.set("Authorization", "Zoho-oauthtoken " + zohoTokenService.getAccessToken());
            HttpEntity<String> entity = new HttpEntity<>(headers);

            ResponseEntity<Map> response = restTemplate.exchange(
                    fetchAppointmentsUrl,
                    HttpMethod.GET,
                    entity,
                    Map.class
            );

            Map responseBody = response.getBody();
            if (responseBody != null && responseBody.containsKey("response") && ((Map)responseBody.get("response")).containsKey("returnvalue")) {
                List<Map<String, Object>> appointments = (List<Map<String, Object>>) ((Map)responseBody.get("response")).get("returnvalue");

                if (appointments != null) {
                    for (Map<String, Object> app : appointments) {
                        if (app.containsKey("from_time") && app.get("from_time") != null) {
                            bookedSlots.add(app.get("from_time").toString());
                        }
                    }
                }
            }
        } catch (HttpClientErrorException | HttpServerErrorException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return bookedSlots;
    }
}