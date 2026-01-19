package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.NotificationListDTO;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.service.NotificationService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService notificationService;

    @GetMapping
    @RequiresPermission("VIEW_NOTIFICATIONS")
    public ResponseEntity<NotificationListDTO> getMyNotifications(@RequestParam(defaultValue = "10") int limit) {
        return ResponseEntity.ok(notificationService.getMyNotifications(limit));
    }

    @PostMapping("/{notificationId}/read")
    @RequiresPermission("VIEW_NOTIFICATIONS")
    public ResponseEntity<Void> markAsRead(@PathVariable Long notificationId, HttpServletRequest request) {
        notificationService.markAsRead(notificationId, request);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/read-all")
    @RequiresPermission("VIEW_NOTIFICATIONS")
    public ResponseEntity<Void> markAllAsRead(HttpServletRequest request) {
        notificationService.markAllAsRead(request);
        return ResponseEntity.ok().build();
    }
}
