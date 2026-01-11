package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.Zone;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface ZoneRepository extends JpaRepository<Zone, Long> {
    Optional<Zone> findByCode(String code);
    List<Zone> findByCircleId(Long circleId);
    boolean existsByCode(String code);
}
