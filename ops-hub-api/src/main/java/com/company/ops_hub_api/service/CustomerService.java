package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Customer;
import com.company.ops_hub_api.repository.CustomerRepository;
import com.company.ops_hub_api.security.UserPrincipal;
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

    /**
     * Get all customers - filtered by permissions
     * Users with VIEW_CUSTOMERS can see all customers
     * Users without VIEW_CUSTOMERS can only see their allocated customers
     */
    @Transactional(readOnly = true)
    public List<Customer> getAllCustomers() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // If user has VIEW_CUSTOMERS permission, return all customers
        if (userPrincipal.hasPermission("VIEW_CUSTOMERS")) {
            return customerRepository.findAll();
        }
        
        // Otherwise, return only allocated customers
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return customerRepository.findCustomersByAssignedUserId(userId);
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
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // If user has VIEW_CUSTOMERS permission, allow access
        if (userPrincipal.hasPermission("VIEW_CUSTOMERS")) {
            return customer;
        }
        
        // Otherwise, check if customer is allocated to user
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        
        boolean isAllocated = customerRepository.findCustomersByAssignedUserId(userId)
                .stream()
                .anyMatch(c -> c.getId().equals(customerId));
        
        if (!isAllocated) {
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
        List<Customer> customers = customerRepository.findByAreaId(areaId);
        
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new org.springframework.security.access.AccessDeniedException("User not authenticated");
        }
        
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        
        // If user has VIEW_CUSTOMERS permission, return all customers in area
        if (userPrincipal.hasPermission("VIEW_CUSTOMERS")) {
            return customers;
        }
        
        // Otherwise, filter to only allocated customers
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        
        List<Customer> allocatedCustomers = customerRepository.findCustomersByAssignedUserId(userId);
        return customers.stream()
                .filter(c -> allocatedCustomers.stream()
                        .anyMatch(ac -> ac.getId().equals(c.getId())))
                .toList();
    }
}
