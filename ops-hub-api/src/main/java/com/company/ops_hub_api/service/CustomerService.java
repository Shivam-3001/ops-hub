package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Customer;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.repository.CustomerRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.company.ops_hub_api.util.HierarchyUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerService {

    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;

    /**
     * Get all customers - filtered by permissions
     * Users with VIEW_CUSTOMERS can see all customers
     * Users without VIEW_CUSTOMERS can only see their allocated customers
     */
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        User currentUser = getCurrentUser();
        String userType = HierarchyUtil.normalizeUserType(currentUser);

        if (HierarchyUtil.ADMIN.equals(userType)) {
            return customerRepository.findAll();
        }

        Long clusterId = HierarchyUtil.getClusterId(currentUser);
        Long circleId = HierarchyUtil.getCircleId(currentUser);
        Long zoneId = HierarchyUtil.getZoneId(currentUser);

        if (HierarchyUtil.CLUSTER_HEAD.equals(userType) && clusterId != null) {
            return customerRepository.findByAreaZoneCircleClusterId(clusterId);
        }
        if (HierarchyUtil.CIRCLE_HEAD.equals(userType) && circleId != null) {
            return customerRepository.findByAreaZoneCircleId(circleId);
        }
        if (HierarchyUtil.ZONE_HEAD.equals(userType) && zoneId != null) {
            return customerRepository.findByAreaZoneId(zoneId);
        }

        return customerRepository.findCustomersByAssignedUserId(currentUser.getId());
    }

    /**
     * Get a specific customer by ID
     * Users can only view customers they have access to
     */
    @Transactional(readOnly = true)
    public Customer getCustomerById(Long customerId) {
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        User currentUser = getCurrentUser();
        if (!canAccessCustomer(currentUser, customer)) {
            throw new org.springframework.security.access.AccessDeniedException(
                    "You do not have access to this customer");
        }

        return customer;
    }

    /**
     * Get customers by area
     * Filtered by permissions
     */
    @Transactional(readOnly = true)
    public List<Customer> getCustomersByArea(Long areaId) {
        if (areaId == null) {
            throw new IllegalArgumentException("Area ID cannot be null");
        }

        User currentUser = getCurrentUser();
        String userType = HierarchyUtil.normalizeUserType(currentUser);

        if (HierarchyUtil.ADMIN.equals(userType)) {
            return customerRepository.findByAreaId(areaId);
        }

        Long circleId = HierarchyUtil.getCircleId(currentUser);
        Long zoneId = HierarchyUtil.getZoneId(currentUser);
        Long currentAreaId = HierarchyUtil.getAreaId(currentUser);

        if (HierarchyUtil.CIRCLE_HEAD.equals(userType) && circleId != null) {
            return customerRepository.findByAreaId(areaId).stream()
                    .filter(customer -> circleId.equals(HierarchyUtil.getCircleId(customer.getArea())))
                    .toList();
        }
        if (HierarchyUtil.ZONE_HEAD.equals(userType) && zoneId != null) {
            return customerRepository.findByAreaId(areaId).stream()
                    .filter(customer -> zoneId.equals(HierarchyUtil.getZoneId(customer.getArea())))
                    .toList();
        }
        if (HierarchyUtil.AREA_HEAD.equals(userType) || HierarchyUtil.STORE_HEAD.equals(userType)
                || HierarchyUtil.AGENT.equals(userType)) {
            if (currentAreaId != null && currentAreaId.equals(areaId)) {
                return customerRepository.findCustomersByAssignedUserId(currentUser.getId()).stream()
                        .filter(customer -> customer.getArea() != null &&
                                areaId.equals(customer.getArea().getId()))
                        .toList();
            }
            return List.of();
        }

        return List.of();
    }

    private boolean canAccessCustomer(User currentUser, Customer customer) {
        if (currentUser == null || customer == null) {
            return false;
        }
        String userType = HierarchyUtil.normalizeUserType(currentUser);
        if (HierarchyUtil.ADMIN.equals(userType)) {
            return true;
        }
        if (HierarchyUtil.CLUSTER_HEAD.equals(userType)) {
            Long userClusterId = HierarchyUtil.getClusterId(currentUser);
            Long customerClusterId = HierarchyUtil.getClusterId(customer.getArea());
            return userClusterId != null && userClusterId.equals(customerClusterId);
        }
        if (HierarchyUtil.CIRCLE_HEAD.equals(userType)) {
            Long userCircleId = HierarchyUtil.getCircleId(currentUser);
            Long customerCircleId = HierarchyUtil.getCircleId(customer.getArea());
            return userCircleId != null && userCircleId.equals(customerCircleId);
        }
        if (HierarchyUtil.ZONE_HEAD.equals(userType)) {
            Long userZoneId = HierarchyUtil.getZoneId(currentUser);
            Long customerZoneId = HierarchyUtil.getZoneId(customer.getArea());
            return userZoneId != null && userZoneId.equals(customerZoneId);
        }

        return customerRepository.findCustomersByAssignedUserId(currentUser.getId())
                .stream()
                .anyMatch(allocated -> allocated.getId().equals(customer.getId()));
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

}
