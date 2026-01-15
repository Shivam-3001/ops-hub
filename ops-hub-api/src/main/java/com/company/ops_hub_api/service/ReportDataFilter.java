package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.repository.CustomerAllocationRepository;
import com.company.ops_hub_api.repository.CustomerRepository;
import com.company.ops_hub_api.util.HierarchyUtil;
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
    private final CustomerRepository customerRepository;

    /**
     * Get customer IDs that the user has access to
     * Based on role and customer allocation
     */
    public Set<Long> getAccessibleCustomerIds(User user) {
        String userType = HierarchyUtil.normalizeUserType(user);

        if (HierarchyUtil.ADMIN.equals(userType)) {
            return null;
        }

        Long clusterId = HierarchyUtil.getClusterId(user);
        Long circleId = HierarchyUtil.getCircleId(user);
        Long zoneId = HierarchyUtil.getZoneId(user);
        Long areaId = HierarchyUtil.getAreaId(user);

        if (HierarchyUtil.CLUSTER_HEAD.equals(userType) && clusterId != null) {
            return customerRepository.findByAreaZoneCircleClusterId(clusterId)
                    .stream()
                    .map(customer -> customer.getId())
                    .collect(Collectors.toSet());
        }
        if (HierarchyUtil.CIRCLE_HEAD.equals(userType) && circleId != null) {
            return customerRepository.findByAreaZoneCircleId(circleId)
                    .stream()
                    .map(customer -> customer.getId())
                    .collect(Collectors.toSet());
        }
        if (HierarchyUtil.ZONE_HEAD.equals(userType) && zoneId != null) {
            return customerRepository.findByAreaZoneId(zoneId)
                    .stream()
                    .map(customer -> customer.getId())
                    .collect(Collectors.toSet());
        }
        if (HierarchyUtil.AREA_HEAD.equals(userType) && areaId != null) {
            return customerRepository.findByAreaId(areaId)
                    .stream()
                    .map(customer -> customer.getId())
                    .collect(Collectors.toSet());
        }

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
