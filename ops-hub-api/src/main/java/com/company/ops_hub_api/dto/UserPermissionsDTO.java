package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Set;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermissionsDTO {
    private Long userId;
    private String employeeId;
    private String username;
    private Set<String> roles;
    private Set<String> permissions;
}
