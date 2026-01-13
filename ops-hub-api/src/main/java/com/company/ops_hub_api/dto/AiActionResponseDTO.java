package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * AI Action Response DTO
 * Response from executing an AI action
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiActionResponseDTO {
    private Long actionId;
    private String actionType;
    private String actionName;
    private String executionStatus; // PENDING, EXECUTING, COMPLETED, FAILED
    private Object resultData;
    private String errorMessage;
    private boolean requiresConfirmation;
    private String confirmationToken;
}
