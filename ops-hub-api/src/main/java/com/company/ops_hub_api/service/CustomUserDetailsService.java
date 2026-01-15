package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Permission;
import com.company.ops_hub_api.domain.Role;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.repository.PermissionRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.repository.UserRoleRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.company.ops_hub_api.util.HierarchyUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final PermissionRepository permissionRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByEmployeeId(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with employeeId: " + username));

        return buildUserPrincipal(user);
    }

    @Transactional(readOnly = true)
    public UserDetails loadUserById(Long userId) {
        if (userId == null) {
            throw new UsernameNotFoundException("User ID cannot be null");
        }
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException("User not found with id: " + userId));

        return buildUserPrincipal(user);
    }

    private UserPrincipal buildUserPrincipal(User user) {
        // Get user roles
        List<Role> roles = userRoleRepository.findRolesByUserId(user.getId());
        Set<String> roleCodes = roles.stream()
                .map(Role::getCode)
                .collect(Collectors.toSet());

        // Get all permissions from user's roles
        Set<String> permissions = new HashSet<>();
        for (Role role : roles) {
            List<Permission> rolePermissions = permissionRepository.findByRoleCode(role.getCode());
            permissions.addAll(rolePermissions.stream()
                    .filter(Permission::getActive)
                    .map(Permission::getCode)
                    .collect(Collectors.toSet()));
        }

        String userType = HierarchyUtil.normalizeUserType(user);
        if (HierarchyUtil.CLUSTER_HEAD.equals(userType)) {
            permissions.add("MANAGE_USERS");
        }

        return new UserPrincipal(user, permissions, roleCodes);
    }
}
