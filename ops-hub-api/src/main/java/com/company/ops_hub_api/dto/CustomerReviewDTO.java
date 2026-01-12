package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerReviewDTO {
    private Long id;
    private Long visitId;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private Long userId;
    private String userEmployeeId;
    private String userName;
    private Integer rating;
    private String reviewText;
    private String reviewCategories;
    private Boolean isPositive;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
