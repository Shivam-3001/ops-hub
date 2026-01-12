package com.company.ops_hub_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewProfileUpdateRequestDTO {
    
    @NotNull(message = "Request ID is required")
    private Long requestId;
    
    @NotNull(message = "Action is required")
    @NotBlank(message = "Action cannot be blank")
    private String action; // APPROVE, REJECT
    
    private String reviewNotes;
}
