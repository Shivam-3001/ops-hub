package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.PermissionDTO;
import com.company.ops_hub_api.dto.RoleDTO;
import com.company.ops_hub_api.dto.UserPermissionsDTO;
import com.company.ops_hub_api.domain.Permission;
import com.company.ops_hub_api.domain.Role;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.security.UserPrincipal;
import com.company.ops_hub_api.service.PermissionService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/permissions")
@RequiredArgsConstructor
public class PermissionController {

    private final PermissionService permissionService;

    @GetMapping("/me")
    public ResponseEntity<UserPermissionsDTO> getCurrentUserPermissions() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
            
            UserPermissionsDTO dto = UserPermissionsDTO.builder()
                    .userId(userPrincipal.getUserId())
                    .employeeId(userPrincipal.getEmployeeId())
                    .username(userPrincipal.getUsername())
                    .roles(userPrincipal.getRoles())
                    .permissions(userPrincipal.getPermissions())
                    .build();
            
            return ResponseEntity.ok(dto);
        }
        return ResponseEntity.badRequest().build();
    }

    @GetMapping("/all")
    @RequiresPermission("VIEW_PERMISSIONS")
    public ResponseEntity<List<PermissionDTO>> getAllPermissions() {
        List<Permission> permissions = permissionService.getAllActivePermissions();
        List<PermissionDTO> dtos = permissions.stream()
                .map(this::toDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    @GetMapping("/roles")
    @RequiresPermission("VIEW_ROLES")
    public ResponseEntity<List<RoleDTO>> getAllRoles() {
        List<Role> roles = permissionService.getAllActiveRoles();
        List<RoleDTO> dtos = roles.stream()
                .map(this::toRoleDTO)
                .collect(Collectors.toList());
        return ResponseEntity.ok(dtos);
    }

    private PermissionDTO toDTO(Permission permission) {
        return PermissionDTO.builder()
                .id(permission.getId())
                .code(permission.getCode())
                .name(permission.getName())
                .description(permission.getDescription())
                .resource(permission.getResource())
                .action(permission.getAction())
                .active(permission.getActive())
                .build();
    }

    private RoleDTO toRoleDTO(Role role) {
        List<PermissionDTO> permissionDTOs = role.getPermissions() != null ?
                role.getPermissions().stream()
                        .filter(Permission::getActive)
                        .map(this::toDTO)
                        .collect(Collectors.toList()) : List.of();

        return RoleDTO.builder()
                .id(role.getId())
                .code(role.getCode())
                .name(role.getName())
                .description(role.getDescription())
                .active(role.getActive())
                .permissions(permissionDTOs)
                .build();
    }
}
