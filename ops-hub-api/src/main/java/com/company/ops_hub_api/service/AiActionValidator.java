package com.company.ops_hub_api.service;

import com.company.ops_hub_api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * AI Action Validator
 * Validates AI actions against user permissions and determines if confirmation is required
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class AiActionValidator {

    // Map of action types to required permissions
    private static final Map<String, String> ACTION_PERMISSION_MAP = new HashMap<>();
    
    // Set of restricted actions that always require confirmation
    private static final Set<String> RESTRICTED_ACTIONS = Set.of(
        "DELETE_CUSTOMER",
        "DELETE_USER",
        "REASSIGN_CUSTOMER",
        "APPROVE_PROFILE",
        "REJECT_PROFILE",
        "EXPORT_REPORTS",
        "MANAGE_USERS",
        "MANAGE_SETTINGS"
    );

    static {
        // Map action types to required permissions
        ACTION_PERMISSION_MAP.put("VIEW_CUSTOMERS", "VIEW_CUSTOMERS");
        ACTION_PERMISSION_MAP.put("ASSIGN_CUSTOMERS", "ASSIGN_CUSTOMERS");
        ACTION_PERMISSION_MAP.put("MANAGE_CUSTOMERS", "MANAGE_CUSTOMERS");
        ACTION_PERMISSION_MAP.put("DELETE_CUSTOMER", "MANAGE_CUSTOMERS");
        ACTION_PERMISSION_MAP.put("REASSIGN_CUSTOMER", "ASSIGN_CUSTOMERS");
        ACTION_PERMISSION_MAP.put("COLLECT_PAYMENT", "COLLECT_PAYMENT");
        ACTION_PERMISSION_MAP.put("VIEW_PAYMENTS", "VIEW_PAYMENTS");
        ACTION_PERMISSION_MAP.put("APPROVE_PROFILE", "APPROVE_PROFILE");
        ACTION_PERMISSION_MAP.put("REJECT_PROFILE", "APPROVE_PROFILE");
        ACTION_PERMISSION_MAP.put("VIEW_REPORTS", "VIEW_REPORTS");
        ACTION_PERMISSION_MAP.put("EXPORT_REPORTS", "EXPORT_REPORTS");
        ACTION_PERMISSION_MAP.put("MANAGE_USERS", "MANAGE_USERS");
        ACTION_PERMISSION_MAP.put("MANAGE_SETTINGS", "MANAGE_SETTINGS");
        ACTION_PERMISSION_MAP.put("DATA_QUERY", "VIEW_REPORTS");
        ACTION_PERMISSION_MAP.put("REPORT_GENERATION", "VIEW_REPORTS");
    }

    /**
     * Validate if user has permission to perform the action
     */
    public void validateActionPermission(String actionType, String actionName) {
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal == null) {
            throw new AccessDeniedException("User not authenticated");
        }

        // Check if action requires a specific permission
        String requiredPermission = ACTION_PERMISSION_MAP.get(actionType);
        if (requiredPermission != null && !userPrincipal.hasPermission(requiredPermission)) {
            log.warn("AI action denied: User {} attempted action {} without permission {}", 
                    userPrincipal.getEmployeeId(), actionName, requiredPermission);
            throw new AccessDeniedException(
                    String.format("Insufficient permission to perform action: %s. Required: %s", 
                            actionName, requiredPermission));
        }

        // Additional check for action name
        if (actionName != null) {
            String actionNamePermission = ACTION_PERMISSION_MAP.get(actionName);
            if (actionNamePermission != null && !userPrincipal.hasPermission(actionNamePermission)) {
                log.warn("AI action denied: User {} attempted action {} without permission {}", 
                        userPrincipal.getEmployeeId(), actionName, actionNamePermission);
                throw new AccessDeniedException(
                        String.format("Insufficient permission to perform action: %s. Required: %s", 
                                actionName, actionNamePermission));
            }
        }
    }

    /**
     * Check if action requires confirmation
     */
    public boolean requiresConfirmation(String actionType, String actionName) {
        // Check if action is in restricted list
        if (RESTRICTED_ACTIONS.contains(actionType) || RESTRICTED_ACTIONS.contains(actionName)) {
            return true;
        }

        // Check if action requires a permission that the user might not have
        UserPrincipal userPrincipal = getCurrentUserPrincipal();
        if (userPrincipal != null) {
            String requiredPermission = ACTION_PERMISSION_MAP.get(actionType);
            if (requiredPermission != null && !userPrincipal.hasPermission(requiredPermission)) {
                return true;
            }
        }

        return false;
    }

    /**
     * Get required permission for an action
     */
    public String getRequiredPermission(String actionType, String actionName) {
        String permission = ACTION_PERMISSION_MAP.get(actionType);
        if (permission == null && actionName != null) {
            permission = ACTION_PERMISSION_MAP.get(actionName);
        }
        return permission;
    }

    /**
     * Generate confirmation message for an action
     */
    public String generateConfirmationMessage(String actionType, String actionName) {
        String requiredPermission = getRequiredPermission(actionType, actionName);
        if (requiredPermission != null) {
            return String.format("This action requires %s permission. Are you sure you want to proceed?", 
                    requiredPermission);
        }
        return String.format("Are you sure you want to perform action: %s?", actionName);
    }

    /**
     * Get current user principal from security context
     */
    private UserPrincipal getCurrentUserPrincipal() {
        try {
            Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
            if (authentication != null && authentication.getPrincipal() instanceof UserPrincipal) {
                return (UserPrincipal) authentication.getPrincipal();
            }
        } catch (Exception e) {
            log.debug("Could not get user principal from security context", e);
        }
        return null;
    }
}
