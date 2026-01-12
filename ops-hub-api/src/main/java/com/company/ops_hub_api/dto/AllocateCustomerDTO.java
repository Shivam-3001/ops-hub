package com.company.ops_hub_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class AllocateCustomerDTO {
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotNull(message = "User ID is required")
    private Long userId;
    
    @NotBlank(message = "Role code is required")
    private String roleCode;
    
    @NotBlank(message = "Allocation type is required")
    private String allocationType; // PRIMARY, SECONDARY, TEMPORARY
    
    private String notes;
}
