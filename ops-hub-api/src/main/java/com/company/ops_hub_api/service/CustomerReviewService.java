package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.Customer;
import com.company.ops_hub_api.domain.CustomerReview;
import com.company.ops_hub_api.domain.CustomerVisit;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.SubmitReviewDTO;
import com.company.ops_hub_api.repository.CustomerReviewRepository;
import com.company.ops_hub_api.repository.CustomerVisitRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerReviewService {

    private final CustomerReviewRepository reviewRepository;
    private final CustomerVisitRepository visitRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    /**
     * Submit a review for a visit
     * Users can only submit reviews for their own visits
     */
    @Transactional
    public CustomerReview submitReview(SubmitReviewDTO dto, HttpServletRequest httpRequest) {
        // Get current user
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }

        // Get visit
        Long visitId = dto.getVisitId();
        if (visitId == null) {
            throw new IllegalArgumentException("Visit ID cannot be null");
        }
        CustomerVisit visit = visitRepository.findById(visitId)
                .orElseThrow(() -> new IllegalArgumentException("Visit not found"));

        // Validate visit belongs to user
        Long visitUserId = visit.getUser() != null ? visit.getUser().getId() : null;
        if (visitUserId == null || !visitUserId.equals(userId)) {
            throw new AccessDeniedException("You can only submit reviews for your own visits");
        }

        // Check if review already exists
        reviewRepository.findByVisitId(visitId)
                .ifPresent(existing -> {
                    throw new IllegalStateException("A review already exists for this visit");
                });

        // Get customer
        Customer customer = visit.getCustomer();
        if (customer == null) {
            throw new IllegalStateException("Visit must be associated with a customer");
        }

        // Create review
        CustomerReview review = new CustomerReview();
        review.setVisit(visit);
        review.setCustomer(customer);
        review.setUser(currentUser);
        review.setRating(dto.getRating());
        review.setReviewText(dto.getReviewText());
        review.setReviewCategories(dto.getReviewCategories());
        review.setIsPositive(dto.getIsPositive() != null ? dto.getIsPositive() : dto.getRating() >= 3);

        CustomerReview savedReview = reviewRepository.save(review);

        // Log audit
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("reviewId", savedReview.getId());
        newValues.put("visitId", visitId);
        newValues.put("customerId", customer.getId());
        newValues.put("customerCode", customer.getCustomerCode());
        newValues.put("userId", userId);
        newValues.put("rating", dto.getRating());
        newValues.put("isPositive", savedReview.getIsPositive());

        Long reviewId = savedReview.getId();
        if (reviewId != null) {
            auditLogService.logAction("CREATE", "CUSTOMER_REVIEW", reviewId, 
                    null, newValues, httpRequest);
        }

        log.info("Review {} created for visit {} by user {}", 
                savedReview.getId(), visitId, currentUser.getEmployeeId());

        return savedReview;
    }

    /**
     * Get review for a visit
     */
    @Transactional(readOnly = true)
    public CustomerReview getReviewByVisitId(Long visitId) {
        CustomerReview review = reviewRepository.findByVisitId(visitId)
                .orElseThrow(() -> new IllegalArgumentException("Review not found"));

        // Validate access - user must own the visit
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }

        Long visitUserId = review.getVisit() != null && review.getVisit().getUser() != null 
                ? review.getVisit().getUser().getId() : null;
        if (visitUserId == null || !visitUserId.equals(userId)) {
            throw new AccessDeniedException("You do not have access to this review");
        }

        return review;
    }

    /**
     * Get all reviews for current user
     */
    @Transactional(readOnly = true)
    public List<CustomerReview> getMyReviews() {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return reviewRepository.findByUserId(userId);
    }

    /**
     * Get reviews for a customer (only if allocated to user)
     */
    @Transactional(readOnly = true)
    public List<CustomerReview> getCustomerReviews(Long customerId) {
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }

        // For simplicity, we'll allow if user has any visits for this customer
        // This ensures user has access to the customer
        List<CustomerVisit> visits = visitRepository
                .findByCustomerIdAndUserId(customerId, userId);
        
        if (visits.isEmpty()) {
            throw new AccessDeniedException("You do not have access to reviews for this customer");
        }

        return reviewRepository.findByCustomerId(customerId).stream()
                .filter(review -> {
                    Long reviewUserId = review.getUser() != null ? review.getUser().getId() : null;
                    return reviewUserId != null && reviewUserId.equals(userId);
                })
                .toList();
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
