package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.CustomerUpload;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerUploadRepository extends JpaRepository<CustomerUpload, Long> {
    List<CustomerUpload> findByUploadedByIdOrderByUploadedAtDesc(Long uploadedById);
}
