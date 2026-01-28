package com.company.ops_hub_api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AssignableUserDTO {
    private Long id;
    private String employeeId;
    private String username;
    private String fullName;
    private String userType;
    private String areaName;
    private String zoneName;
    private String circleName;
    private String clusterName;
}
