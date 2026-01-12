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
public class ProfileUpdateRequestDTO {
    private Long id;
    private Long userId;
    private String userEmployeeId;
    private String userName;
    private Long profileId;
    private String requestType; // CREATE, UPDATE, DELETE
    private String fieldName;
    private String oldValue;
    private String newValue;
    private String reason;
    private String status; // PENDING, APPROVED, REJECTED
    private Long requestedBy;
    private String requestedByEmployeeId;
    private LocalDateTime requestedAt;
    private Long reviewedBy;
    private String reviewedByEmployeeId;
    private LocalDateTime reviewedAt;
    private String reviewNotes;
}
