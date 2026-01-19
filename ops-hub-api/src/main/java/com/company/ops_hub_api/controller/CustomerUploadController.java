package com.company.ops_hub_api.controller;

import com.company.ops_hub_api.dto.CustomerUploadHistoryDTO;
import com.company.ops_hub_api.dto.CustomerUploadPreviewResponseDTO;
import com.company.ops_hub_api.dto.CustomerUploadResultDTO;
import com.company.ops_hub_api.security.RequiresPermission;
import com.company.ops_hub_api.service.CustomerUploadService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/customer-uploads")
@RequiredArgsConstructor
public class CustomerUploadController {

    private final CustomerUploadService customerUploadService;

    @PostMapping("/preview")
    @RequiresPermission("MANAGE_CUSTOMERS")
    public ResponseEntity<CustomerUploadPreviewResponseDTO> previewUpload(
            @RequestParam("file") MultipartFile file) {
        return ResponseEntity.ok(customerUploadService.preview(file));
    }

    @PostMapping
    @RequiresPermission("MANAGE_CUSTOMERS")
    public ResponseEntity<CustomerUploadResultDTO> uploadCustomers(
            @RequestParam("file") MultipartFile file,
            HttpServletRequest request) {
        return ResponseEntity.ok(customerUploadService.upload(file, request));
    }

    @GetMapping("/history")
    @RequiresPermission("MANAGE_CUSTOMERS")
    public ResponseEntity<List<CustomerUploadHistoryDTO>> getUploadHistory() {
        return ResponseEntity.ok(customerUploadService.getMyUploads());
    }

    @GetMapping("/{uploadId}")
    @RequiresPermission("MANAGE_CUSTOMERS")
    public ResponseEntity<CustomerUploadResultDTO> getUploadResult(@PathVariable Long uploadId) {
        return ResponseEntity.ok(customerUploadService.getUploadResult(uploadId));
    }
}
