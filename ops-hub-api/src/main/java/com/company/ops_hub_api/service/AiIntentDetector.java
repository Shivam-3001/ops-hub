package com.company.ops_hub_api.service;

import org.springframework.stereotype.Component;

@Component
public class AiIntentDetector {

    public AiIntent detect(String question) {
        if (question == null) {
            return AiIntent.UNKNOWN;
        }
        String normalized = question.trim().toLowerCase();
        if (containsAny(normalized,
                "pending payment",
                "pending payments",
                "pending amount",
                "payment pending",
                "payments pending",
                "payment due",
                "payments due",
                "overdue",
                "over due",
                "unpaid",
                "payment status",
                "collection",
                "collections",
                "receivable",
                "receivables",
                "payment summary",
                "payment totals",
                "outstanding")) {
            return AiIntent.PENDING_PAYMENTS_SUMMARY;
        }
        if (containsAny(normalized,
                "agent performance",
                "agent stats",
                "agent summary",
                "agent productivity",
                "agent efficiency",
                "field agent",
                "field agents",
                "agent workload",
                "agent allocations",
                "agent visits",
                "agent payments",
                "agent metrics")) {
            return AiIntent.AGENT_PERFORMANCE;
        }
        if (containsAny(normalized,
                "visit summary",
                "visit stats",
                "visit status",
                "visit performance",
                "visits",
                "field visits",
                "site visits",
                "follow up",
                "followup")) {
            return AiIntent.VISIT_SUMMARY;
        }
        return AiIntent.UNKNOWN;
    }

    private boolean containsAny(String text, String... keywords) {
        if (text == null || text.isBlank()) {
            return false;
        }
        for (String keyword : keywords) {
            if (text.contains(keyword)) {
                return true;
            }
        }
        return false;
    }
}
