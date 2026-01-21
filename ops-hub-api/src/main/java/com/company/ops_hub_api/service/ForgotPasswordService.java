package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.dto.*;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.util.EncryptionUtil;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class ForgotPasswordService {

    private static final String PASSWORD_REGEX =
            "^(?=.*[a-z])(?=.*[A-Z])(?=.*\\d)(?=.*[^A-Za-z\\d]).{8,}$";
    private static final int OTP_LENGTH = 6;
    private static final Duration OTP_TTL = Duration.ofMinutes(3);
    private static final Duration RESET_TOKEN_TTL = Duration.ofMinutes(10);

    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final PasswordEncoder passwordEncoder;
    private final EmailService emailService;
    private final AuditLogService auditLogService;

    private final SecureRandom secureRandom = new SecureRandom();
    private final Map<String, OtpEntry> otpStore = new ConcurrentHashMap<>();
    private final Map<String, ResetEntry> resetStore = new ConcurrentHashMap<>();

    public ForgotPasswordLookupResponseDTO lookupUser(ForgotPasswordLookupRequestDTO request) {
        User user = findUser(request.getUsername());
        ensureActive(user);

        List<ForgotPasswordChannelDTO> channels = new ArrayList<>();
        if (user.getEmail() != null && !user.getEmail().isBlank()) {
            String email = decryptSafe(user.getEmail());
            channels.add(ForgotPasswordChannelDTO.builder()
                    .type("EMAIL")
                    .maskedValue(maskEmail(email))
                    .build());
        }
        if (user.getPhone() != null && !user.getPhone().isBlank()) {
            String phone = decryptSafe(user.getPhone());
            channels.add(ForgotPasswordChannelDTO.builder()
                    .type("MOBILE")
                    .maskedValue(maskPhone(phone))
                    .build());
        }

        if (channels.isEmpty()) {
            throw new IllegalArgumentException("No recovery channels found for this user");
        }

        return ForgotPasswordLookupResponseDTO.builder()
                .username(user.getUsername())
                .channels(channels)
                .build();
    }

    public Map<String, Object> sendOtp(ForgotPasswordSendOtpRequestDTO request) {
        User user = findUser(request.getUsername());
        ensureActive(user);

        String channel = normalizeChannel(request.getChannel());
        String providedValue = request.getValue() != null ? request.getValue().trim() : "";

        if ("EMAIL".equals(channel)) {
            String email = decryptSafe(user.getEmail());
            if (!email.equalsIgnoreCase(providedValue)) {
                throw new IllegalArgumentException("Email does not match our records");
            }
            String otp = generateOtp();
            storeOtp(user.getUsername(), channel, otp);
            sendEmailOtp(email, user, otp);
        } else if ("MOBILE".equals(channel)) {
            String phone = decryptSafe(user.getPhone());
            if (!phone.equals(providedValue)) {
                throw new IllegalArgumentException("Mobile number does not match our records");
            }
            String otp = generateOtp();
            storeOtp(user.getUsername(), channel, otp);
            sendSmsOtp(phone, user, otp);
        } else {
            throw new IllegalArgumentException("Invalid channel selected");
        }

        return Map.of(
                "success", true,
                "expiresInSeconds", OTP_TTL.toSeconds()
        );
    }

    public Map<String, Object> verifyOtp(ForgotPasswordVerifyOtpRequestDTO request) {
        String channel = normalizeChannel(request.getChannel());
        String key = buildKey(request.getUsername(), channel);
        OtpEntry entry = otpStore.get(key);
        if (entry == null || entry.isExpired()) {
            otpStore.remove(key);
            throw new IllegalArgumentException("OTP has expired or is invalid");
        }
        if (!entry.getOtp().equals(request.getOtp())) {
            throw new IllegalArgumentException("Invalid OTP");
        }
        otpStore.remove(key);

        String resetToken = UUID.randomUUID().toString();
        resetStore.put(buildResetKey(request.getUsername()), new ResetEntry(resetToken));

        return Map.of(
                "success", true,
                "resetToken", resetToken,
                "expiresInSeconds", RESET_TOKEN_TTL.toSeconds()
        );
    }

    public Map<String, Object> resetPassword(ForgotPasswordResetRequestDTO request) {
        User user = findUser(request.getUsername());
        ensureActive(user);

        if (!request.getNewPassword().equals(request.getConfirmPassword())) {
            throw new IllegalArgumentException("New password and confirm password do not match");
        }
        if (!request.getNewPassword().matches(PASSWORD_REGEX)) {
            throw new IllegalArgumentException(
                    "Password must be at least 8 characters and include uppercase, lowercase, number, and special character"
            );
        }

        ResetEntry entry = resetStore.get(buildResetKey(request.getUsername()));
        if (entry == null || entry.isExpired()) {
            resetStore.remove(buildResetKey(request.getUsername()));
            throw new IllegalArgumentException("Reset session has expired. Please request OTP again.");
        }
        if (!entry.getToken().equals(request.getResetToken())) {
            throw new IllegalArgumentException("Invalid reset token");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
        resetStore.remove(buildResetKey(request.getUsername()));

        auditLogService.logAction(
                "FORGOT_PASSWORD_RESET",
                "USER",
                user.getId(),
                null,
                Map.of("changed", true)
        );

        return Map.of(
                "success", true,
                "message", "Password updated successfully"
        );
    }

    private User findUser(String username) {
        String normalized = username != null ? username.trim() : "";
        return userRepository.findByUsername(normalized)
                .or(() -> userRepository.findByEmployeeId(normalized))
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void ensureActive(User user) {
        if (user == null || !Boolean.TRUE.equals(user.getActive())) {
            throw new IllegalArgumentException("User is inactive or not found");
        }
    }

    private String normalizeChannel(String channel) {
        return channel != null ? channel.trim().toUpperCase() : "";
    }

    private String generateOtp() {
        int max = (int) Math.pow(10, OTP_LENGTH) - 1;
        int min = (int) Math.pow(10, OTP_LENGTH - 1);
        int value = secureRandom.nextInt(max - min + 1) + min;
        return String.valueOf(value);
    }

    private void storeOtp(String username, String channel, String otp) {
        otpStore.put(buildKey(username, channel), new OtpEntry(otp));
        resetStore.remove(buildResetKey(username));
    }

    private String buildKey(String username, String channel) {
        return (username != null ? username.trim().toLowerCase() : "") + ":" + channel;
    }

    private String buildResetKey(String username) {
        String base = username != null ? username.trim().toLowerCase() : "";
        return base + ":reset";
    }

    private void sendEmailOtp(String email, User user, String otp) {
        String name = user.getFullName() != null ? user.getFullName() : user.getUsername();
        String subject = "Ops Hub OTP Verification";
        String bodyText = String.format("Your OTP is %s. It is valid for 3 minutes.", otp);
        emailService.sendEmailDirect(email, name, subject, null, bodyText, null);
    }

    private void sendSmsOtp(String phone, User user, String otp) {
        String masked = maskPhone(phone);
        log.info("Sending OTP to mobile {} for user {}. OTP: {}", masked, user.getUsername(), otp);
    }

    private String decryptSafe(String value) {
        if (value == null || value.isBlank()) {
            return value;
        }
        try {
            return encryptionUtil.decrypt(value);
        } catch (RuntimeException ex) {
            return value;
        }
    }

    private String maskEmail(String email) {
        if (email == null || !email.contains("@")) {
            return "****";
        }
        String[] parts = email.split("@", 2);
        String name = parts[0];
        String domain = parts[1];
        String prefix = name.length() <= 2 ? name.substring(0, 1) : name.substring(0, 2);
        return prefix + "****@" + domain;
    }

    private String maskPhone(String phone) {
        if (phone == null || phone.length() < 4) {
            return "****";
        }
        return "******" + phone.substring(phone.length() - 4);
    }

    @Data
    @AllArgsConstructor
    private static class OtpEntry {
        private String otp;
        private LocalDateTime expiresAt;

        OtpEntry(String otp) {
            this.otp = otp;
            this.expiresAt = LocalDateTime.now().plus(OTP_TTL);
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }

    @Data
    @AllArgsConstructor
    private static class ResetEntry {
        private String token;
        private LocalDateTime expiresAt;

        ResetEntry(String token) {
            this.token = token;
            this.expiresAt = LocalDateTime.now().plus(RESET_TOKEN_TTL);
        }

        boolean isExpired() {
            return LocalDateTime.now().isAfter(expiresAt);
        }
    }
}
