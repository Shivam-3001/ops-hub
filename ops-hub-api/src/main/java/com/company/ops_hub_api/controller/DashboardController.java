package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;

    @GetMapping
    public ResponseEntity<Map<String, Object>> getDashboard() {
        Map<String, Object> response = new HashMap<>();
        response.put("message", "Welcome to Ops Hub Dashboard!");
        response.put("status", "operational");
        
        // Test database connectivity
        try {
            long userCount = userRepository.count();
            response.put("database", "connected");
            response.put("userCount", userCount);
        } catch (Exception e) {
            response.put("database", "disconnected");
            response.put("error", e.getClass().getSimpleName() + ": " + e.getMessage());
            response.put("hint", "Please check database connection settings and ensure 'ops_hub' database exists");
        }
        
        return ResponseEntity.ok(response);
    }
}
