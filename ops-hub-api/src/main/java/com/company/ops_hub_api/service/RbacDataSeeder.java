package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Permission;
import com.company.ops_hub_api.domain.Role;
import com.company.ops_hub_api.domain.RolePermission;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.repository.PermissionRepository;
import com.company.ops_hub_api.repository.RolePermissionRepository;
import com.company.ops_hub_api.repository.RoleRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class RbacDataSeeder {

    private final RoleRepository roleRepository;
    private final PermissionRepository permissionRepository;
    private final RolePermissionRepository rolePermissionRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;

    @Bean
    @Transactional
    public CommandLineRunner seedRbacData() {
        return args -> {
            if (roleRepository.count() == 0) {
                log.info("Seeding RBAC data...");

                // 1. Create Permissions
                Permission viewCustomers = createPermission("VIEW_CUSTOMERS", "View Customers", 
                        "View customer information", "CUSTOMER", "VIEW");
                Permission assignCustomers = createPermission("ASSIGN_CUSTOMERS", "Assign Customers", 
                        "Assign customers to users", "CUSTOMER", "ASSIGN");
                Permission collectPayment = createPermission("COLLECT_PAYMENT", "Collect Payment", 
                        "Collect payments from customers", "PAYMENT", "CREATE");
                Permission approveProfile = createPermission("APPROVE_PROFILE", "Approve Profile", 
                        "Approve user profile updates", "PROFILE", "APPROVE");
                Permission viewReports = createPermission("VIEW_REPORTS", "View Reports", 
                        "View reports and analytics", "REPORT", "VIEW");
                Permission exportReports = createPermission("EXPORT_REPORTS", "Export Reports", 
                        "Export reports in various formats", "REPORT", "EXPORT");
                Permission useAiAgent = createPermission("USE_AI_AGENT", "Use AI Agent", 
                        "Access and use AI assistant features", "AI", "USE");
                Permission viewPermissions = createPermission("VIEW_PERMISSIONS", "View Permissions", 
                        "View roles and permissions", "PERMISSION", "VIEW");
                Permission viewRoles = createPermission("VIEW_ROLES", "View Roles", 
                        "View role definitions", "ROLE", "VIEW");
                Permission manageUsers = createPermission("MANAGE_USERS", "Manage Users", 
                        "Create, update, and delete users", "USER", "MANAGE");
                Permission manageCustomers = createPermission("MANAGE_CUSTOMERS", "Manage Customers", 
                        "Create, update, and delete customers", "CUSTOMER", "MANAGE");
                Permission viewVisits = createPermission("VIEW_VISITS", "View Visits", 
                        "View customer visit records", "VISIT", "VIEW");
                Permission createVisits = createPermission("CREATE_VISITS", "Create Visits", 
                        "Create customer visit records", "VISIT", "CREATE");
                Permission viewPayments = createPermission("VIEW_PAYMENTS", "View Payments", 
                        "View payment transactions", "PAYMENT", "VIEW");
                Permission manageSettings = createPermission("MANAGE_SETTINGS", "Manage Settings", 
                        "Manage application settings", "SETTING", "MANAGE");

                List<Permission> allPermissions = Arrays.asList(
                        viewCustomers, assignCustomers, collectPayment, approveProfile,
                        viewReports, exportReports, useAiAgent, viewPermissions, viewRoles,
                        manageUsers, manageCustomers, viewVisits, createVisits, viewPayments, manageSettings
                );

                if (allPermissions != null && !allPermissions.isEmpty()) {
                    permissionRepository.saveAll(allPermissions);
                    log.info("Created {} permissions", allPermissions.size());
                }

                // 2. Create Roles
                Role adminRole = createRole("ADMIN", "Administrator", 
                        "Full system access with all permissions");
                Role leadRole = createRole("LEAD", "Lead", 
                        "Team lead with management permissions");
                Role agentRole = createRole("AGENT", "Agent", 
                        "Field agent with basic operational permissions");

                List<Role> allRoles = Arrays.asList(adminRole, leadRole, agentRole);
                if (allRoles != null && !allRoles.isEmpty()) {
                    roleRepository.saveAll(allRoles);
                    log.info("Created {} roles", allRoles.size());
                }

                // 3. Map Permissions to Roles
                // ADMIN gets all permissions
                if (allPermissions != null) {
                    for (Permission permission : allPermissions) {
                        createRolePermission(adminRole, permission);
                    }
                }

                // LEAD gets most permissions except user management
                List<Permission> leadPermissions = Arrays.asList(
                        viewCustomers, assignCustomers, collectPayment, approveProfile,
                        viewReports, exportReports, useAiAgent, viewPermissions, viewRoles,
                        manageCustomers, viewVisits, createVisits, viewPayments
                );
                for (Permission permission : leadPermissions) {
                    createRolePermission(leadRole, permission);
                }

                // AGENT gets basic operational permissions
                List<Permission> agentPermissions = Arrays.asList(
                        viewCustomers, collectPayment, viewReports, useAiAgent,
                        viewVisits, createVisits, viewPayments
                );
                for (Permission permission : agentPermissions) {
                    createRolePermission(agentRole, permission);
                }

                log.info("Mapped permissions to roles");

                // 4. Assign roles to existing users based on their current role field
                assignRolesToUsers();

                log.info("RBAC data seeding complete.");
            } else {
                log.info("RBAC data already exists, skipping seeding.");
            }
        };
    }

    private Permission createPermission(String code, String name, String description, String resource, String action) {
        Permission permission = new Permission();
        permission.setCode(code);
        permission.setName(name);
        permission.setDescription(description);
        permission.setResource(resource);
        permission.setAction(action);
        permission.setActive(true);
        permission.setCreatedAt(LocalDateTime.now());
        permission.setUpdatedAt(LocalDateTime.now());
        return permission;
    }

    private Role createRole(String code, String name, String description) {
        Role role = new Role();
        role.setCode(code);
        role.setName(name);
        role.setDescription(description);
        role.setActive(true);
        role.setCreatedAt(LocalDateTime.now());
        role.setUpdatedAt(LocalDateTime.now());
        return role;
    }

    private void createRolePermission(Role role, Permission permission) {
        RolePermission rolePermission = new RolePermission();
        rolePermission.setRole(role);
        rolePermission.setPermission(permission);
        rolePermission.setCreatedAt(LocalDateTime.now());
        rolePermissionRepository.save(rolePermission);
    }

    private void assignRolesToUsers() {
        List<User> users = userRepository.findAll();
        for (User user : users) {
            String userRole = user.getRole();
            Role role = null;

            if ("ADMIN".equalsIgnoreCase(userRole)) {
                role = roleRepository.findByCode("ADMIN").orElse(null);
            } else if ("MANAGER".equalsIgnoreCase(userRole) || user.getUserType().contains("LEAD")) {
                role = roleRepository.findByCode("LEAD").orElse(null);
            } else {
                role = roleRepository.findByCode("AGENT").orElse(null);
            }

            if (role != null && !userRoleRepository.existsByUserIdAndRoleId(user.getId(), role.getId())) {
                com.company.ops_hub_api.domain.UserRole userRoleEntity = new com.company.ops_hub_api.domain.UserRole();
                userRoleEntity.setUser(user);
                userRoleEntity.setRole(role);
                userRoleEntity.setCreatedAt(LocalDateTime.now());
                userRoleRepository.save(userRoleEntity);
                log.info("Assigned role {} to user {}", role.getCode(), user.getEmployeeId());
            }
        }
    }
}
