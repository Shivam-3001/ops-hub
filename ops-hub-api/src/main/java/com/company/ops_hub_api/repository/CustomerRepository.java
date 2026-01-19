package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.Customer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.math.BigDecimal;

@Repository
public interface CustomerRepository extends JpaRepository<Customer, Long> {
    Optional<Customer> findByCustomerCode(String customerCode);
    boolean existsByCustomerCode(String customerCode);
    
    @Query("SELECT DISTINCT c FROM Customer c " +
           "JOIN c.allocations a " +
           "WHERE a.user.id = :userId AND a.status = 'ACTIVE'")
    List<Customer> findCustomersByAssignedUserId(@Param("userId") Long userId);
    
    @Query("SELECT c FROM Customer c WHERE c.area.id = :areaId")
    List<Customer> findByAreaId(@Param("areaId") Long areaId);

    List<Customer> findByAreaZoneId(Long zoneId);
    List<Customer> findByAreaZoneCircleId(Long circleId);
    List<Customer> findByAreaZoneCircleClusterId(Long clusterId);
    
    List<Customer> findByStatus(String status);

    @Query("SELECT COALESCE(SUM(c.pendingAmount), 0) FROM Customer c")
    BigDecimal sumPendingAmountAll();

    @Query("SELECT COALESCE(SUM(c.pendingAmount), 0) FROM Customer c WHERE c.id IN :customerIds")
    BigDecimal sumPendingAmountByIds(@Param("customerIds") List<Long> customerIds);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = :status")
    long countByStatusValue(@Param("status") String status);

    @Query("SELECT COUNT(c) FROM Customer c WHERE c.status = :status AND c.id IN :customerIds")
    long countByStatusValueAndIds(@Param("status") String status, @Param("customerIds") List<Long> customerIds);

    boolean existsByPhoneEncrypted(String phoneEncrypted);
    boolean existsByEmailEncrypted(String emailEncrypted);
}
