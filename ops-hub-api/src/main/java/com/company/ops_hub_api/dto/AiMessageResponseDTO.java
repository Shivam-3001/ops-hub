package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * AI Message Response DTO
 * Response from AI agent
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiMessageResponseDTO {
    private String conversationId;
    private String response;
    private List<AiActionSuggestionDTO> suggestedActions;
    private boolean requiresConfirmation;
    private String confirmationMessage;
    private AiContextDTO context;
}
