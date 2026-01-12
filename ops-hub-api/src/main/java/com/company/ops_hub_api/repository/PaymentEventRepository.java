package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.PaymentEvent;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PaymentEventRepository extends JpaRepository<PaymentEvent, Long> {
    List<PaymentEvent> findByPaymentId(Long paymentId);
    
    @Query("SELECT pe FROM PaymentEvent pe WHERE pe.payment.id = :paymentId AND pe.eventType = :eventType")
    List<PaymentEvent> findByPaymentIdAndEventType(@Param("paymentId") Long paymentId, @Param("eventType") String eventType);
    
    @Query("SELECT pe FROM PaymentEvent pe WHERE pe.payment.paymentReference = :paymentReference AND pe.gatewayTransactionId = :gatewayTransactionId")
    Optional<PaymentEvent> findByPaymentReferenceAndGatewayTransactionId(
            @Param("paymentReference") String paymentReference,
            @Param("gatewayTransactionId") String gatewayTransactionId);
}
