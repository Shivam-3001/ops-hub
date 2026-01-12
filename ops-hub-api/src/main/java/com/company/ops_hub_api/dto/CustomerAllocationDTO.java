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
public class CustomerAllocationDTO {
    private Long id;
    private Long customerId;
    private String customerCode;
    private String customerName;
    private Long userId;
    private String userEmployeeId;
    private String userName;
    private String roleCode;
    private String allocationType; // PRIMARY, SECONDARY, TEMPORARY
    private String status; // ACTIVE, INACTIVE, TRANSFERRED
    private Long allocatedBy;
    private String allocatedByEmployeeId;
    private String allocatedByName;
    private LocalDateTime allocatedAt;
    private LocalDateTime deallocatedAt;
    private String deallocationReason;
    private String notes;
}
