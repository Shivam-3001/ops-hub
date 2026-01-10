package com.company.ops_hub_api.config;

import org.springframework.boot.autoconfigure.domain.EntityScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@Configuration
@EnableJpaRepositories(basePackages = "com.company.ops_hub_api.repository")
@EntityScan(basePackages = "com.company.ops_hub_api.domain")
@EnableJpaAuditing
@EnableTransactionManagement
public class JpaConfig {
    // JPA configuration handled by annotations
}
