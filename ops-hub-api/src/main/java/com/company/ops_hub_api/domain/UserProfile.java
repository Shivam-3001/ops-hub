package com.company.ops_hub_api.domain;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "user_profiles")
@EntityListeners(AuditingEntityListener.class)
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(length = 100, name = "first_name")
    private String firstName;

    @Column(length = 100, name = "last_name")
    private String lastName;

    @Column(length = 100, name = "middle_name")
    private String middleName;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    @Column(length = 20)
    private String gender;

    @Column(length = 500, name = "phone_encrypted")
    private String phoneEncrypted;

    @Column(length = 500, name = "alternate_phone_encrypted")
    private String alternatePhoneEncrypted;

    @Column(length = 500, name = "email_encrypted")
    private String emailEncrypted;

    @Column(length = 500, name = "address_line1")
    private String addressLine1;

    @Column(length = 500, name = "address_line2")
    private String addressLine2;

    @Column(length = 100)
    private String city;

    @Column(length = 100)
    private String state;

    @Column(length = 20, name = "postal_code")
    private String postalCode;

    @Column(length = 100)
    private String country;

    @Column(length = 500, name = "profile_picture_url")
    private String profilePictureUrl;

    @Column(length = 200, name = "emergency_contact_name")
    private String emergencyContactName;

    @Column(length = 500, name = "emergency_contact_phone_encrypted")
    private String emergencyContactPhoneEncrypted;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "approved_by")
    private User approvedBy;

    @Column(name = "approved_at")
    private LocalDateTime approvedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false, name = "created_at")
    private LocalDateTime createdAt;

    @LastModifiedDate
    @Column(nullable = false, name = "updated_at")
    private LocalDateTime updatedAt;
}
