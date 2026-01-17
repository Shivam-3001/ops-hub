package com.company.ops_hub_api.service;

import org.springframework.stereotype.Component;

@Component
public class AiIntentDetector {

    public AiIntent detect(String question) {
        if (question == null) {
            return AiIntent.UNKNOWN;
        }
        String normalized = question.trim().toLowerCase();
        if (normalized.contains("pending payment")
                || normalized.contains("pending amount")
                || normalized.contains("payment pending")
                || normalized.contains("unpaid")) {
            return AiIntent.PENDING_PAYMENTS_SUMMARY;
        }
        if (normalized.contains("agent performance")
                || normalized.contains("agent stats")
                || normalized.contains("agent summary")
                || normalized.contains("agent productivity")) {
            return AiIntent.AGENT_PERFORMANCE;
        }
        if (normalized.contains("visit summary")
                || normalized.contains("visit stats")
                || normalized.contains("visits")
                || normalized.contains("visit performance")) {
            return AiIntent.VISIT_SUMMARY;
        }
        return AiIntent.UNKNOWN;
    }
}
