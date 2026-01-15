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
public class UserManagementUserDTO {

    private Long id;
    private String employeeId;
    private String username;
    private String fullName;
    private String email;
    private String phone;
    private String role;
    private String userType;
    private Boolean active;
    private String areaName;
    private String zoneName;
    private String circleName;
    private String clusterName;
    private LocalDateTime createdAt;
}
