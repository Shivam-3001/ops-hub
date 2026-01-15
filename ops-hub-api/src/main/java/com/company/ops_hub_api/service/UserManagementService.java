package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Area;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.CreateUserRequestDTO;
import com.company.ops_hub_api.dto.UpdateUserStatusDTO;
import com.company.ops_hub_api.dto.UserManagementUserDTO;
import com.company.ops_hub_api.repository.AreaRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.company.ops_hub_api.util.EncryptionUtil;
import com.company.ops_hub_api.util.HierarchyUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class UserManagementService {

    private static final String USER_ENTITY = "USER";
    private static final String ROLE_ADMIN = "ADMIN";

    private final UserRepository userRepository;
    private final AreaRepository areaRepository;
    private final PasswordEncoder passwordEncoder;
    private final EncryptionUtil encryptionUtil;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public List<UserManagementUserDTO> listUsers() {
        User currentUser = getCurrentUser();
        String currentType = HierarchyUtil.normalizeUserType(currentUser);

        if (HierarchyUtil.ADMIN.equals(currentType)) {
            return userRepository.findAll().stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }

        if (HierarchyUtil.CLUSTER_HEAD.equals(currentType)) {
            Long clusterId = HierarchyUtil.getClusterId(currentUser);
            if (clusterId == null) {
                throw new IllegalStateException("Cluster not found for current user");
            }
            return userRepository.findByAreaZoneCircleClusterId(clusterId).stream()
                    .map(this::toDto)
                    .collect(Collectors.toList());
        }

        throw new AccessDeniedException("You do not have permission to view users");
    }

    @Transactional
    public UserManagementUserDTO createUser(CreateUserRequestDTO dto, HttpServletRequest request) {
        User currentUser = getCurrentUser();
        String targetUserType = normalizeUserType(dto.getUserType(), dto.getRole());
        String targetRole = normalizeRole(dto.getRole());

        validateCreationPermission(currentUser, targetUserType, targetRole);

        String employeeId = buildEmployeeId(dto.getEmployeeId());
        String username = dto.getUsername().trim();
        String email = dto.getEmail().trim();

        if (userRepository.existsByEmployeeId(employeeId)) {
            throw new IllegalArgumentException("Employee ID already exists");
        }
        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already exists");
        }
        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already exists");
        }

        Area area = resolveArea(dto.getAreaId(), currentUser);
        boolean active = dto.getActive() == null ? true : dto.getActive();

        User user = new User();
        user.setEmployeeId(employeeId);
        user.setUsername(username);
        user.setFullName(dto.getFullName().trim());
        user.setEmail(encryptionUtil.encrypt(email));
        user.setPhone(encryptionUtil.encrypt(dto.getPhone().trim()));
        user.setPasswordHash(passwordEncoder.encode(dto.getPassword()));
        user.setRole(targetRole);
        user.setUserType(targetUserType);
        user.setArea(area);
        user.setActive(active);
        user.setTwoFactorEnabled(false);

        User savedUser = userRepository.save(user);

        Map<String, Object> newValues = new HashMap<>();
        newValues.put("employeeId", savedUser.getEmployeeId());
        newValues.put("username", savedUser.getUsername());
        newValues.put("fullName", savedUser.getFullName());
        newValues.put("role", savedUser.getRole());
        newValues.put("userType", savedUser.getUserType());
        newValues.put("areaId", area != null ? area.getId() : null);
        newValues.put("active", savedUser.getActive());

        auditLogService.logAction("CREATE", USER_ENTITY, savedUser.getId(), null, newValues, request);
        auditLogService.logAction(active ? "ACTIVATE" : "DEACTIVATE", USER_ENTITY, savedUser.getId(),
                null, newValues, request);

        return toDto(savedUser);
    }

    @Transactional
    public UserManagementUserDTO updateUserStatus(Long userId, UpdateUserStatusDTO dto, HttpServletRequest request) {
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        User currentUser = getCurrentUser();
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));

        validateStatusChangePermission(currentUser, targetUser);

        Boolean newStatus = dto.getActive();
        if (newStatus == null) {
            throw new IllegalArgumentException("Active status is required");
        }

        Boolean oldStatus = targetUser.getActive();
        if (oldStatus != null && oldStatus.equals(newStatus)) {
            return toDto(targetUser);
        }

        targetUser.setActive(newStatus);
        User savedUser = userRepository.save(targetUser);

        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("active", oldStatus);
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("active", savedUser.getActive());
        newValues.put("employeeId", savedUser.getEmployeeId());
        newValues.put("username", savedUser.getUsername());

        auditLogService.logAction(newStatus ? "ACTIVATE" : "DEACTIVATE", USER_ENTITY, savedUser.getId(),
                oldValues, newValues, request);

        return toDto(savedUser);
    }

    private void validateCreationPermission(User currentUser, String targetUserType, String targetRole) {
        String currentType = HierarchyUtil.normalizeUserType(currentUser);
        if (HierarchyUtil.ADMIN.equals(currentType)) {
            if (ROLE_ADMIN.equalsIgnoreCase(targetRole) || HierarchyUtil.ADMIN.equalsIgnoreCase(targetUserType)) {
                throw new AccessDeniedException("Admin users must be created through system provisioning");
            }
            return;
        }

        if (HierarchyUtil.CLUSTER_HEAD.equals(currentType)) {
            int targetLevel = HierarchyUtil.hierarchyLevel(targetUserType);
            int clusterLevel = HierarchyUtil.hierarchyLevel(HierarchyUtil.CLUSTER_HEAD);
            if (targetLevel >= clusterLevel) {
                throw new AccessDeniedException("Cluster Heads can only create lower roles");
            }
            return;
        }

        throw new AccessDeniedException("You do not have permission to create users");
    }

    private void validateStatusChangePermission(User currentUser, User targetUser) {
        String currentType = HierarchyUtil.normalizeUserType(currentUser);
        String targetType = HierarchyUtil.normalizeUserType(targetUser);
        if (HierarchyUtil.ADMIN.equals(currentType)) {
            return;
        }

        if (HierarchyUtil.CLUSTER_HEAD.equals(currentType)) {
            Long clusterId = HierarchyUtil.getClusterId(currentUser);
            Long targetClusterId = HierarchyUtil.getClusterId(targetUser);
            if (clusterId == null || targetClusterId == null || !clusterId.equals(targetClusterId)) {
                throw new AccessDeniedException("You can only update users in your cluster");
            }
            if (HierarchyUtil.ADMIN.equals(targetType) || ROLE_ADMIN.equalsIgnoreCase(targetUser.getRole())) {
                throw new AccessDeniedException("You cannot change status for admin users");
            }
            if (!HierarchyUtil.isAbove(currentType, targetType)) {
                throw new AccessDeniedException("You can only update users below your level");
            }
            return;
        }

        throw new AccessDeniedException("You do not have permission to update user status");
    }

    private Area resolveArea(Long areaId, User currentUser) {
        if (areaId == null) {
            return currentUser.getArea();
        }
        Area area = areaRepository.findById(areaId)
                .orElseThrow(() -> new IllegalArgumentException("Area not found"));

        String currentType = HierarchyUtil.normalizeUserType(currentUser);
        if (HierarchyUtil.CLUSTER_HEAD.equals(currentType)) {
            Long currentClusterId = HierarchyUtil.getClusterId(currentUser);
            Long areaClusterId = HierarchyUtil.getClusterId(area);
            if (currentClusterId == null || areaClusterId == null || !currentClusterId.equals(areaClusterId)) {
                throw new AccessDeniedException("You can only assign users within your cluster");
            }
        }
        return area;
    }

    private String buildEmployeeId(String requestedEmployeeId) {
        if (requestedEmployeeId != null && !requestedEmployeeId.trim().isEmpty()) {
            return requestedEmployeeId.trim();
        }
        for (int i = 0; i < 5; i++) {
            String generated = "EMP" + UUID.randomUUID().toString().replace("-", "").substring(0, 8).toUpperCase();
            if (!userRepository.existsByEmployeeId(generated)) {
                return generated;
            }
        }
        throw new IllegalStateException("Unable to generate unique employee ID");
    }

    private String normalizeUserType(String userType, String role) {
        if (userType != null && !userType.trim().isEmpty()) {
            return HierarchyUtil.normalizeUserType(userType.trim());
        }
        String normalizedRole = normalizeRole(role);
        if (ROLE_ADMIN.equalsIgnoreCase(normalizedRole)) {
            return HierarchyUtil.ADMIN;
        }
        return HierarchyUtil.normalizeUserType(normalizedRole);
    }

    private String normalizeRole(String role) {
        if (role == null || role.trim().isEmpty()) {
            throw new IllegalArgumentException("Role is required");
        }
        return role.trim().toUpperCase();
    }


    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private UserManagementUserDTO toDto(User user) {
        if (user == null) {
            return null;
        }
        String email = safeDecrypt(user.getEmail());
        String phone = safeDecrypt(user.getPhone());
        return UserManagementUserDTO.builder()
                .id(user.getId())
                .employeeId(user.getEmployeeId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .email(email)
                .phone(phone)
                .role(user.getRole())
                .userType(user.getUserType())
                .active(user.getActive())
                .areaName(user.getArea() != null ? user.getArea().getName() : null)
                .zoneName(user.getArea() != null && user.getArea().getZone() != null
                        ? user.getArea().getZone().getName() : null)
                .circleName(user.getArea() != null && user.getArea().getZone() != null
                        && user.getArea().getZone().getCircle() != null
                        ? user.getArea().getZone().getCircle().getName() : null)
                .clusterName(user.getArea() != null && user.getArea().getZone() != null
                        && user.getArea().getZone().getCircle() != null
                        && user.getArea().getZone().getCircle().getCluster() != null
                        ? user.getArea().getZone().getCircle().getCluster().getName() : null)
                .createdAt(user.getCreatedAt())
                .build();
    }

    private String safeDecrypt(String value) {
        if (value == null) {
            return null;
        }
        try {
            return encryptionUtil.decrypt(value);
        } catch (Exception e) {
            return value;
        }
    }
}
