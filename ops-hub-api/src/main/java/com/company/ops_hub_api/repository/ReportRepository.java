package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.Report;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ReportRepository extends JpaRepository<Report, Long> {
    Optional<Report> findByReportCode(String reportCode);
    
    @Query("SELECT r FROM Report r WHERE r.active = true ORDER BY r.name")
    List<Report> findAllActive();
    
    @Query("SELECT r FROM Report r WHERE r.active = true AND r.category = :category ORDER BY r.name")
    List<Report> findActiveByCategory(@Param("category") String category);
    
    @Query("SELECT r FROM Report r WHERE r.active = true AND r.reportType = :reportType ORDER BY r.name")
    List<Report> findActiveByType(@Param("reportType") String reportType);
}
