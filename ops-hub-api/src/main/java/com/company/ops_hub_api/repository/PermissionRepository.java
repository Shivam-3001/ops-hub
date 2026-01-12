package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.Permission;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface PermissionRepository extends JpaRepository<Permission, Long> {
    Optional<Permission> findByCode(String code);
    boolean existsByCode(String code);
    
    @Query("SELECT p FROM Permission p JOIN p.roles r WHERE r.code = :roleCode AND p.active = true")
    List<Permission> findByRoleCode(@Param("roleCode") String roleCode);
}
