package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AuditLogRepository extends JpaRepository<AuditLog, Long> {
    List<AuditLog> findByUserId(Long userId);
    List<AuditLog> findByEntityTypeAndEntityId(String entityType, Long entityId);

    @Query("SELECT a FROM AuditLog a WHERE " +
            "(:actionType IS NULL OR a.actionType = :actionType) AND " +
            "(:entityType IS NULL OR a.entityType = :entityType) AND " +
            "(:userId IS NULL OR a.user.id = :userId) AND " +
            "(:startTime IS NULL OR a.createdAt >= :startTime) AND " +
            "(:endTime IS NULL OR a.createdAt <= :endTime)")
    Page<AuditLog> searchAuditLogs(
            @Param("actionType") String actionType,
            @Param("entityType") String entityType,
            @Param("userId") Long userId,
            @Param("startTime") java.time.LocalDateTime startTime,
            @Param("endTime") java.time.LocalDateTime endTime,
            Pageable pageable);
}
