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
                "upload",
                "uploads",
                "excel",
                "csv",
                "import",
                "bulk upload")) {
            return AiIntent.UPLOADS_SUMMARY;
        }
        if (containsAny(normalized,
                "notification",
                "notifications",
                "alert",
                "alerts",
                "escalation",
                "escalations")) {
            return AiIntent.NOTIFICATIONS_SUMMARY;
        }
        if (containsAny(normalized,
                "allocation",
                "allocations",
                "assign",
                "assignment",
                "assigned",
                "unassigned")) {
            return AiIntent.ALLOCATION_SUMMARY;
        }
        if (containsAny(normalized,
                "customer",
                "customers",
                "delinquent",
                "portfolio",
                "bucket",
                "customer status",
                "customer summary")) {
            return AiIntent.CUSTOMER_STATUS_SUMMARY;
        }
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
                "paid",
                "successful payments",
                "collections done",
                "payment success",
                "payments success",
                "payment received",
                "payments received")) {
            return AiIntent.PAYMENTS_SUMMARY;
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
        if (containsAny(normalized,
                "summary",
                "overview",
                "dashboard",
                "snapshot",
                "status overview",
                "health check")) {
            return AiIntent.GENERAL_SUMMARY;
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
