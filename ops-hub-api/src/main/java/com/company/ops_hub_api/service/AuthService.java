package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.LoginRequest;
import com.company.ops_hub_api.dto.LoginResponse;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.util.JwtUtil;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public LoginResponse login(LoginRequest request) {
        // Find user by employeeId (trim and handle case)
        String employeeId = request.getEmployeeId() != null ? request.getEmployeeId().trim() : null;
        
        log.debug("Attempting login for employeeId: {}", employeeId);
        
        User user = userRepository.findByEmployeeId(employeeId)
                .orElseThrow(() -> {
                    log.warn("Login failed: User not found for employeeId: {}", employeeId);
                    return new BadCredentialsException("Invalid employee ID or password");
                });

        // Check if user is active
        if (!user.getActive()) {
            log.warn("Login failed: User account is inactive for employeeId: {}", employeeId);
            throw new BadCredentialsException("User account is inactive");
        }

        // Verify password
        boolean passwordMatches = passwordEncoder.matches(request.getPassword(), user.getPasswordHash());
        if (!passwordMatches) {
            log.warn("Login failed: Password mismatch for employeeId: {}", employeeId);
            throw new BadCredentialsException("Invalid employee ID or password");
        }
        
        log.info("Login successful for employeeId: {}", employeeId);

        // Generate JWT token
        String token = jwtUtil.generateToken(
                user.getEmployeeId(),
                user.getUsername(),
                user.getUserType(),
                user.getId()
        );

        // Note: Email and phone can be encrypted in DB using EncryptionUtil when creating/updating users
        // For login response, we return the stored values (can decrypt if needed)

        // Build response
        return LoginResponse.builder()
                .token(token)
                .employeeId(user.getEmployeeId())
                .username(user.getUsername())
                .fullName(user.getFullName())
                .userType(user.getUserType())
                .role(user.getRole())
                .areaName(user.getArea() != null ? user.getArea().getName() : null)
                .zoneName(user.getArea() != null && user.getArea().getZone() != null 
                        ? user.getArea().getZone().getName() : null)
                .circleName(user.getArea() != null && user.getArea().getZone() != null 
                        && user.getArea().getZone().getCircle() != null
                        ? user.getArea().getZone().getCircle().getName() : null)
                .clusterName(user.getArea() != null && user.getArea().getZone() != null 
                        && user.getArea().getZone().getCircle() != null
                        && user.getArea().getZone().getCircle().getCluster() != null
                        ? user.getArea().getZone().getCircle().getCluster().getName() : null)
                .build();
    }
}
