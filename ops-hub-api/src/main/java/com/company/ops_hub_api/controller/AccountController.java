package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.ChangePasswordRequestDTO;
import com.company.ops_hub_api.service.AccountService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/account")
@RequiredArgsConstructor
public class AccountController {

    private final AccountService accountService;

    @PostMapping("/change-password")
    public ResponseEntity<Map<String, Object>> changePassword(
            @Valid @RequestBody ChangePasswordRequestDTO request) {
        accountService.changePassword(request);
        return ResponseEntity.ok(Map.of(
                "success", true,
                "message", "Password updated successfully"
        ));
    }
}
