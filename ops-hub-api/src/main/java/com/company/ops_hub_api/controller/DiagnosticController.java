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
@RequestMapping("/diagnostic")
@RequiredArgsConstructor
public class DiagnosticController {

    private final UserRepository userRepository;

    @GetMapping("/users")
    public ResponseEntity<Map<String, Object>> checkUsers() {
        Map<String, Object> response = new HashMap<>();
        
        long userCount = userRepository.count();
        response.put("totalUsers", userCount);
        
        // Check for test users
        response.put("EMP004_exists", userRepository.findByEmployeeId("EMP004").isPresent());
        response.put("EMP001_exists", userRepository.findByEmployeeId("EMP001").isPresent());
        response.put("EMP005_exists", userRepository.findByEmployeeId("EMP005").isPresent());
        
        if (userRepository.findByEmployeeId("EMP004").isPresent()) {
            var user = userRepository.findByEmployeeId("EMP004").get();
            Map<String, Object> userInfo = new HashMap<>();
            userInfo.put("employeeId", user.getEmployeeId());
            userInfo.put("username", user.getUsername());
            userInfo.put("active", user.getActive());
            userInfo.put("userType", user.getUserType());
            userInfo.put("hasPasswordHash", user.getPasswordHash() != null && !user.getPasswordHash().isEmpty());
            response.put("EMP004_details", userInfo);
        }
        
        return ResponseEntity.ok(response);
    }
}
