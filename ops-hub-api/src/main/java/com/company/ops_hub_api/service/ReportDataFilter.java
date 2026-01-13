package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.repository.CustomerAllocationRepository;
import com.company.ops_hub_api.repository.UserRoleRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Report Data Filter
 * Filters report data based on user role and customer allocation
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class ReportDataFilter {

    private final CustomerAllocationRepository allocationRepository;
    private final UserRoleRepository userRoleRepository;

    /**
     * Get customer IDs that the user has access to
     * Based on role and customer allocation
     */
    public Set<Long> getAccessibleCustomerIds(User user) {
        List<String> userRoles = userRoleRepository.findRoleCodesByUserId(user.getId());
        
        // Admin and high-level managers can see all customers
        if (userRoles.contains("ADMIN") || 
            userRoles.contains("CLUSTER_LEAD") || 
            user.getUserType().contains("CLUSTER_LEAD")) {
            return null; // null means no filter (all customers)
        }
        
        // Circle leads can see customers in their circle
        if (userRoles.contains("CIRCLE_LEAD") || 
            user.getUserType().contains("CIRCLE_LEAD")) {
            // Get customers in the same circle
            if (user.getArea() != null && 
                user.getArea().getZone() != null && 
                user.getArea().getZone().getCircle() != null) {
                // For now, return null (all customers in circle)
                // In production, query customers by circle
                return null;
            }
        }
        
        // Zone leads can see customers in their zone
        if (userRoles.contains("ZONE_LEAD") || 
            user.getUserType().contains("ZONE_LEAD")) {
            // Get customers in the same zone
            if (user.getArea() != null && 
                user.getArea().getZone() != null) {
                // For now, return null (all customers in zone)
                // In production, query customers by zone
                return null;
            }
        }
        
        // Area leads and agents can only see allocated customers
        List<com.company.ops_hub_api.domain.CustomerAllocation> allocations = 
                allocationRepository.findActiveAllocationsByUserId(user.getId());
        
        return allocations.stream()
                .map(allocation -> allocation.getCustomer().getId())
                .collect(Collectors.toSet());
    }

    /**
     * Check if user can access a specific customer
     */
    public boolean canAccessCustomer(User user, Long customerId) {
        Set<Long> accessibleCustomerIds = getAccessibleCustomerIds(user);
        
        // null means no filter (all customers accessible)
        if (accessibleCustomerIds == null) {
            return true;
        }
        
        return accessibleCustomerIds.contains(customerId);
    }

    /**
     * Apply data filter to report data
     * Filters out records that user doesn't have access to
     */
    public List<java.util.Map<String, Object>> filterReportData(
            List<java.util.Map<String, Object>> data, 
            User user, 
            String customerIdField) {
        
        Set<Long> accessibleCustomerIds = getAccessibleCustomerIds(user);
        
        // If no filter needed (admin/high-level), return all data
        if (accessibleCustomerIds == null) {
            return data;
        }
        
        // Filter data based on accessible customer IDs
        return data.stream()
                .filter(record -> {
                    Object customerIdObj = record.get(customerIdField);
                    if (customerIdObj == null) {
                        return false;
                    }
                    Long customerId = null;
                    if (customerIdObj instanceof Long) {
                        customerId = (Long) customerIdObj;
                    } else if (customerIdObj instanceof Number) {
                        customerId = ((Number) customerIdObj).longValue();
                    } else if (customerIdObj instanceof String) {
                        try {
                            customerId = Long.parseLong((String) customerIdObj);
                        } catch (NumberFormatException e) {
                            return false;
                        }
                    }
                    return customerId != null && accessibleCustomerIds.contains(customerId);
                })
                .collect(java.util.stream.Collectors.toList());
    }

    /**
     * Build SQL WHERE clause for data-level access control
     */
    public String buildAccessControlWhereClause(User user, String customerTableAlias) {
        Set<Long> accessibleCustomerIds = getAccessibleCustomerIds(user);
        
        // If no filter needed (admin/high-level), return empty string
        if (accessibleCustomerIds == null) {
            return "";
        }
        
        // Build WHERE clause with customer IDs
        if (accessibleCustomerIds.isEmpty()) {
            return " AND 1=0"; // No access
        }
        
        String customerIds = accessibleCustomerIds.stream()
                .map(String::valueOf)
                .collect(Collectors.joining(","));
        
        return String.format(" AND %s.id IN (%s)", customerTableAlias, customerIds);
    }
}
