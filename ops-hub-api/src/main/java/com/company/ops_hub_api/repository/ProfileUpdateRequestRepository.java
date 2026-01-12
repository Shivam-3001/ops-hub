package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.ProfileUpdateRequest;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface ProfileUpdateRequestRepository extends JpaRepository<ProfileUpdateRequest, Long> {
    List<ProfileUpdateRequest> findByUserId(Long userId);
    List<ProfileUpdateRequest> findByStatus(String status);
    
    @Query("SELECT pur FROM ProfileUpdateRequest pur WHERE pur.status = :status ORDER BY pur.requestedAt DESC")
    List<ProfileUpdateRequest> findPendingRequests(@Param("status") String status);
    
    List<ProfileUpdateRequest> findByUserIdAndStatus(Long userId, String status);
}
