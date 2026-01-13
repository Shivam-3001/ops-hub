package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.ReportExport;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface ReportExportRepository extends JpaRepository<ReportExport, Long> {
    @Query("SELECT e FROM ReportExport e WHERE e.report.id = :reportId ORDER BY e.exportedAt DESC")
    List<ReportExport> findByReportId(@Param("reportId") Long reportId);
    
    @Query("SELECT e FROM ReportExport e WHERE e.exportedBy.id = :userId ORDER BY e.exportedAt DESC")
    List<ReportExport> findByUserId(@Param("userId") Long userId);
    
    @Query("SELECT e FROM ReportExport e WHERE e.exportStatus = :status ORDER BY e.exportedAt DESC")
    List<ReportExport> findByStatus(@Param("status") String status);
    
    @Query("SELECT e FROM ReportExport e WHERE e.exportStatus = :status AND e.exportedAt < :beforeDate")
    List<ReportExport> findStaleExports(@Param("status") String status, @Param("beforeDate") LocalDateTime beforeDate);
    
    Optional<ReportExport> findByIdAndExportedById(Long id, Long userId);
}
