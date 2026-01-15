package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.CustomerUploadError;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface CustomerUploadErrorRepository extends JpaRepository<CustomerUploadError, Long> {
    List<CustomerUploadError> findByUploadIdOrderByRowNumberAsc(Long uploadId);
}
