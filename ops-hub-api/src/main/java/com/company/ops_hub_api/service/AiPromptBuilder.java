package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.util.HierarchyUtil;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Component
public class AiPromptBuilder {

    public String buildPrompt(User user,
                              String question,
                              AiIntent intent,
                              String businessSummary,
                              List<Map<String, String>> recentMessages) {
        String userType = HierarchyUtil.normalizeUserType(user);
        Map<String, Object> geo = HierarchyUtil.buildGeographyContext(user);

        String history = recentMessages.stream()
                .map(entry -> entry.get("role") + ": " + entry.get("content"))
                .collect(Collectors.joining("\n"));

        return String.join("\n",
                "SYSTEM:",
                "You are Ops Hub AI Assistant.",
                "You are read-only and must never perform actions.",
                "Do not output SQL, queries, or database schema details.",
                "Do not reveal PII or sensitive identifiers.",
                "If asked to perform restricted actions, refuse.",
                "",
                "USER CONTEXT:",
                "Role: " + userType,
                "Geography: " + geo,
                "Intent: " + intent.name(),
                "",
                "AUTHORIZED BUSINESS SUMMARY:",
                businessSummary,
                "",
                "RECENT CONVERSATION:",
                history.isEmpty() ? "(no previous messages)" : history,
                "",
                "USER QUESTION:",
                question,
                "",
                "RESPONSE:"
        );
    }
}
