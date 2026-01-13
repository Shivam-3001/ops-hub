package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.LoginRequest;
import com.company.ops_hub_api.dto.LoginResponse;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.service.AuthService;
import com.company.ops_hub_api.service.DataSeederService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final DataSeederService dataSeederService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @PostMapping("/login")
    public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        LoginResponse response = authService.login(request);
        return ResponseEntity.ok(response);
    }

    @PostMapping("/logout")
    public ResponseEntity<?> logout() {
        // JWT is stateless, so logout is handled client-side by removing the token
        return ResponseEntity.ok().build();
    }

    @GetMapping("/seed-status")
    public ResponseEntity<Map<String, Object>> getSeedStatus() {
        Map<String, Object> response = new HashMap<>();
        long userCount = userRepository.count();
        response.put("userCount", userCount);
        response.put("usersExist", userCount > 0);
        response.put("EMP004_exists", userRepository.findByEmployeeId("EMP004").isPresent());
        return ResponseEntity.ok(response);
    }

    @PostMapping("/seed-users")
    public ResponseEntity<Map<String, Object>> seedUsers() {
        Map<String, Object> response = new HashMap<>();
        try {
            long beforeCount = userRepository.count();
            // Manually trigger user seeding
            dataSeederService.seedUsersOnly();
            long afterCount = userRepository.count();
            
            response.put("success", true);
            response.put("message", "Users seeded successfully");
            response.put("usersCreated", afterCount - beforeCount);
            response.put("totalUsers", afterCount);
        } catch (Exception e) {
            response.put("success", false);
            response.put("message", "Failed to seed users: " + e.getMessage());
            response.put("error", e.getClass().getSimpleName());
            response.put("errorDetails", e.getMessage());
        }
        return ResponseEntity.ok(response);
    }

    @GetMapping("/generate-hash")
    public ResponseEntity<Map<String, Object>> generateHash(@RequestParam String password) {
        Map<String, Object> response = new HashMap<>();
        try {
            String hash = passwordEncoder.encode(password);
            response.put("password", password);
            response.put("hash", hash);
            response.put("sqlFormat", String.format("'%s'", hash));
            response.put("message", "Hash generated successfully");
        } catch (Exception e) {
            response.put("error", e.getMessage());
            response.put("errorType", e.getClass().getSimpleName());
            return ResponseEntity.status(500).body(response);
        }
        return ResponseEntity.ok(response);
    }
}
