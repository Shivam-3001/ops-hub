package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.CreateUserRequestDTO;
import com.company.ops_hub_api.dto.UpdateUserStatusDTO;
import com.company.ops_hub_api.dto.UserManagementUserDTO;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.service.UserManagementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/user-management")
@RequiredArgsConstructor
public class UserManagementController {

    private final UserManagementService userManagementService;

    @GetMapping("/users")
    @RequiresPermission("MANAGE_USERS")
    public ResponseEntity<List<UserManagementUserDTO>> listUsers() {
        return ResponseEntity.ok(userManagementService.listUsers());
    }

    @PostMapping("/users")
    @RequiresPermission("MANAGE_USERS")
    public ResponseEntity<UserManagementUserDTO> createUser(
            @Valid @RequestBody CreateUserRequestDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(userManagementService.createUser(dto, request));
    }

    @PatchMapping("/users/{userId}/status")
    @RequiresPermission("MANAGE_USERS")
    public ResponseEntity<UserManagementUserDTO> updateUserStatus(
            @PathVariable Long userId,
            @Valid @RequestBody UpdateUserStatusDTO dto,
            HttpServletRequest request) {
        return ResponseEntity.ok(userManagementService.updateUserStatus(userId, dto, request));
    }
}
