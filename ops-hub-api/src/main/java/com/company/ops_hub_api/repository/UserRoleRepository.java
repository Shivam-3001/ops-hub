package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.UserRole;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserRoleRepository extends JpaRepository<UserRole, Long> {
    List<UserRole> findByUserId(Long userId);
    
    @Query("SELECT ur.role FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.active = true")
    List<com.company.ops_hub_api.domain.Role> findRolesByUserId(@Param("userId") Long userId);
    
    @Query("SELECT ur.role.code FROM UserRole ur WHERE ur.user.id = :userId AND ur.role.active = true")
    List<String> findRoleCodesByUserId(@Param("userId") Long userId);
    
    boolean existsByUserIdAndRoleId(Long userId, Long roleId);
}
