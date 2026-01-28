package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.domain.CustomerAllocation;
import com.company.ops_hub_api.dto.AllocateCustomerDTO;
import com.company.ops_hub_api.dto.AssignableUserDTO;
import com.company.ops_hub_api.dto.CustomerAllocationDTO;
import com.company.ops_hub_api.dto.ReassignCustomerDTO;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.service.CustomerAllocationService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customer-allocations")
@RequiredArgsConstructor
public class CustomerAllocationController {

    private final CustomerAllocationService allocationService;

    /**
     * Allocate a customer to a user (requires ASSIGN_CUSTOMERS permission)
     */
    @PostMapping
    @RequiresPermission("ASSIGN_CUSTOMERS")
    public ResponseEntity<CustomerAllocationDTO> allocateCustomer(
            @Valid @RequestBody AllocateCustomerDTO dto,
            HttpServletRequest httpRequest) {
        CustomerAllocation allocation = allocationService.allocateCustomer(dto, httpRequest);
        return ResponseEntity.ok(toDTO(allocation));
    }

    /**
     * Reassign a customer to a new user (requires ASSIGN_CUSTOMERS permission)
     */
    @PostMapping("/reassign")
    @RequiresPermission("ASSIGN_CUSTOMERS")
    public ResponseEntity<CustomerAllocationDTO> reassignCustomer(
            @Valid @RequestBody ReassignCustomerDTO dto,
            HttpServletRequest httpRequest) {
        CustomerAllocation allocation = allocationService.reassignCustomer(dto, httpRequest);
        return ResponseEntity.ok(toDTO(allocation));
    }

    /**
     * Deallocate a customer (requires ASSIGN_CUSTOMERS permission)
     */
    @DeleteMapping("/customers/{customerId}/users/{userId}")
    @RequiresPermission("ASSIGN_CUSTOMERS")
    public ResponseEntity<Void> deallocateCustomer(
            @PathVariable Long customerId,
            @PathVariable Long userId,
            @RequestParam(required = false, defaultValue = "Manual deallocation") String reason,
            HttpServletRequest httpRequest) {
        allocationService.deallocateCustomer(customerId, userId, reason, httpRequest);
        return ResponseEntity.noContent().build();
    }

    /**
     * Get all allocations for a customer (requires ASSIGN_CUSTOMERS permission)
     */
    @GetMapping("/customers/{customerId}")
    @RequiresPermission("ASSIGN_CUSTOMERS")
    public ResponseEntity<List<CustomerAllocationDTO>> getCustomerAllocations(@PathVariable Long customerId) {
        List<CustomerAllocation> allocations = allocationService.getCustomerAllocations(customerId);
        return ResponseEntity.ok(allocations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get all active allocations for current user
     */
    @GetMapping("/my-allocations")
    public ResponseEntity<List<CustomerAllocationDTO>> getMyAllocations() {
        org.springframework.security.core.Authentication authentication = 
                org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        
        if (authentication == null || !(authentication.getPrincipal() instanceof com.company.ops_hub_api.security.UserPrincipal)) {
            return ResponseEntity.badRequest().build();
        }
        
        com.company.ops_hub_api.security.UserPrincipal userPrincipal = 
                (com.company.ops_hub_api.security.UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        
        if (userId == null) {
            return ResponseEntity.badRequest().build();
        }
        
        List<CustomerAllocation> allocations = allocationService.getUserAllocations(userId);
        return ResponseEntity.ok(allocations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get all active allocations (requires ASSIGN_CUSTOMERS permission)
     */
    @GetMapping
    @RequiresPermission("ASSIGN_CUSTOMERS")
    public ResponseEntity<List<CustomerAllocationDTO>> getAllActiveAllocations() {
        List<CustomerAllocation> allocations = allocationService.getAllActiveAllocations();
        return ResponseEntity.ok(allocations.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get assignable users for the current allocator (next hierarchy level, in scope)
     */
    @GetMapping("/assignees")
    @RequiresPermission("ASSIGN_CUSTOMERS")
    public ResponseEntity<List<AssignableUserDTO>> getAssignableUsers() {
        return ResponseEntity.ok(allocationService.getAssignableUsers());
    }

    private CustomerAllocationDTO toDTO(CustomerAllocation allocation) {
        return CustomerAllocationDTO.builder()
                .id(allocation.getId())
                .customerId(allocation.getCustomer() != null ? allocation.getCustomer().getId() : null)
                .customerCode(allocation.getCustomer() != null ? allocation.getCustomer().getCustomerCode() : null)
                .customerName(allocation.getCustomer() != null ? 
                        buildCustomerName(allocation.getCustomer()) : null)
                .userId(allocation.getUser() != null ? allocation.getUser().getId() : null)
                .userEmployeeId(allocation.getUser() != null ? allocation.getUser().getEmployeeId() : null)
                .userName(allocation.getUser() != null ? 
                        (allocation.getUser().getFullName() != null ? 
                                allocation.getUser().getFullName() : allocation.getUser().getUsername()) : null)
                .roleCode(allocation.getRoleCode())
                .allocationType(allocation.getAllocationType())
                .status(allocation.getStatus())
                .allocatedBy(allocation.getAllocatedBy() != null ? allocation.getAllocatedBy().getId() : null)
                .allocatedByEmployeeId(allocation.getAllocatedBy() != null ? 
                        allocation.getAllocatedBy().getEmployeeId() : null)
                .allocatedByName(allocation.getAllocatedBy() != null ? 
                        (allocation.getAllocatedBy().getFullName() != null ? 
                                allocation.getAllocatedBy().getFullName() : 
                                allocation.getAllocatedBy().getUsername()) : null)
                .allocatedAt(allocation.getAllocatedAt())
                .deallocatedAt(allocation.getDeallocatedAt())
                .deallocationReason(allocation.getDeallocationReason())
                .notes(allocation.getNotes())
                .build();
    }

    private String buildCustomerName(com.company.ops_hub_api.domain.Customer customer) {
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
