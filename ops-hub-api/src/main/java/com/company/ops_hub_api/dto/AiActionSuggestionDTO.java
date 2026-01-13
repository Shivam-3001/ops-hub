package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI Action Suggestion DTO
 * Suggested action from AI agent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiActionSuggestionDTO {
    private String actionType; // DATA_QUERY, REPORT_GENERATION, etc.
    private String actionName;
    private String description;
    private boolean requiresPermission;
    private String requiredPermission;
    private boolean requiresConfirmation;
    private Map<String, Object> actionData;
}
