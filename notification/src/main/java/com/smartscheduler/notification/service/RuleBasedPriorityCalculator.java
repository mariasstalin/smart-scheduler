package com.smartscheduler.notification.service;

import com.smartscheduler.common.entity.Patient;
import com.smartscheduler.notification.config.ScoringProperties;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class RuleBasedPriorityCalculator implements PriorityCalculator {

    private final ScoringProperties props;
    private static final double MAX_SCORE_CAP = 100.0;
    private static final double MIN_SCORE_CAP = 0.0;

    @Override
    public double calculateScore(Patient patient) {
        if (patient == null) {
            return MIN_SCORE_CAP;
        }

        double score = 0.0;

        score += patient.getSeverityLevel() * props.getSeverityLevel();

        double visitScore = patient.getVisitCount() / props.getVisitCountDivisor() * props.getVisitCountMaxScore();
        score += Math.min(visitScore, props.getVisitCountMaxScore());

        if (Boolean.TRUE.equals(patient.getVip())) {
            score += props.getVip();
        }

        if (patient.getTotalNotificationsSent() > 0) {
            double responseRate = (double) patient.getTotalNotificationsResponded() / patient.getTotalNotificationsSent();
            score += responseRate * props.getResponseRateWeight();
        }

        double missPenalty = patient.getConsecutiveMisses() * props.getConsecutiveMissPenalty();
        score -= Math.min(missPenalty, props.getConsecutiveMissMaxPenalty());

        return Math.max(MIN_SCORE_CAP, Math.min(score, MAX_SCORE_CAP));
    }
}