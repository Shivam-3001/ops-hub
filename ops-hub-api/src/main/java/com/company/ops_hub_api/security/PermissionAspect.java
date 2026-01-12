package com.company.ops_hub_api.security;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.aspectj.lang.JoinPoint;
import org.aspectj.lang.annotation.Aspect;
import org.aspectj.lang.annotation.Before;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Aspect
@Component
@RequiredArgsConstructor
@Slf4j
public class PermissionAspect {

    @Before("@annotation(requiresPermission)")
    public void checkPermission(JoinPoint joinPoint, RequiresPermission requiresPermission) {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("Authentication required");
        }

        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        String[] requiredPermissions = requiresPermission.value();
        boolean requireAll = requiresPermission.requireAll();

        if (requireAll) {
            // User must have ALL permissions
            for (String permission : requiredPermissions) {
                if (!userPrincipal.hasPermission(permission)) {
                    log.warn("User {} does not have required permission: {}", 
                            userPrincipal.getEmployeeId(), permission);
                    throw new AccessDeniedException("Access denied. Required permission: " + permission);
                }
            }
        } else {
            // User must have ANY permission
            boolean hasAnyPermission = userPrincipal.hasAnyPermission(requiredPermissions);
            if (!hasAnyPermission) {
                log.warn("User {} does not have any of the required permissions: {}", 
                        userPrincipal.getEmployeeId(), String.join(", ", requiredPermissions));
                throw new AccessDeniedException("Access denied. Required permissions: " + 
                        String.join(", ", requiredPermissions));
            }
        }
    }
}
