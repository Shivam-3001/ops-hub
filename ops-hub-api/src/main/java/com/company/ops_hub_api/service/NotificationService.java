package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.AppNotification;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.NotificationDTO;
import com.company.ops_hub_api.dto.NotificationListDTO;
import com.company.ops_hub_api.repository.AppNotificationRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class NotificationService {

    private final AppNotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;

    @Transactional(readOnly = true)
    public NotificationListDTO getMyNotifications(int limit) {
        User currentUser = getCurrentUser();
        List<AppNotification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        if (limit > 0 && notifications.size() > limit) {
            notifications = notifications.subList(0, limit);
        }
        return NotificationListDTO.builder()
                .notifications(notifications.stream().map(this::toDTO).collect(Collectors.toList()))
                .unreadCount(notificationRepository.countByUserIdAndReadAtIsNull(currentUser.getId()))
                .build();
    }

    @Transactional
    public void markAsRead(Long notificationId, HttpServletRequest request) {
        if (notificationId == null) {
            throw new IllegalArgumentException("Notification ID cannot be null");
        }
        User currentUser = getCurrentUser();
        AppNotification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new IllegalArgumentException("Notification not found"));
        if (!Objects.equals(notification.getUser().getId(), currentUser.getId())) {
            throw new AccessDeniedException("You do not have access to this notification");
        }
        if (notification.getReadAt() == null) {
            notification.setReadAt(LocalDateTime.now());
            notificationRepository.save(notification);
            auditLogService.logAction("READ", "NOTIFICATION", notification.getId(), null, null, request);
        }
    }

    @Transactional
    public void markAllAsRead(HttpServletRequest request) {
        User currentUser = getCurrentUser();
        List<AppNotification> notifications = notificationRepository.findByUserIdOrderByCreatedAtDesc(currentUser.getId());
        LocalDateTime now = LocalDateTime.now();
        boolean updated = false;
        for (AppNotification notification : notifications) {
            if (notification.getReadAt() == null) {
                notification.setReadAt(now);
                notificationRepository.save(notification);
                updated = true;
            }
        }
        if (updated) {
            auditLogService.logAction("READ_ALL", "NOTIFICATION", null, null, null, request);
        }
    }

    @Transactional
    public void notifyUser(User user, String type, String title, String message, String entityType,
                           Long entityId, String severity) {
        if (user == null || user.getId() == null) {
            return;
        }
        AppNotification notification = new AppNotification();
        notification.setUser(user);
        notification.setNotificationType(type);
        notification.setTitle(title);
        notification.setMessage(message);
        notification.setEntityType(entityType);
        notification.setEntityId(entityId);
        notification.setSeverity(severity != null ? severity : "INFO");
        notificationRepository.save(notification);
    }

    @Transactional
    public void notifyUserOnce(User user, String type, String title, String message, String entityType,
                               Long entityId, String severity) {
        if (user == null || user.getId() == null) {
            return;
        }
        if (entityType != null && entityId != null) {
            boolean exists = notificationRepository.existsByUserIdAndEntityTypeAndEntityIdAndNotificationType(
                    user.getId(), entityType, entityId, type);
            if (exists) {
                return;
            }
        }
        notifyUser(user, type, title, message, entityType, entityId, severity);
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private NotificationDTO toDTO(AppNotification notification) {
        return NotificationDTO.builder()
                .id(notification.getId())
                .type(notification.getNotificationType())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .entityType(notification.getEntityType())
                .entityId(notification.getEntityId())
                .severity(notification.getSeverity())
                .createdAt(notification.getCreatedAt())
                .readAt(notification.getReadAt())
                .build();
    }
}
