package com.smartscheduler.notification.priority;

import com.smartscheduler.notification.config.PriorityWeightProperties;
import com.smartscheduler.notification.model.PatientPriority;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.DayOfWeek;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;

@Service
@ConditionalOnProperty(name = "notification.patient.priority.engine", havingValue = "rule-based")
public class RuleBasedPatientPriorityEngine implements PatientPriorityEngine {

    private final PriorityWeightProperties weights;

    public RuleBasedPatientPriorityEngine(PriorityWeightProperties weights) {
        this.weights = weights;
    }

    @Override
    public List<PatientPriority> rankPatientsByPriority(LocalDateTime cancelledSlot, List<PatientPriority> patients) {
        if (patients == null || patients.isEmpty()) {
            return List.of();
        }

        DayOfWeek cancelledDay = cancelledSlot.getDayOfWeek();
        int cancelledMinutes = cancelledSlot.getHour() * 60 + cancelledSlot.getMinute();

        for (PatientPriority patient : patients) {
            double acceptanceRate = (patient.getTotalNotificationsSent() > 0)
                    ? (double) patient.getTotalNotificationsResponded() / patient.getTotalNotificationsSent()
                    : 0.0;

            List<LocalDateTime> history = patient.getBookingHistory();
            if (history == null || history.isEmpty()) {
                patient.setScore(0.0);
                continue;
            }

            // Average difference in minutes from cancelled slot
            double avgTimeDifference = history.stream()
                    .mapToDouble(h -> Math.abs(((h.getHour() * 60 + h.getMinute()) - cancelledMinutes)))
                    .average()
                    .orElse(180.0);

            // Normalize time difference — smaller is better
            double timeDifferenceScore = normalizeInverse(avgTimeDifference, 180);

            // Consistency score (how close bookings are to each other)
            double stdDev = calculateStdDev(
                    history.stream().mapToInt(h -> h.getHour() * 60 + h.getMinute()).boxed().toList()
            );
            double timeConsistencyScore = normalizeInverse(stdDev, 90);

            // Day match score (how often patient books same weekday)
            long dayMatches = history.stream().filter(h -> h.getDayOfWeek().equals(cancelledDay)).count();
            double dayScore = (double) dayMatches / history.size();

            // Compute weighted score
            double score =
                    (patient.getIsVip() ? 1.0 : 0.0) * weights.getIsVip() +
                            normalizeSeverity(patient.getSeverityLevel()) * weights.getSeverityLevel() +
                            acceptanceRate * weights.getAcceptanceRate() +
                            timeDifferenceScore * weights.getTimeDifference() +
                            timeConsistencyScore * weights.getTimeConsistencyStdDev();

            // Add slight bias for weekday match
            score += (dayScore * 0.05);

            patient.setScore(score);
        }

        // Sort descending by score
        return patients.stream()
                .sorted(Comparator.comparingDouble(PatientPriority::getScore).reversed())
                .collect(Collectors.toList());
    }

    private double calculateStdDev(List<Integer> values) {
        if (values.isEmpty()) return 90.0;
        double mean = values.stream().mapToInt(Integer::intValue).average().orElse(0.0);
        double variance = values.stream()
                .mapToDouble(v -> Math.pow(v - mean, 2))
                .average()
                .orElse(0.0);
        return Math.sqrt(variance);
    }

    private double normalizeInverse(double value, double max) {
        // Smaller values are better
        return 1.0 - Math.min(value / max, 1.0);
    }

    private double normalizeSeverity(int severityLevel) {
        // Assuming range 0–10
        return Math.min(Math.max(severityLevel / 10.0, 0.0), 1.0);
    }
}
