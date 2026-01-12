package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerVisitDTO {
    private Long id;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private Long userId;
    private String userEmployeeId;
    private String userName;
    private LocalDateTime visitDate;
    private String visitType;
    private String purpose;
    private String notes;
    private BigDecimal latitude;
    private BigDecimal longitude;
    private String address;
    private String visitStatus;
    private LocalDateTime scheduledAt;
    private LocalDateTime startedAt;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private Boolean hasReview;
}
