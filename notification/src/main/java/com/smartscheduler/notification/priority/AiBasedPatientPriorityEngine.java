package com.smartscheduler.notification.priority;

import com.smartscheduler.common.util.DateUtils;
import com.smartscheduler.notification.client.feign.AiPriorityFeignClient;
import com.smartscheduler.notification.model.PatientPriority;
import feign.FeignException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
@ConditionalOnProperty(name = "notification.patient.priority.engine", havingValue = "ai-based")
public class AiBasedPatientPriorityEngine implements PatientPriorityEngine {

    private final AiPriorityFeignClient aiClient;

    public AiBasedPatientPriorityEngine(AiPriorityFeignClient aiClient) {
        this.aiClient = aiClient;
    }

    @Override
    public List<PatientPriority> rankPatientsByPriority(LocalDateTime cancelledSlot, List<PatientPriority> patients) {
        if (patients == null || patients.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            // Build request body dynamically
            Map<String, Object> request = new HashMap<>();
            request.put("cancelled_slot", DateUtils.toFormattedDateTimeString(cancelledSlot));

            List<Map<String, Object>> userDetails = patients.stream().map(p -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", String.valueOf(p.getId()));
                map.put("is_vip", p.getIsVip() ? 1 : 0);
                map.put("severity_level", p.getSeverityLevel());
                map.put("total_notifications_responded", p.getTotalNotificationsResponded());
                map.put("total_notifications_sent", p.getTotalNotificationsSent());
                map.put("booking_history", p.getBookingHistory()
                        .stream()
                        .map(DateUtils::toFormattedDateTimeString)
                        .collect(Collectors.toList()));
                return map;
            }).collect(Collectors.toList());

            request.put("user_details", userDetails);

            log.info("Calling AI model with payload: {}", request);

            // Call the ML model API — returns sorted list already
            List<Map<String, Object>> aiResponse = aiClient.getPriorityScores(request);

            // Map response to PatientPriority list (in same order as response)
            List<PatientPriority> sortedPatients = new ArrayList<>();
            for (Map<String, Object> r : aiResponse) {
                String id = (String) r.get("id");
                double score = ((Number) r.get("score")).doubleValue();

                patients.stream()
                        .filter(p -> p.getId().equals(id))
                        .findFirst()
                        .ifPresent(p -> {
                            p.setScore(score);
                            sortedPatients.add(p);
                        });
            }

            return sortedPatients;

        } catch (Exception e) {
            log.error("AI Priority ranking failed, fallback to random scores", e);
            for (PatientPriority p : patients) {
                p.setScore(Math.random());
            }
            return patients;
        }
    }

}
