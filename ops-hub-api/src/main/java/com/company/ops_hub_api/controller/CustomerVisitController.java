package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.domain.CustomerVisit;
import com.company.ops_hub_api.dto.CustomerVisitDTO;
import com.company.ops_hub_api.dto.SubmitVisitDTO;
import com.company.ops_hub_api.service.CustomerVisitService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customer-visits")
@RequiredArgsConstructor
public class CustomerVisitController {

    private final CustomerVisitService visitService;

    /**
     * Submit a customer visit
     * Users can only submit visits for customers allocated to them
     */
    @PostMapping
    public ResponseEntity<CustomerVisitDTO> submitVisit(
            @Valid @RequestBody SubmitVisitDTO dto,
            HttpServletRequest httpRequest) {
        CustomerVisit visit = visitService.submitVisit(dto, httpRequest);
        return ResponseEntity.ok(toDTO(visit));
    }

    /**
     * Get all visits for current user
     */
    @GetMapping("/my-visits")
    public ResponseEntity<List<CustomerVisitDTO>> getMyVisits() {
        List<CustomerVisit> visits = visitService.getMyVisits();
        return ResponseEntity.ok(visits.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get visits for a specific customer
     * Only returns visits for customers allocated to user
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<List<CustomerVisitDTO>> getCustomerVisits(@PathVariable Long customerId) {
        List<CustomerVisit> visits = visitService.getCustomerVisits(customerId);
        return ResponseEntity.ok(visits.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get a specific visit by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<CustomerVisitDTO> getVisitById(@PathVariable Long id) {
        CustomerVisit visit = visitService.getVisitById(id);
        return ResponseEntity.ok(toDTO(visit));
    }

    /**
     * Update visit status
     */
    @PatchMapping("/{id}/status")
    public ResponseEntity<CustomerVisitDTO> updateVisitStatus(
            @PathVariable Long id,
            @RequestParam String status,
            HttpServletRequest httpRequest) {
        CustomerVisit visit = visitService.updateVisitStatus(id, status, httpRequest);
        return ResponseEntity.ok(toDTO(visit));
    }

    private CustomerVisitDTO toDTO(CustomerVisit visit) {
        return CustomerVisitDTO.builder()
                .id(visit.getId())
                .customerId(visit.getCustomer() != null ? visit.getCustomer().getId() : null)
                .customerCode(visit.getCustomer() != null ? visit.getCustomer().getCustomerCode() : null)
                .customerName(visit.getCustomer() != null ? buildCustomerName(visit.getCustomer()) : null)
                .userId(visit.getUser() != null ? visit.getUser().getId() : null)
                .userEmployeeId(visit.getUser() != null ? visit.getUser().getEmployeeId() : null)
                .userName(visit.getUser() != null ? 
                        (visit.getUser().getFullName() != null ? 
                                visit.getUser().getFullName() : visit.getUser().getUsername()) : null)
                .visitDate(visit.getVisitDate())
                .visitType(visit.getVisitType())
                .purpose(visit.getPurpose())
                .notes(visit.getNotes())
                .latitude(visit.getLatitude())
                .longitude(visit.getLongitude())
                .address(visit.getAddress())
                .visitStatus(visit.getVisitStatus())
                .scheduledAt(visit.getScheduledAt())
                .startedAt(visit.getStartedAt())
                .completedAt(visit.getCompletedAt())
                .createdAt(visit.getCreatedAt())
                .updatedAt(visit.getUpdatedAt())
                .hasReview(visit.getReview() != null)
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
