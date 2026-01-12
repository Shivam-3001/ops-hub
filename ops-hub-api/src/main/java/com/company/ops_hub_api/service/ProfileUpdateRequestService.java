package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.ProfileUpdateRequest;
import com.company.ops_hub_api.domain.User;
import com.company.ops_hub_api.domain.UserProfile;
import com.company.ops_hub_api.dto.ReviewProfileUpdateRequestDTO;
import com.company.ops_hub_api.dto.SubmitProfileUpdateRequestDTO;
import com.company.ops_hub_api.repository.ProfileUpdateRequestRepository;
import com.company.ops_hub_api.repository.UserProfileRepository;
import com.company.ops_hub_api.repository.UserRepository;
import com.company.ops_hub_api.security.UserPrincipal;
import com.company.ops_hub_api.util.EncryptionUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class ProfileUpdateRequestService {

    private final ProfileUpdateRequestRepository requestRepository;
    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final EncryptionUtil encryptionUtil;
    private final AuditLogService auditLogService;
    private final EmailNotificationService emailNotificationService;
    private final ObjectMapper objectMapper;

    /**
     * Submit a profile update request
     */
    @Transactional
    public ProfileUpdateRequest submitRequest(SubmitProfileUpdateRequestDTO dto, HttpServletRequest httpRequest) {
        // Get current user
        User currentUser = getCurrentUser();
        Long userId = currentUser.getId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        
        // Get or create user profile
        UserProfile profile = profileRepository.findByUserId(userId).orElse(null);

        // Create request
        ProfileUpdateRequest request = new ProfileUpdateRequest();
        request.setUser(currentUser);
        request.setProfile(profile);
        request.setRequestType(dto.getRequestType());
        request.setRequestedBy(currentUser);
        request.setStatus("PENDING");
        request.setReason(dto.getReason());
        request.setRequestedAt(LocalDateTime.now());

        // Store field changes
        if ("CREATE".equals(dto.getRequestType())) {
            // For CREATE, store all new values
            request.setNewValueEncrypted(encryptProfileData(dto));
        } else if ("UPDATE".equals(dto.getRequestType())) {
            // For UPDATE, store old and new values
            if (profile != null) {
                request.setOldValueEncrypted(encryptProfileData(profile));
                request.setNewValueEncrypted(encryptProfileData(dto));
                
                // If specific field update
                if (dto.getFieldName() != null) {
                    request.setFieldName(dto.getFieldName());
                }
            }
        }

        ProfileUpdateRequest savedRequest = requestRepository.save(request);

        // Log audit
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("requestId", savedRequest.getId());
        newValues.put("requestType", dto.getRequestType());
        newValues.put("status", "PENDING");
        Long requestId = savedRequest.getId();
        if (requestId != null) {
            auditLogService.logAction("CREATE", "PROFILE_UPDATE_REQUEST", requestId, 
                    null, newValues, httpRequest);
        }

        // Send notification
        emailNotificationService.sendProfileUpdateSubmittedNotification(
                currentUser.getEmail(), 
                currentUser.getFullName() != null ? currentUser.getFullName() : currentUser.getUsername(),
                savedRequest.getId()
        );

        log.info("Profile update request {} submitted by user {}", savedRequest.getId(), currentUser.getEmployeeId());
        return savedRequest;
    }

    /**
     * Get all pending requests (requires APPROVE_PROFILE permission)
     */
    @Transactional(readOnly = true)
    public List<ProfileUpdateRequest> getPendingRequests() {
        checkApprovalPermission();
        return requestRepository.findPendingRequests("PENDING");
    }

    /**
     * Get requests for current user
     */
    @Transactional(readOnly = true)
    public List<ProfileUpdateRequest> getMyRequests() {
        User currentUser = getCurrentUser();
        return requestRepository.findByUserId(currentUser.getId());
    }

    /**
     * Approve a profile update request
     */
    @Transactional
    public ProfileUpdateRequest approveRequest(ReviewProfileUpdateRequestDTO dto, HttpServletRequest httpRequest) {
        checkApprovalPermission();
        
        Long requestId = dto.getRequestId();
        if (requestId == null) {
            throw new IllegalArgumentException("Request ID cannot be null");
        }
        ProfileUpdateRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Profile update request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Only PENDING requests can be approved");
        }

        User reviewer = getCurrentUser();
        Long reviewerId = reviewer.getId();
        if (reviewerId == null) {
            throw new IllegalStateException("Reviewer ID cannot be null");
        }
        
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setStatus("APPROVED");
        request.setReviewNotes(dto.getReviewNotes());

        // Update user profile
        UserProfile profile = request.getProfile();
        if (profile == null) {
            // Create new profile
            profile = new UserProfile();
            profile.setUser(request.getUser());
            profile.setApprovedBy(reviewer);
            profile.setApprovedAt(LocalDateTime.now());
        } else {
            profile.setApprovedBy(reviewer);
            profile.setApprovedAt(LocalDateTime.now());
        }

        // Apply changes from request
        if (request.getNewValueEncrypted() != null) {
            applyProfileChanges(profile, request.getNewValueEncrypted());
        }

        profileRepository.save(profile);
        request.setProfile(profile);
        ProfileUpdateRequest savedRequest = requestRepository.save(request);

        // Log audit
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("status", "PENDING");
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("status", "APPROVED");
        newValues.put("reviewedBy", reviewer.getEmployeeId());
        newValues.put("reviewNotes", dto.getReviewNotes());
        Long savedRequestId = savedRequest.getId();
        if (savedRequestId != null) {
            auditLogService.logAction("APPROVE", "PROFILE_UPDATE_REQUEST", savedRequestId, 
                    oldValues, newValues, httpRequest);
        }

        // Send notification
        emailNotificationService.sendProfileUpdateApprovedNotification(
                request.getUser().getEmail(),
                request.getUser().getFullName() != null ? request.getUser().getFullName() : request.getUser().getUsername(),
                savedRequest.getId()
        );

        log.info("Profile update request {} approved by {}", savedRequest.getId(), reviewer.getEmployeeId());
        return savedRequest;
    }

    /**
     * Reject a profile update request
     */
    @Transactional
    public ProfileUpdateRequest rejectRequest(ReviewProfileUpdateRequestDTO dto, HttpServletRequest httpRequest) {
        checkApprovalPermission();
        
        Long requestId = dto.getRequestId();
        if (requestId == null) {
            throw new IllegalArgumentException("Request ID cannot be null");
        }
        ProfileUpdateRequest request = requestRepository.findById(requestId)
                .orElseThrow(() -> new IllegalArgumentException("Profile update request not found"));

        if (!"PENDING".equals(request.getStatus())) {
            throw new IllegalStateException("Only PENDING requests can be rejected");
        }

        User reviewer = getCurrentUser();
        request.setReviewedBy(reviewer);
        request.setReviewedAt(LocalDateTime.now());
        request.setStatus("REJECTED");
        request.setReviewNotes(dto.getReviewNotes());

        ProfileUpdateRequest savedRequest = requestRepository.save(request);

        // Log audit
        Map<String, Object> oldValues = new HashMap<>();
        oldValues.put("status", "PENDING");
        Map<String, Object> newValues = new HashMap<>();
        newValues.put("status", "REJECTED");
        newValues.put("reviewedBy", reviewer.getEmployeeId());
        newValues.put("reviewNotes", dto.getReviewNotes());
        auditLogService.logAction("REJECT", "PROFILE_UPDATE_REQUEST", savedRequest.getId(), 
                oldValues, newValues, httpRequest);

        // Send notification
        emailNotificationService.sendProfileUpdateRejectedNotification(
                request.getUser().getEmail(),
                request.getUser().getFullName() != null ? request.getUser().getFullName() : request.getUser().getUsername(),
                savedRequest.getId(),
                dto.getReviewNotes()
        );

        log.info("Profile update request {} rejected by {}", savedRequest.getId(), reviewer.getEmployeeId());
        return savedRequest;
    }

    private User getCurrentUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        Long userId = userPrincipal.getUserId();
        if (userId == null) {
            throw new IllegalStateException("User ID cannot be null");
        }
        return userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found"));
    }

    private void checkApprovalPermission() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof UserPrincipal)) {
            throw new AccessDeniedException("User not authenticated");
        }
        UserPrincipal userPrincipal = (UserPrincipal) authentication.getPrincipal();
        if (!userPrincipal.hasPermission("APPROVE_PROFILE")) {
            throw new AccessDeniedException("Insufficient permissions to approve profile updates");
        }
    }

    private String encryptProfileData(SubmitProfileUpdateRequestDTO dto) {
        try {
            Map<String, Object> data = new HashMap<>();
            if (dto.getFirstName() != null) data.put("firstName", dto.getFirstName());
            if (dto.getLastName() != null) data.put("lastName", dto.getLastName());
            if (dto.getMiddleName() != null) data.put("middleName", dto.getMiddleName());
            if (dto.getDateOfBirth() != null) data.put("dateOfBirth", dto.getDateOfBirth().toString());
            if (dto.getGender() != null) data.put("gender", dto.getGender());
            if (dto.getPhone() != null) data.put("phone", encryptionUtil.encrypt(dto.getPhone()));
            if (dto.getAlternatePhone() != null) data.put("alternatePhone", encryptionUtil.encrypt(dto.getAlternatePhone()));
            if (dto.getEmail() != null) data.put("email", encryptionUtil.encrypt(dto.getEmail()));
            if (dto.getAddressLine1() != null) data.put("addressLine1", dto.getAddressLine1());
            if (dto.getAddressLine2() != null) data.put("addressLine2", dto.getAddressLine2());
            if (dto.getCity() != null) data.put("city", dto.getCity());
            if (dto.getState() != null) data.put("state", dto.getState());
            if (dto.getPostalCode() != null) data.put("postalCode", dto.getPostalCode());
            if (dto.getCountry() != null) data.put("country", dto.getCountry());
            if (dto.getProfilePictureUrl() != null) data.put("profilePictureUrl", dto.getProfilePictureUrl());
            if (dto.getEmergencyContactName() != null) data.put("emergencyContactName", dto.getEmergencyContactName());
            if (dto.getEmergencyContactPhone() != null) data.put("emergencyContactPhone", encryptionUtil.encrypt(dto.getEmergencyContactPhone()));
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Error encrypting profile data", e);
            throw new RuntimeException("Error processing profile data", e);
        }
    }

    private String encryptProfileData(UserProfile profile) {
        try {
            Map<String, Object> data = new HashMap<>();
            if (profile.getFirstName() != null) data.put("firstName", profile.getFirstName());
            if (profile.getLastName() != null) data.put("lastName", profile.getLastName());
            if (profile.getMiddleName() != null) data.put("middleName", profile.getMiddleName());
            if (profile.getDateOfBirth() != null) data.put("dateOfBirth", profile.getDateOfBirth().toString());
            if (profile.getGender() != null) data.put("gender", profile.getGender());
            if (profile.getPhoneEncrypted() != null) data.put("phone", profile.getPhoneEncrypted());
            if (profile.getAlternatePhoneEncrypted() != null) data.put("alternatePhone", profile.getAlternatePhoneEncrypted());
            if (profile.getEmailEncrypted() != null) data.put("email", profile.getEmailEncrypted());
            if (profile.getAddressLine1() != null) data.put("addressLine1", profile.getAddressLine1());
            if (profile.getAddressLine2() != null) data.put("addressLine2", profile.getAddressLine2());
            if (profile.getCity() != null) data.put("city", profile.getCity());
            if (profile.getState() != null) data.put("state", profile.getState());
            if (profile.getPostalCode() != null) data.put("postalCode", profile.getPostalCode());
            if (profile.getCountry() != null) data.put("country", profile.getCountry());
            if (profile.getProfilePictureUrl() != null) data.put("profilePictureUrl", profile.getProfilePictureUrl());
            if (profile.getEmergencyContactName() != null) data.put("emergencyContactName", profile.getEmergencyContactName());
            if (profile.getEmergencyContactPhoneEncrypted() != null) data.put("emergencyContactPhone", profile.getEmergencyContactPhoneEncrypted());
            return objectMapper.writeValueAsString(data);
        } catch (Exception e) {
            log.error("Error serializing profile data", e);
            throw new RuntimeException("Error processing profile data", e);
        }
    }

    @SuppressWarnings("unchecked")
    private void applyProfileChanges(UserProfile profile, String encryptedData) {
        try {
            Map<String, Object> data = (Map<String, Object>) objectMapper.readValue(encryptedData, Map.class);
            
            if (data.containsKey("firstName")) profile.setFirstName((String) data.get("firstName"));
            if (data.containsKey("lastName")) profile.setLastName((String) data.get("lastName"));
            if (data.containsKey("middleName")) profile.setMiddleName((String) data.get("middleName"));
            if (data.containsKey("dateOfBirth")) {
                String dobStr = (String) data.get("dateOfBirth");
                if (dobStr != null) {
                    profile.setDateOfBirth(java.time.LocalDate.parse(dobStr));
                }
            }
            if (data.containsKey("gender")) profile.setGender((String) data.get("gender"));
            if (data.containsKey("phone")) profile.setPhoneEncrypted((String) data.get("phone"));
            if (data.containsKey("alternatePhone")) profile.setAlternatePhoneEncrypted((String) data.get("alternatePhone"));
            if (data.containsKey("email")) profile.setEmailEncrypted((String) data.get("email"));
            if (data.containsKey("addressLine1")) profile.setAddressLine1((String) data.get("addressLine1"));
            if (data.containsKey("addressLine2")) profile.setAddressLine2((String) data.get("addressLine2"));
            if (data.containsKey("city")) profile.setCity((String) data.get("city"));
            if (data.containsKey("state")) profile.setState((String) data.get("state"));
            if (data.containsKey("postalCode")) profile.setPostalCode((String) data.get("postalCode"));
            if (data.containsKey("country")) profile.setCountry((String) data.get("country"));
            if (data.containsKey("profilePictureUrl")) profile.setProfilePictureUrl((String) data.get("profilePictureUrl"));
            if (data.containsKey("emergencyContactName")) profile.setEmergencyContactName((String) data.get("emergencyContactName"));
            if (data.containsKey("emergencyContactPhone")) profile.setEmergencyContactPhoneEncrypted((String) data.get("emergencyContactPhone"));
        } catch (Exception e) {
            log.error("Error applying profile changes", e);
            throw new RuntimeException("Error applying profile changes", e);
        }
    }
}
