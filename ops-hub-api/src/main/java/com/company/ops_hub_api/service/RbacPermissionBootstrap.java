package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Permission;
import com.company.ops_hub_api.domain.Role;
import com.company.ops_hub_api.domain.RolePermission;
import com.company.ops_hub_api.repository.PermissionRepository;
import com.company.ops_hub_api.repository.RolePermissionRepository;
import com.company.ops_hub_api.repository.RoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RbacPermissionBootstrap {

    private final PermissionRepository permissionRepository;
    private final RoleRepository roleRepository;
    private final RolePermissionRepository rolePermissionRepository;

    @Bean
    @Transactional
    public CommandLineRunner ensurePermissionsAndRoles() {
        return args -> {
            Map<String, PermissionSpec> permissionSpecs = buildPermissionSpecs();
            upsertPermissions(permissionSpecs);
            mapPermissionsToRoles(permissionSpecs);
        };
    }

    private Map<String, PermissionSpec> buildPermissionSpecs() {
        Map<String, PermissionSpec> specs = new LinkedHashMap<>();
        specs.put("VIEW_CUSTOMERS", new PermissionSpec("View Customers", "View customer information", "CUSTOMER", "VIEW"));
        specs.put("ASSIGN_CUSTOMERS", new PermissionSpec("Assign Customers", "Assign customers to users", "CUSTOMER", "ASSIGN"));
        specs.put("COLLECT_PAYMENT", new PermissionSpec("Collect Payment", "Collect payments from customers", "PAYMENT", "CREATE"));
        specs.put("APPROVE_PROFILE", new PermissionSpec("Approve Profile", "Approve user profile updates", "PROFILE", "APPROVE"));
        specs.put("VIEW_REPORTS", new PermissionSpec("View Reports", "View reports and analytics", "REPORT", "VIEW"));
        specs.put("EXPORT_REPORTS", new PermissionSpec("Export Reports", "Export reports in various formats", "REPORT", "EXPORT"));
        specs.put("USE_AI_AGENT", new PermissionSpec("Use AI Agent", "Access and use AI assistant features", "AI", "USE"));
        specs.put("VIEW_PERMISSIONS", new PermissionSpec("View Permissions", "View roles and permissions", "PERMISSION", "VIEW"));
        specs.put("VIEW_ROLES", new PermissionSpec("View Roles", "View role definitions", "ROLE", "VIEW"));
        specs.put("MANAGE_USERS", new PermissionSpec("Manage Users", "Create, update, and delete users", "USER", "MANAGE"));
        specs.put("MANAGE_CUSTOMERS", new PermissionSpec("Manage Customers", "Create, update, and delete customers", "CUSTOMER", "MANAGE"));
        specs.put("VIEW_VISITS", new PermissionSpec("View Visits", "View customer visit records", "VISIT", "VIEW"));
        specs.put("CREATE_VISITS", new PermissionSpec("Create Visits", "Create customer visit records", "VISIT", "CREATE"));
        specs.put("VIEW_PAYMENTS", new PermissionSpec("View Payments", "View payment transactions", "PAYMENT", "VIEW"));
        specs.put("MANAGE_SETTINGS", new PermissionSpec("Manage Settings", "Manage application settings", "SETTING", "MANAGE"));
        specs.put("VIEW_NOTIFICATIONS", new PermissionSpec("View Notifications", "View in-app notifications", "NOTIFICATION", "VIEW"));
        specs.put("VIEW_AUDIT_LOGS", new PermissionSpec("View Audit Logs", "View audit trail logs", "AUDIT", "VIEW"));
        return specs;
    }

    private void upsertPermissions(Map<String, PermissionSpec> specs) {
        for (Map.Entry<String, PermissionSpec> entry : specs.entrySet()) {
            String code = entry.getKey();
            PermissionSpec spec = entry.getValue();
            Permission permission = permissionRepository.findByCode(code).orElseGet(Permission::new);
            permission.setCode(code);
            permission.setName(spec.name());
            permission.setDescription(spec.description());
            permission.setResource(spec.resource());
            permission.setAction(spec.action());
            permission.setActive(true);
            if (permission.getCreatedAt() == null) {
                permission.setCreatedAt(LocalDateTime.now());
            }
            permission.setUpdatedAt(LocalDateTime.now());
            permissionRepository.save(permission);
        }
    }

    private void mapPermissionsToRoles(Map<String, PermissionSpec> specs) {
        Map<String, List<String>> rolePermissions = new HashMap<>();
        List<String> allPermissions = new ArrayList<>(specs.keySet());
        rolePermissions.put("ADMIN", allPermissions);

        List<String> headPermissions = Arrays.asList(
                "VIEW_CUSTOMERS", "ASSIGN_CUSTOMERS", "COLLECT_PAYMENT", "APPROVE_PROFILE",
                "VIEW_REPORTS", "EXPORT_REPORTS", "USE_AI_AGENT", "VIEW_PERMISSIONS", "VIEW_ROLES",
                "MANAGE_CUSTOMERS", "VIEW_VISITS", "CREATE_VISITS", "VIEW_PAYMENTS", "VIEW_NOTIFICATIONS"
        );
        rolePermissions.put("CLUSTER_HEAD", concat(headPermissions, List.of("MANAGE_USERS")));
        rolePermissions.put("CIRCLE_HEAD", headPermissions);
        rolePermissions.put("ZONE_HEAD", headPermissions);
        rolePermissions.put("AREA_HEAD", headPermissions);
        rolePermissions.put("STORE_HEAD", headPermissions);

        List<String> agentPermissions = Arrays.asList(
                "VIEW_CUSTOMERS", "COLLECT_PAYMENT", "VIEW_REPORTS", "USE_AI_AGENT",
                "VIEW_VISITS", "CREATE_VISITS", "VIEW_PAYMENTS", "VIEW_NOTIFICATIONS"
        );
        rolePermissions.put("AGENT", agentPermissions);

        // Ensure role permissions exist
        for (Map.Entry<String, List<String>> entry : rolePermissions.entrySet()) {
            String roleCode = entry.getKey();
            Role role = roleRepository.findByCode(roleCode).orElse(null);
            if (role == null) {
                continue;
            }
            for (String permissionCode : entry.getValue()) {
                Permission permission = permissionRepository.findByCode(permissionCode).orElse(null);
                if (permission == null) {
                    continue;
                }
                boolean exists = rolePermissionRepository.existsByRoleIdAndPermissionId(role.getId(), permission.getId());
                if (!exists) {
                    RolePermission rolePermission = new RolePermission();
                    rolePermission.setRole(role);
                    rolePermission.setPermission(permission);
                    rolePermission.setCreatedAt(LocalDateTime.now());
                    rolePermissionRepository.save(rolePermission);
                }
            }
        }
    }

    private List<String> concat(List<String> base, List<String> extra) {
        List<String> merged = new ArrayList<>(base);
        merged.addAll(extra);
        return merged;
    }

    private record PermissionSpec(String name, String description, String resource, String action) {}
}
