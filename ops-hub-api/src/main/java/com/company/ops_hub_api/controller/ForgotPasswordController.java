package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.*;
import com.company.ops_hub_api.service.ForgotPasswordService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/auth/forgot-password")
@RequiredArgsConstructor
public class ForgotPasswordController {

    private final ForgotPasswordService forgotPasswordService;

    @PostMapping("/lookup")
    public ResponseEntity<ForgotPasswordLookupResponseDTO> lookup(
            @Valid @RequestBody ForgotPasswordLookupRequestDTO request) {
        return ResponseEntity.ok(forgotPasswordService.lookupUser(request));
    }

    @PostMapping("/send-otp")
    public ResponseEntity<Map<String, Object>> sendOtp(
            @Valid @RequestBody ForgotPasswordSendOtpRequestDTO request) {
        return ResponseEntity.ok(forgotPasswordService.sendOtp(request));
    }

    @PostMapping("/verify-otp")
    public ResponseEntity<Map<String, Object>> verifyOtp(
            @Valid @RequestBody ForgotPasswordVerifyOtpRequestDTO request) {
        return ResponseEntity.ok(forgotPasswordService.verifyOtp(request));
    }

    @PostMapping("/reset")
    public ResponseEntity<Map<String, Object>> resetPassword(
            @Valid @RequestBody ForgotPasswordResetRequestDTO request) {
        return ResponseEntity.ok(forgotPasswordService.resetPassword(request));
    }
}
