package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.ChangePasswordRequestDTO;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
@Transactional
public class AccountService {

    private static final String PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuditLogService auditLogService;

    public void changePassword(ChangePasswordRequestDTO request) {
        if (request == null) {
            throw new IllegalArgumentException("Request cannot be null");
        }

        User user = getCurrentUser();

        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Current password is incorrect");
        }

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }

        if (!request.getNewPassword().matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and include uppercase, lowercase, number, and special character"
            );
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);

        auditLogService.logAction(
                "CHANGE_PASSWORD",
                "USER",
                user.getId(),
                null,
                Map.of("changed", true)
        );
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
}
