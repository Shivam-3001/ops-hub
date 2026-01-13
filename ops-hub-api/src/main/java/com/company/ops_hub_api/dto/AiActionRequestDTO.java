package com.company.ops_hub_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

/**
 * AI Action Request DTO
 * Request to execute an AI-suggested action
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class AiActionRequestDTO {
    
    @NotBlank(message = "Action type is required")
    private String actionType;
    
    @NotBlank(message = "Action name is required")
    private String actionName;
    
    private String conversationId;
    
    private Map<String, Object> actionData;
    
    private String confirmationToken; // Required for restricted actions
}
