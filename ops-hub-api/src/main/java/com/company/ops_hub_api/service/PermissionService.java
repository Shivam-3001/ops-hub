package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Permission;
import com.company.ops_hub_api.domain.Role;
import com.company.ops_hub_api.repository.PermissionRepository;
import com.company.ops_hub_api.repository.RoleRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PermissionService {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;

    /**
     * Check if current user has a specific permission
     */
    public boolean hasPermission(String permissionCode) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return userPrincipal != null && userPrincipal.hasPermission(permissionCode);
    }

    /**
     * Check if current user has any of the specified permissions
     */
    public boolean hasAnyPermission(String... permissionCodes) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return userPrincipal != null && userPrincipal.hasAnyPermission(permissionCodes);
    }

    /**
     * Check if current user has all of the specified permissions
     */
    public boolean hasAllPermissions(String... permissionCodes) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal == null) {
            return false;
        }
        for (String permission : permissionCodes) {
            if (!userPrincipal.hasPermission(permission)) {
                return false;
            }
        }
        return true;
    }

    /**
     * Check if current user has a specific role
     */
    public boolean hasRole(String roleCode) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return userPrincipal != null && userPrincipal.hasRole(roleCode);
    }

    /**
     * Get all permissions for current user
     */
    public Set<String> getCurrentUserPermissions() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return userPrincipal != null ? userPrincipal.getPermissions() : Set.of();
    }

    /**
     * Get all roles for current user
     */
    public Set<String> getCurrentUserRoles() {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        return userPrincipal != null ? userPrincipal.getRoles() : Set.of();
    }

    /**
     * Get all permissions for a role
     */
    public List<Permission> getPermissionsByRole(String roleCode) {
        return permissionRepository.findByRoleCode(roleCode);
    }

    /**
     * Get all active permissions
     */
    public List<Permission> getAllActivePermissions() {
        return permissionRepository.findAll().stream()
                .filter(Permission::getActive)
                .collect(Collectors.toList());
    }

    /**
     * Get all active roles
     */
    public List<Role> getAllActiveRoles() {
        return roleRepository.findAll().stream()
                .filter(Role::getActive)
                .collect(Collectors.toList());
    }

    private UserPrincipal getCurrentUserPrincipal() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
            return (UserPrincipal) authentication.getPrincipal();
        }
        return null;
    }
}
