package com.company.ops_hub_api.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ForgotPasswordVerifyOtpRequestDTO {

    @NotBlank(message = "Username is required")
    private String username;

    @NotBlank(message = "Channel is required")
    private String channel;

    @NotBlank(message = "OTP is required")
    private String otp;
}
