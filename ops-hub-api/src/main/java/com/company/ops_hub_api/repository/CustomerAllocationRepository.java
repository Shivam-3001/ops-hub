package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.CustomerAllocation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CustomerAllocationRepository extends JpaRepository<CustomerAllocation, Long> {
    
    @Query("SELECT a FROM CustomerAllocation a WHERE a.customer.id = :customerId AND a.status = 'ACTIVE'")
    List<CustomerAllocation> findActiveAllocationsByCustomerId(@Param("customerId") Long customerId);
    
    @Query("SELECT a FROM CustomerAllocation a WHERE a.user.id = :userId AND a.status = 'ACTIVE'")
    List<CustomerAllocation> findActiveAllocationsByUserId(@Param("userId") Long userId);
    
    @Query("SELECT a FROM CustomerAllocation a WHERE a.customer.id = :customerId AND a.user.id = :userId AND a.status = 'ACTIVE'")
    Optional<CustomerAllocation> findActiveAllocationByCustomerAndUser(
            @Param("customerId") Long customerId, 
            @Param("userId") Long userId);
    
    List<CustomerAllocation> findByCustomerId(Long customerId);
    List<CustomerAllocation> findByUserId(Long userId);
    List<CustomerAllocation> findByStatus(String status);
}
