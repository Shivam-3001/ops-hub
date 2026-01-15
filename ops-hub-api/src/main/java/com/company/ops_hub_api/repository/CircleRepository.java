package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.Circle;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CircleRepository extends JpaRepository<Circle, Long> {
    Optional<Circle> findByCode(String code);
    Optional<Circle> findByNameIgnoreCase(String name);
    List<Circle> findByClusterId(Long clusterId);
    boolean existsByCode(String code);
}
