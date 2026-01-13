package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Customer;
import com.company.ops_hub_api.domain.CustomerAllocation;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.AllocateCustomerDTO;
import com.company.ops_hub_api.dto.ReassignCustomerDTO;
import com.company.ops_hub_api.repository.CustomerAllocationRepository;
import com.company.ops_hub_api.repository.CustomerRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.repository.UserRoleRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerAllocationService {

    private final CustomerAllocationRepository allocationRepository;
    private final CustomerRepository customerRepository;
    private final UserRepository userRepository;
    private final UserRoleRepository userRoleRepository;
    private final AuditLogService auditLogService;
    private final EmailNotificationService emailNotificationService;

    /**
     * Allocate a customer to a user
     */
    @Transactional
    public CustomerAllocation allocateCustomer(AllocateCustomerDTO dto, HttpServletRequest httpRequest) {
        // Check permission
        checkAllocationPermission();
        
        // Get customer
        Long customerId = dto.getCustomerId();
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        // Get user to assign
        Long userId = dto.getUserId();
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        User assignee = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
        
        // Validate user has the required role
        Long assigneeId = assignee.getId();
        if (assigneeId == null) {
            throw new IllegalStateException("Assignee user ID cannot be null");
        }
        validateUserRole(assigneeId, dto.getRoleCode());
        
        // Check for existing active allocation
        allocationRepository.findActiveAllocationByCustomerAndUser(customerId, userId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Customer is already allocated to this user");
                });
        
        // Get current user (allocator)
        User allocator = getCurrentUser();
        
        // Create allocation
        CustomerAllocation allocation = new CustomerAllocation();
        allocation.setCustomer(customer);
        allocation.setUser(assignee);
        allocation.setRoleCode(dto.getRoleCode());
        allocation.setAllocationType(dto.getAllocationType());
        allocation.setStatus("ACTIVE");
        allocation.setAllocatedBy(allocator);
        allocation.setAllocatedAt(LocalDateTime.now());
        allocation.setNotes(dto.getNotes());
        
        CustomerAllocation savedAllocation = allocationRepository.save(allocation);
        
        // Log audit
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("allocationId", savedAllocation.getId());
        newValues.put("customerId", customer.getId());
        newValues.put("customerCode", customer.getCustomerCode());
        newValues.put("userId", assignee.getId());
        newValues.put("userEmployeeId", assignee.getEmployeeId());
        newValues.put("roleCode", dto.getRoleCode());
        newValues.put("allocationType", dto.getAllocationType());
        newValues.put("status", "ACTIVE");
        
        Long allocationId = savedAllocation.getId();
        if (allocationId != null) {
            auditLogService.logAction("CREATE", "CUSTOMER_ALLOCATION", allocationId, 
                    null, newValues, httpRequest);
        }
        
        // Send email notification
        try {
            String userName = assignee.getFullName() != null ? assignee.getFullName() : assignee.getUsername();
            String customerName = buildCustomerName(customer);
            
            emailNotificationService.sendCustomerAllocatedNotification(
                    assignee.getEmail(),
                    userName,
                    customerName,
                    customer.getCustomerCode(),
                    dto.getRoleCode()
            );
        } catch (Exception e) {
            log.error("Error sending customer allocation email notification", e);
        }
        
        log.info("Customer {} allocated to user {} by {}", 
                customer.getCustomerCode(), assignee.getEmployeeId(), allocator.getEmployeeId());
        
        return savedAllocation;
    }

    /**
     * Reassign a customer from one user to another
     */
    @Transactional
    public CustomerAllocation reassignCustomer(ReassignCustomerDTO dto, HttpServletRequest httpRequest) {
        // Check permission
        checkAllocationPermission();
        
        // Get customer
        Long customerId = dto.getCustomerId();
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        // Get new user
        Long newUserId = dto.getNewUserId();
        if (newUserId == null) {
            throw new IllegalArgumentException("New user ID cannot be null");
        }
        User newAssignee = userRepository.findById(newUserId)
                .orElseThrow(() -> new IllegalArgumentException("New user not found"));
        
        // Validate new user has the required role
        Long newAssigneeId = newAssignee.getId();
        if (newAssigneeId == null) {
            throw new IllegalStateException("New assignee user ID cannot be null");
        }
        validateUserRole(newAssigneeId, dto.getRoleCode());
        
        // Get current user (reassigner)
        User reassigner = getCurrentUser();
        
        // Deactivate existing active allocations
        List<CustomerAllocation> activeAllocations = allocationRepository
                .findActiveAllocationsByCustomerId(customerId);
        
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("customerId", customer.getId());
        oldValues.put("customerCode", customer.getCustomerCode());
        oldValues.put("activeAllocationsCount", activeAllocations.size());
        
        for (CustomerAllocation existing : activeAllocations) {
            existing.setStatus("INACTIVE");
            existing.setDeallocatedAt(LocalDateTime.now());
            existing.setDeallocationReason("Reassigned to user " + newAssignee.getEmployeeId() + ": " + dto.getReason());
            allocationRepository.save(existing);
        }
        
        // Check if customer is already allocated to new user
        allocationRepository.findActiveAllocationByCustomerAndUser(customerId, newUserId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("Customer is already allocated to the new user");
                });
        
        // Create new allocation
        CustomerAllocation newAllocation = new CustomerAllocation();
        newAllocation.setCustomer(customer);
        newAllocation.setUser(newAssignee);
        newAllocation.setRoleCode(dto.getRoleCode());
        newAllocation.setAllocationType(dto.getAllocationType());
        newAllocation.setStatus("ACTIVE");
        newAllocation.setAllocatedBy(reassigner);
        newAllocation.setAllocatedAt(LocalDateTime.now());
        newAllocation.setNotes(dto.getNotes());
        
        CustomerAllocation savedAllocation = allocationRepository.save(newAllocation);
        
        // Log audit
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("allocationId", savedAllocation.getId());
        newValues.put("customerId", customer.getId());
        newValues.put("customerCode", customer.getCustomerCode());
        newValues.put("newUserId", newAssignee.getId());
        newValues.put("newUserEmployeeId", newAssignee.getEmployeeId());
        newValues.put("roleCode", dto.getRoleCode());
        newValues.put("allocationType", dto.getAllocationType());
        newValues.put("status", "ACTIVE");
        newValues.put("reason", dto.getReason());
        newValues.put("deactivatedAllocations", activeAllocations.size());
        
        Long allocationId = savedAllocation.getId();
        if (allocationId != null) {
            auditLogService.logAction("REASSIGN", "CUSTOMER_ALLOCATION", allocationId, 
                    oldValues, newValues, httpRequest);
        }
        
        log.info("Customer {} reassigned from {} users to user {} by {}", 
                customer.getCustomerCode(), activeAllocations.size(), 
                newAssignee.getEmployeeId(), reassigner.getEmployeeId());
        
        return savedAllocation;
    }

    /**
     * Deallocate a customer (remove allocation)
     */
    @Transactional
    public void deallocateCustomer(Long customerId, Long userId, String reason, HttpServletRequest httpRequest) {
        // Check permission
        checkAllocationPermission();
        
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        if (userId == null) {
            throw new IllegalArgumentException("User ID cannot be null");
        }
        
        // Get customer
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));
        
        // Get allocation
        CustomerAllocation allocation = allocationRepository
                .findActiveAllocationByCustomerAndUser(customerId, userId)
                .orElseThrow(() -> new IllegalArgumentException("Active allocation not found"));
        
        // Deactivate allocation
        allocation.setStatus("INACTIVE");
        allocation.setDeallocatedAt(LocalDateTime.now());
        allocation.setDeallocationReason(reason);
        
        CustomerAllocation savedAllocation = allocationRepository.save(allocation);
        
        // Log audit
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("status", "ACTIVE");
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("status", "INACTIVE");
        newValues.put("deallocationReason", reason);
        
        Long allocationId = savedAllocation.getId();
        if (allocationId != null) {
            auditLogService.logAction("DEALLOCATE", "CUSTOMER_ALLOCATION", allocationId, 
                    oldValues, newValues, httpRequest);
        }
        
        log.info("Customer {} deallocated from user {} by {}", 
                customer.getCustomerCode(), userId, getCurrentUser().getEmployeeId());
    }

    /**
     * Get all active allocations for a customer
     */
    @Transactional(readOnly = true)
    public List<CustomerAllocation> getCustomerAllocations(Long customerId) {
        return allocationRepository.findByCustomerId(customerId);
    }

    /**
     * Get all active allocations for a user
     */
    @Transactional(readOnly = true)
    public List<CustomerAllocation> getUserAllocations(Long userId) {
        return allocationRepository.findActiveAllocationsByUserId(userId);
    }

    /**
     * Get all active allocations (requires ASSIGN_CUSTOMERS permission)
     */
    @Transactional(readOnly = true)
    public List<CustomerAllocation> getAllActiveAllocations() {
        checkAllocationPermission();
        return allocationRepository.findByStatus("ACTIVE");
    }

    private void checkAllocationPermission() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        if (!userPrincipal.hasPermission("ASSIGN_CUSTOMERS")) {
            throw new AccessDeniedException("Insufficient permissions to allocate customers");
        }
    }

    private void validateUserRole(Long userId, String requiredRoleCode) {
        List<String> userRoles = userRoleRepository.findRoleCodesByUserId(userId);
        if (!userRoles.contains(requiredRoleCode)) {
            throw new IllegalArgumentException(
                    String.format("User does not have the required role: %s. User has roles: %s", 
                            requiredRoleCode, String.join(", ", userRoles)));
        }
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

    private String buildCustomerName(Customer customer) {
        StringBuilder name = new StringBuilder();
        if (customer.getFirstName() != null) {
            name.append(customer.getFirstName());
        }
        if (customer.getMiddleName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(customer.getMiddleName());
        }
        if (customer.getLastName() != null) {
            if (name.length() > 0) name.append(" ");
            name.append(customer.getLastName());
        }
        return name.length() > 0 ? name.toString() : customer.getCustomerCode();
    }
}
