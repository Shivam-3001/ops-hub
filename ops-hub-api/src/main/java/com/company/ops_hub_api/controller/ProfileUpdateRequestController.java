package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.domain.ProfileUpdateRequest;
import com.company.ops_hub_api.dto.ProfileUpdateRequestDTO;
import com.company.ops_hub_api.dto.ReviewProfileUpdateRequestDTO;
import com.company.ops_hub_api.dto.SubmitProfileUpdateRequestDTO;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.service.ProfileUpdateRequestService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/profile-update-requests")
@RequiredArgsConstructor
public class ProfileUpdateRequestController {

    private final ProfileUpdateRequestService requestService;

    /**
     * Submit a new profile update request
     */
    @PostMapping
    public ResponseEntity<ProfileUpdateRequestDTO> submitRequest(
            @Valid @RequestBody SubmitProfileUpdateRequestDTO dto,
            HttpServletRequest httpRequest) {
        ProfileUpdateRequest request = requestService.submitRequest(dto, httpRequest);
        return ResponseEntity.ok(toDTO(request));
    }

    /**
     * Get all pending requests (requires APPROVE_PROFILE permission)
     */
    @GetMapping("/pending")
    @RequiresPermission("APPROVE_PROFILE")
    public ResponseEntity<List<ProfileUpdateRequestDTO>> getPendingRequests() {
        List<ProfileUpdateRequest> requests = requestService.getPendingRequests();
        return ResponseEntity.ok(requests.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get current user's requests
     */
    @GetMapping("/my-requests")
    public ResponseEntity<List<ProfileUpdateRequestDTO>> getMyRequests() {
        List<ProfileUpdateRequest> requests = requestService.getMyRequests();
        return ResponseEntity.ok(requests.stream()
                .map(this::toDTO)
                .collect(Collectors.toList()));
    }

    /**
     * Get a specific request by ID
     */
    @GetMapping("/{id}")
    public ResponseEntity<ProfileUpdateRequestDTO> getRequest(@PathVariable Long id) {
        // Users can only view their own requests unless they have approval permission
        // This logic should be in the service, but for simplicity, we'll check here
        ProfileUpdateRequest request = requestService.getMyRequests().stream()
                .filter(r -> r.getId().equals(id))
                .findFirst()
                .orElse(null);
        
        if (request == null) {
            // Check if user has approval permission and request exists in pending
            List<ProfileUpdateRequest> pending = requestService.getPendingRequests();
            request = pending.stream()
                    .filter(r -> r.getId().equals(id))
                    .findFirst()
                    .orElse(null);
        }
        
        if (request == null) {
            return ResponseEntity.notFound().build();
        }
        
        return ResponseEntity.ok(toDTO(request));
    }

    /**
     * Approve a profile update request (requires APPROVE_PROFILE permission)
     */
    @PostMapping("/approve")
    @RequiresPermission("APPROVE_PROFILE")
    public ResponseEntity<ProfileUpdateRequestDTO> approveRequest(
            @Valid @RequestBody ReviewProfileUpdateRequestDTO dto,
            HttpServletRequest httpRequest) {
        ProfileUpdateRequest request = requestService.approveRequest(dto, httpRequest);
        return ResponseEntity.ok(toDTO(request));
    }

    /**
     * Reject a profile update request (requires APPROVE_PROFILE permission)
     */
    @PostMapping("/reject")
    @RequiresPermission("APPROVE_PROFILE")
    public ResponseEntity<ProfileUpdateRequestDTO> rejectRequest(
            @Valid @RequestBody ReviewProfileUpdateRequestDTO dto,
            HttpServletRequest httpRequest) {
        ProfileUpdateRequest request = requestService.rejectRequest(dto, httpRequest);
        return ResponseEntity.ok(toDTO(request));
    }

    private ProfileUpdateRequestDTO toDTO(ProfileUpdateRequest request) {
        return ProfileUpdateRequestDTO.builder()
                .id(request.getId())
                .userId(request.getUser() != null ? request.getUser().getId() : null)
                .userEmployeeId(request.getUser() != null ? request.getUser().getEmployeeId() : null)
                .userName(request.getUser() != null ? 
                        (request.getUser().getFullName() != null ? request.getUser().getFullName() : request.getUser().getUsername()) : null)
                .profileId(request.getProfile() != null ? request.getProfile().getId() : null)
                .requestType(request.getRequestType())
                .fieldName(request.getFieldName())
                .oldValue(request.getOldValueEncrypted()) // Note: In production, decrypt if needed
                .newValue(request.getNewValueEncrypted()) // Note: In production, decrypt if needed
                .reason(request.getReason())
                .status(request.getStatus())
                .requestedBy(request.getRequestedBy() != null ? request.getRequestedBy().getId() : null)
                .requestedByEmployeeId(request.getRequestedBy() != null ? request.getRequestedBy().getEmployeeId() : null)
                .requestedAt(request.getRequestedAt())
                .reviewedBy(request.getReviewedBy() != null ? request.getReviewedBy().getId() : null)
                .reviewedByEmployeeId(request.getReviewedBy() != null ? request.getReviewedBy().getEmployeeId() : null)
                .reviewedAt(request.getReviewedAt())
                .reviewNotes(request.getReviewNotes())
                .build();
    }
}
