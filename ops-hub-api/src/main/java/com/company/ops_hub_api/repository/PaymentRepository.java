package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.Payment;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentRepository extends JpaRepository<Payment, Long> {
    Optional<Payment> findByPaymentReference(String paymentReference);
    Optional<Payment> findByGatewayTransactionId(String gatewayTransactionId);
    boolean existsByPaymentReference(String paymentReference);
    
    @Query("SELECT p FROM Payment p WHERE p.customer.id = :customerId")
    List<Payment> findByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT p FROM Payment p WHERE p.user.id = :userId")
    List<Payment> findByUserId(@Param("userId") Long userId);
    
    List<Payment> findByPaymentStatus(String paymentStatus);
}
