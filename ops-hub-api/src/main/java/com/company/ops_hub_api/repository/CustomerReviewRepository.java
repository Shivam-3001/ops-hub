package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.CustomerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerReviewRepository extends JpaRepository<CustomerReview, Long> {
    Optional<CustomerReview> findByVisitId(Long visitId);
    List<CustomerReview> findByCustomerId(Long customerId);
    List<CustomerReview> findByUserId(Long userId);
}
