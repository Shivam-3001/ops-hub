package com.company.ops_hub_api.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SubmitProfileUpdateRequestDTO {
    
    @NotNull(message = "Request type is required")
    @NotBlank(message = "Request type cannot be blank")
    private String requestType; // CREATE, UPDATE, DELETE
    
    private String fieldName; // For UPDATE requests
    
    private String firstName;
    private String lastName;
    private String middleName;
    private LocalDate dateOfBirth;
    private String gender;
    private String phone;
    private String alternatePhone;
    private String email;
    private String addressLine1;
    private String addressLine2;
    private String city;
    private String state;
    private String postalCode;
    private String country;
    private String profilePictureUrl;
    private String emergencyContactName;
    private String emergencyContactPhone;
    
    @NotBlank(message = "Reason is required")
    private String reason;
}
