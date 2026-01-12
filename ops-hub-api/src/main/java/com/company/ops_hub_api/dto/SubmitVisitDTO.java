package com.company.ops_hub_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitVisitDTO {
    
    @NotNull(message = "Customer ID is required")
    private Long customerId;
    
    @NotNull(message = "Visit date is required")
    private LocalDateTime visitDate;
    
    @NotBlank(message = "Visit status is required")
    private String visitStatus; // SCHEDULED, IN_PROGRESS, COMPLETED, CANCELLED
    
    @NotBlank(message = "Notes are required")
    private String notes;
    
    private String visitType; // SCHEDULED, UNSCHEDULED, FOLLOW_UP
    private String purpose;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
}
