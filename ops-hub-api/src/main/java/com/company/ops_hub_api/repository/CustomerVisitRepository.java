package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.CustomerVisit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;

@Repository
public interface CustomerVisitRepository extends JpaRepository<CustomerVisit, Long> {
    
    List<CustomerVisit> findByCustomerId(Long customerId);
    List<CustomerVisit> findByUserId(Long userId);
    
    @Query("SELECT v FROM CustomerVisit v WHERE v.customer.id = :customerId AND v.user.id = :userId")
    List<CustomerVisit> findByCustomerIdAndUserId(@Param("customerId") Long customerId, @Param("userId") Long userId);
    
    @Query("SELECT v FROM CustomerVisit v WHERE v.customer.id = :customerId AND v.user.id = :userId " +
           "AND CAST(v.visitDate AS date) = :visitDate")
    List<CustomerVisit> findByCustomerIdAndUserIdAndVisitDate(
            @Param("customerId") Long customerId,
            @Param("userId") Long userId,
            @Param("visitDate") LocalDate visitDate);
    
    @Query("SELECT v FROM CustomerVisit v WHERE v.customer.id = :customerId " +
           "AND CAST(v.visitDate AS date) = :visitDate")
    List<CustomerVisit> findByCustomerIdAndVisitDate(
            @Param("customerId") Long customerId,
            @Param("visitDate") LocalDate visitDate);
    
    List<CustomerVisit> findByVisitStatus(String visitStatus);

    long countByUserId(Long userId);

    @Query("SELECT COUNT(v) FROM CustomerVisit v WHERE v.customer.id IN :customerIds")
    long countByCustomerIds(@Param("customerIds") List<Long> customerIds);

    @Query("SELECT COUNT(v) FROM CustomerVisit v WHERE v.user.id = :userId AND v.visitDate BETWEEN :start AND :end")
    long countByUserIdAndVisitDateBetween(@Param("userId") Long userId,
                                          @Param("start") java.time.LocalDateTime start,
                                          @Param("end") java.time.LocalDateTime end);
}
