package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.domain.CustomerReview;
import com.company.ops_hub_api.dto.CustomerReviewDTO;
import com.company.ops_hub_api.dto.SubmitReviewDTO;
import com.company.ops_hub_api.service.CustomerReviewService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/customer-reviews")
@RequiredArgsConstructor
public class CustomerReviewController {

    private final CustomerReviewService reviewService;

    /**
     * Submit a review for a visit
     * Users can only submit reviews for their own visits
     */
    @PostMapping
    public ResponseEntity<CustomerReviewDTO> submitReview(
            @Valid @RequestBody SubmitReviewDTO dto,
            HttpServletRequest httpRequest) {
        CustomerReview review = reviewService.submitReview(dto, httpRequest);
        return ResponseEntity.ok(toDTO(review));
    }

    /**
     * Get review for a visit
     */
    @GetMapping("/visits/{visitId}")
    public ResponseEntity<CustomerReviewDTO> getReviewByVisitId(@PathVariable Long visitId) {
        CustomerReview review = reviewService.getReviewByVisitId(visitId);
        return ResponseEntity.ok(toDTO(review));
    }

    /**
     * Get all reviews for current user
     */
    @GetMapping("/my-reviews")
    public ResponseEntity<List<CustomerReviewDTO>> getMyReviews() {
        List<CustomerReview> reviews = reviewService.getMyReviews();
        return ResponseEntity.ok(reviews.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get reviews for a customer
     * Only returns reviews for customers allocated to user
     */
    @GetMapping("/customers/{customerId}")
    public ResponseEntity<List<CustomerReviewDTO>> getCustomerReviews(@PathVariable Long customerId) {
        List<CustomerReview> reviews = reviewService.getCustomerReviews(customerId);
        return ResponseEntity.ok(reviews.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    private CustomerReviewDTO toDTO(CustomerReview review) {
        return CustomerReviewDTO.builder()
                .id(review.getId())
                .visitId(review.getVisit() != null ? review.getVisit().getId() : null)
                .customerId(review.getCustomer() != null ? review.getCustomer().getId() : null)
                .customerCode(review.getCustomer() != null ? review.getCustomer().getCustomerCode() : null)
                .customerName(review.getCustomer() != null ? buildCustomerName(review.getCustomer()) : null)
                .userId(review.getUser() != null ? review.getUser().getId() : null)
                .userEmployeeId(review.getUser() != null ? review.getUser().getEmployeeId() : null)
                .userName(review.getUser() != null ? 
                        (review.getUser().getFullName() != null ? 
                                review.getUser().getFullName() : review.getUser().getUsername()) : null)
                .rating(review.getRating())
                .reviewText(review.getReviewText())
                .reviewCategories(review.getReviewCategories())
                .isPositive(review.getIsPositive())
                .createdAt(review.getCreatedAt())
                .updatedAt(review.getUpdatedAt())
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
