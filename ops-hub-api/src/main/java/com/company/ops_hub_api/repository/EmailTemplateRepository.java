package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.EmailTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface EmailTemplateRepository extends JpaRepository<EmailTemplate, Long> {
    Optional<EmailTemplate> findByTemplateCode(String templateCode);
    Optional<EmailTemplate> findByTemplateCodeAndActiveTrue(String templateCode);
    boolean existsByTemplateCode(String templateCode);
}
