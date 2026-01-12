package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@Entity
@Table(name = "customer_allocations")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CustomerAllocation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "customer_id", nullable = false)
    private Customer customer;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(nullable = false, length = 50, name = "role_code")
    private String roleCode;

    @Column(nullable = false, length = 50, name = "allocation_type")
    private String allocationType; // PRIMARY, SECONDARY, TEMPORARY

    @Column(nullable = false, length = 50)
    private String status = "ACTIVE"; // ACTIVE, INACTIVE, TRANSFERRED

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "allocated_by", nullable = false)
    private User allocatedBy;

    @CreatedDate
    @Column(nullable = false, name = "allocated_at")
    private LocalDateTime allocatedAt;

    @Column(name = "deallocated_at")
    private LocalDateTime deallocatedAt;

    @Column(length = 500, name = "deallocation_reason")
    private String deallocationReason;

    @Column(length = 1000)
    private String notes;
}
