package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;
import java.util.Map;

/**
 * AI Context DTO
 * Contains context information for AI agent to be aware of user's current state
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AiContextDTO {
    private Long userId;
    private String employeeId;
    private String username;
    private String fullName;
    private List<String> roles;
    private List<String> permissions;
    private String currentPage;
    private String currentModule;
    private Map<String, Object> additionalContext;
}
