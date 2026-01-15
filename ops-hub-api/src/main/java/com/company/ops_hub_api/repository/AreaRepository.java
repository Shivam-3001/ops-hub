package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.Area;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface AreaRepository extends JpaRepository<Area, Long> {
    Optional<Area> findByCode(String code);
    Optional<Area> findByNameIgnoreCase(String name);
    List<Area> findByZoneId(Long zoneId);
    boolean existsByCode(String code);
}
