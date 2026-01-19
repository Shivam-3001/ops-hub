package com.company.ops_hub_api.repository;

import com.company.ops_hub_api.domain.AppNotification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface AppNotificationRepository extends JpaRepository<AppNotification, Long> {

    List<AppNotification> findByUserIdOrderByCreatedAtDesc(Long userId);

    @Query("SELECT n FROM AppNotification n WHERE n.user.id = :userId ORDER BY n.createdAt DESC")
    List<AppNotification> findRecentByUserId(@Param("userId") Long userId);

    long countByUserIdAndReadAtIsNull(Long userId);

    boolean existsByUserIdAndEntityTypeAndEntityIdAndNotificationType(Long userId, String entityType, Long entityId, String notificationType);

    @Query("SELECT n FROM AppNotification n WHERE n.user.id = :userId AND n.createdAt >= :since ORDER BY n.createdAt DESC")
    List<AppNotification> findByUserIdSince(@Param("userId") Long userId, @Param("since") LocalDateTime since);
}
