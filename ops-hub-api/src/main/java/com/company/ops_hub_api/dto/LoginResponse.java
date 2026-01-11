package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LoginResponse {
    private String token;
    private String employeeId;
    private String username;
    private String fullName;
    private String userType;
    private String role;
    private String areaName;
    private String zoneName;
    private String circleName;
    private String clusterName;
}
