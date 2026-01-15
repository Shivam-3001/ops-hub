package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.Cluster;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ClusterRepository extends JpaRepository<Cluster, Long> {
    Optional<Cluster> findByCode(String code);
    Optional<Cluster> findByNameIgnoreCase(String name);
    boolean existsByCode(String code);
}
