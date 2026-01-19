package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Customer;
import com.company.ops_hub_api.domain.CustomerVisit;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.SubmitVisitDTO;
import com.company.ops_hub_api.repository.CustomerAllocationRepository;
import com.company.ops_hub_api.repository.CustomerRepository;
import com.company.ops_hub_api.repository.CustomerVisitRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerVisitService {

    private final CustomerVisitRepository visitRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAllocationRepository allocationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Value("${app.visits.prevent-duplicate-per-day:true}")
    private boolean preventDuplicatePerDay;

    /**
     * Submit a customer visit
     * Users can only submit visits for customers allocated to them
     */
    @Transactional
    public CustomerVisit submitVisit(SubmitVisitDTO dto, HttpServletRequest httpRequest) {
        // Get current user
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }

        // Get customer
        Long customerId = dto.getCustomerId();
        if (customerId == null) {
            throw new IllegalArgumentException("Customer ID cannot be null");
        }
        Customer customer = customerRepository.findById(customerId)
                .orElseThrow(() -> new IllegalArgumentException("Customer not found"));

        // Check if customer is allocated to user
        validateCustomerAllocation(customerId, userId);

        // Check for duplicate visit on same day (if enabled)
        if (preventDuplicatePerDay) {
            LocalDate visitDate = dto.getVisitDate().toLocalDate();
            List<com.company.ops_hub_api.domain.CustomerVisit> existingVisits = visitRepository
                    .findByCustomerIdAndVisitDate(customerId, visitDate);

            if (!existingVisits.isEmpty()) {
                throw new IllegalStateException(
                        String.format("A visit already exists for customer %s on %s",
                                customer.getCustomerCode(), visitDate));
            }
        }

        // Create visit
        CustomerVisit visit = new CustomerVisit();
        visit.setCustomer(customer);
        visit.setUser(currentUser);
        visit.setVisitDate(dto.getVisitDate());
        visit.setVisitStatus(dto.getVisitStatus());
        visit.setNotes(dto.getNotes());
        visit.setVisitType(dto.getVisitType());
        visit.setPurpose(dto.getPurpose());
        visit.setLatitude(dto.getLatitude());
        visit.setLongitude(dto.getLongitude());
        visit.setAddress(dto.getAddress());
        visit.setScheduledAt(dto.getScheduledAt());
        visit.setStartedAt(dto.getStartedAt());
        visit.setCompletedAt(dto.getCompletedAt());

        // Set timestamps based on status
        LocalDateTime now = LocalDateTime.now();
        if ("SCHEDULED".equals(dto.getVisitStatus())) {
            visit.setScheduledAt(dto.getScheduledAt() != null ? dto.getScheduledAt() : now);
        } else if ("IN_PROGRESS".equals(dto.getVisitStatus())) {
            visit.setStartedAt(dto.getStartedAt() != null ? dto.getStartedAt() : now);
        } else if ("COMPLETED".equals(dto.getVisitStatus())) {
            visit.setCompletedAt(dto.getCompletedAt() != null ? dto.getCompletedAt() : now);
        }

        CustomerVisit savedVisit = visitRepository.save(visit);

        // Update customer status lifecycle
        updateCustomerStatus(customer, "VISITED");

        // Log audit
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("visitId", savedVisit.getId());
        newValues.put("customerId", customer.getId());
        newValues.put("customerCode", customer.getCustomerCode());
        newValues.put("userId", userId);
        newValues.put("visitDate", dto.getVisitDate().toString());
        newValues.put("visitStatus", dto.getVisitStatus());
        newValues.put("visitType", dto.getVisitType());

        Long visitId = savedVisit.getId();
        if (visitId != null) {
            auditLogService.logAction("CREATE", "CUSTOMER_VISIT", visitId, 
                    null, newValues, httpRequest);
        }

        log.info("Visit {} created for customer {} by user {}", 
                savedVisit.getId(), customer.getCustomerCode(), currentUser.getEmployeeId());

        return savedVisit;
    }

    /**
     * Get all visits for current user
     */
    @Transactional(readOnly = true)
    public List<CustomerVisit> getMyVisits() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return visitRepository.findByUserId(userId);
    }

    /**
     * Get visits for a specific customer (only if allocated to user)
     */
    @Transactional(readOnly = true)
    public List<CustomerVisit> getCustomerVisits(Long customerId) {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }

        // Check if customer is allocated to user
        validateCustomerAllocation(customerId, userId);

        return visitRepository.findByCustomerIdAndUserId(customerId, userId);
    }

    /**
     * Get a specific visit by ID (only if user has access)
     */
    @Transactional(readOnly = true)
    public CustomerVisit getVisitById(Long visitId) {
        if (visitId == null) {
            throw new IllegalArgumentException("Visit ID cannot be null");
        }
        CustomerVisit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new IllegalArgumentException("Visit not found"));

        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }

        // Check if visit belongs to user
        Long visitUserId = visit.getUser() != null ? visit.getUser().getId() : null;
        if (visitUserId == null || !visitUserId.equals(userId)) {
            throw new AccessDeniedException("You do not have access to this visit");
        }

        return visit;
    }

    /**
     * Update visit status
     */
    @Transactional
    public CustomerVisit updateVisitStatus(Long visitId, String newStatus, HttpServletRequest httpRequest) {
        CustomerVisit visit = getVisitById(visitId); // This already validates ownership
        
        String oldStatus = visit.getVisitStatus();
        visit.setVisitStatus(newStatus);

        // Update timestamps based on status
        LocalDateTime now = LocalDateTime.now();
        if ("IN_PROGRESS".equals(newStatus) && visit.getStartedAt() == null) {
            visit.setStartedAt(now);
        } else if ("COMPLETED".equals(newStatus) && visit.getCompletedAt() == null) {
            visit.setCompletedAt(now);
        }

        CustomerVisit savedVisit = visitRepository.save(visit);

        // Log audit
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("visitStatus", oldStatus);
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("visitStatus", newStatus);

        Long savedVisitId = savedVisit.getId();
        if (savedVisitId != null) {
            auditLogService.logAction("UPDATE", "CUSTOMER_VISIT", savedVisitId, 
                    oldValues, newValues, httpRequest);
        }

        return savedVisit;
    }

    private void validateCustomerAllocation(Long customerId, Long userId) {
        allocationRepository.findActiveAllocationByCustomerAndUser(customerId, userId)
                .orElseThrow(() -> new AccessDeniedException(
                        "Customer is not allocated to you. You cannot submit visits for this customer."));
    }

    private void updateCustomerStatus(Customer customer, String status) {
        if (customer == null || status == null || status.isBlank()) {
            return;
        }
        customer.setStatus(status);
        customerRepository.save(customer);
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
}
