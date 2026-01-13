package com.company.ops_hub_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI Message Request DTO
 * Request for AI conversation/message
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiMessageRequestDTO {
    
    @NotBlank(message = "Message is required")
    private String message;
    
    private String conversationId; // Optional: for continuing existing conversation
    
    private String currentPage; // Current page/module context
    
    private String currentModule; // Current module context
    
    private Map<String, Object> context; // Additional context data
}
