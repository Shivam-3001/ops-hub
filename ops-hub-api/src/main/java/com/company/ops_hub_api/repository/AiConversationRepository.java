package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {
    Optional<AiConversation> findByConversationId(String conversationId);
    
    @Query("SELECT c FROM AiConversation c WHERE c.user.id = :userId AND c.status = 'ACTIVE' ORDER BY c.updatedAt DESC")
    List<AiConversation> findActiveConversationsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT c FROM AiConversation c WHERE c.user.id = :userId ORDER BY c.updatedAt DESC")
    List<AiConversation> findByUserId(@Param("userId") Long userId);
    
    boolean existsByConversationId(String conversationId);
}
