package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.AiAction;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AiActionRepository extends JpaRepository<AiAction, Long> {
    @Query("SELECT a FROM AiAction a WHERE a.conversation.id = :conversationId ORDER BY a.createdAt DESC")
    List<AiAction> findByConversationId(@Param("conversationId") Long conversationId);
    
    @Query("SELECT a FROM AiAction a WHERE a.conversation.user.id = :userId ORDER BY a.createdAt DESC")
    List<AiAction> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT a FROM AiAction a WHERE a.executionStatus = :status AND a.createdAt < :beforeDate")
    List<AiAction> findPendingActionsForRetry(@Param("status") String status, @Param("beforeDate") LocalDateTime beforeDate);
    
    @Query("SELECT a FROM AiAction a WHERE a.actionType = :actionType AND a.conversation.user.id = :userId ORDER BY a.createdAt DESC")
    List<AiAction> findByActionTypeAndUserId(@Param("actionType") String actionType, @Param("userId") Long userId);
}
