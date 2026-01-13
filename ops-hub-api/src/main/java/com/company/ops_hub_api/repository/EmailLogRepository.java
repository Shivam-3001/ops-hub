package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.EmailLog;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface EmailLogRepository extends JpaRepository<EmailLog, Long> {
    List<EmailLog> findByEmailStatus(String status);
    
    @Query("SELECT e FROM EmailLog e WHERE e.emailStatus = :status AND e.createdAt < :beforeDate")
    List<EmailLog> findFailedEmailsForRetry(@Param("status") String status, @Param("beforeDate") LocalDateTime beforeDate);
    
    List<EmailLog> findByTemplateCode(String templateCode);
    
    @Query("SELECT e FROM EmailLog e WHERE e.recipientEmailEncrypted = :encryptedEmail ORDER BY e.createdAt DESC")
    List<EmailLog> findByRecipientEmail(@Param("encryptedEmail") String encryptedEmail);
}
