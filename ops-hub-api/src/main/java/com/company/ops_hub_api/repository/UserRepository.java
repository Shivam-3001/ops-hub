package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmployeeId(String employeeId);
    Optional<User> findByUsername(String username);
    Optional<User> findByEmail(String email);
    List<User> findByAreaId(Long areaId);
    List<User> findByAreaZoneCircleClusterId(Long clusterId);
    List<User> findByAreaZoneId(Long zoneId);
    boolean existsByEmployeeId(String employeeId);
    boolean existsByUsername(String username);
    boolean existsByEmail(String email);
    List<User> findByAreaZoneCircleId(Long circleId);
    List<User> findByAreaIdAndUserType(Long areaId, String userType);
    List<User> findByAreaZoneCircleIdAndUserType(Long circleId, String userType);
}
