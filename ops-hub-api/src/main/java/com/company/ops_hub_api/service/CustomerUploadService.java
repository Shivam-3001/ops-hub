package com.company.ops_hub_api.service;

import com.company.ops_hub_api.domain.*;
import com.company.ops_hub_api.dto.*;
import com.company.ops_hub_api.repository.*;
import com.company.ops_hub_api.security.UserPrincipal;
import com.company.ops_hub_api.util.EncryptionUtil;
import com.company.ops_hub_api.util.HierarchyUtil;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class CustomerUploadService {

    private final CustomerUploadRepository uploadRepository;
    private final CustomerUploadErrorRepository uploadErrorRepository;
    private final CustomerRepository customerRepository;
    private final CustomerAllocationRepository allocationRepository;
    private final ClusterRepository clusterRepository;
    private final CircleRepository circleRepository;
    private final ZoneRepository zoneRepository;
    private final AreaRepository areaRepository;
    private final UserRepository userRepository;
    private final AuditLogService auditLogService;
    private final NotificationService notificationService;
    private final EncryptionUtil encryptionUtil;

    private static final List<String> REQUIRED_HEADERS = List.of(
            "customer_name", "phone", "email", "pending_amount",
            "cluster", "circle", "zone", "area"
    );

    @Transactional(readOnly = true)
    public CustomerUploadPreviewResponseDTO preview(MultipartFile file) {
        validateUploadPermission();
        List<CustomerUploadRowDTO> rows = parseFile(file);
        applyDuplicateValidation(rows);
        int validRows = (int) rows.stream().filter(CustomerUploadRowDTO::isValid).count();
        int invalidRows = rows.size() - validRows;
        List<String> errors = new ArrayList<>();
        if (rows.isEmpty()) {
            errors.add("No data rows found in upload.");
        }
        return CustomerUploadPreviewResponseDTO.builder()
                .fileName(file != null ? file.getOriginalFilename() : null)
                .totalRows(rows.size())
                .validRows(validRows)
                .invalidRows(invalidRows)
                .rows(rows)
                .errors(errors)
                .build();
    }

    @Transactional
    public CustomerUploadResultDTO upload(MultipartFile file, HttpServletRequest request) {
        validateUploadPermission();
        User currentUser = getCurrentUser();

        List<CustomerUploadRowDTO> rows = parseFile(file);
        applyDuplicateValidation(rows);
        CustomerUpload upload = new CustomerUpload();
        upload.setFileName(file != null ? file.getOriginalFilename() : null);
        upload.setFileSize(file != null ? file.getSize() : null);
        upload.setUploadedBy(currentUser);
        upload.setUploadStatus("PROCESSING");
        upload.setTotalRows(rows.size());
        upload = uploadRepository.save(upload);

        int success = 0;
        int failed = 0;
        for (CustomerUploadRowDTO row : rows) {
            if (!row.isValid()) {
                failed++;
                saveRowErrors(upload, row);
                continue;
            }
            try {
                Customer customer = buildCustomer(row, currentUser);
                Customer savedCustomer = customerRepository.save(Objects.requireNonNull(customer));
                boolean allocated = allocateToAreaHead(savedCustomer, currentUser);
                if (!allocated) {
                    throw new IllegalStateException("No Area Head found for area " +
                            (savedCustomer.getArea() != null ? savedCustomer.getArea().getName() : "N/A"));
                }
                savedCustomer.setStatus("ASSIGNED");
                customerRepository.save(savedCustomer);
                success++;
            } catch (Exception ex) {
                failed++;
                saveRowError(upload, row, "PROCESSING_ERROR", ex.getMessage());
            }
        }

        upload.setSuccessfulRows(success);
        upload.setFailedRows(failed);
        upload.setUploadStatus("COMPLETED");
        upload.setProcessedAt(LocalDateTime.now());
        if (failed > 0) {
            upload.setErrorSummary("Failed rows: " + failed);
        }
        uploadRepository.save(upload);

        Map<String, Object> auditData = new HashMap<>();
        auditData.put("uploadId", upload.getId());
        auditData.put("fileName", upload.getFileName());
        auditData.put("totalRows", upload.getTotalRows());
        auditData.put("successfulRows", success);
        auditData.put("failedRows", failed);
        auditLogService.logAction("UPLOAD", "CUSTOMER_UPLOAD", upload.getId(), null, auditData, request);

        return toResultDTO(upload);
    }

    @Transactional(readOnly = true)
    public List<CustomerUploadHistoryDTO> getMyUploads() {
        User currentUser = getCurrentUser();
        return uploadRepository.findByUploadedByIdOrderByUploadedAtDesc(currentUser.getId()).stream()
                .map(this::toHistoryDTO)
                .collect(Collectors.toList());
    }

    @Transactional(readOnly = true)
    public CustomerUploadResultDTO getUploadResult(Long uploadId) {
        if (uploadId == null) {
            throw new IllegalArgumentException("Upload ID cannot be null");
        }
        CustomerUpload upload = uploadRepository.findById(uploadId)
                .orElseThrow(() -> new IllegalArgumentException("Upload not found"));
        return toResultDTO(upload);
    }

    private void validateUploadPermission() {
        User currentUser = getCurrentUser();
        String userType = HierarchyUtil.normalizeUserType(currentUser);
        if (!HierarchyUtil.CIRCLE_HEAD.equals(userType)) {
            throw new AccessDeniedException("Only Circle Heads can upload customers");
        }
    }

    private List<CustomerUploadRowDTO> parseFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return List.of();
        }
        String fileName = Optional.ofNullable(file.getOriginalFilename()).orElse("").toLowerCase();
        try {
            if (fileName.endsWith(".xlsx")) {
                return parseExcel(file);
            }
            return parseCsv(file);
        } catch (Exception ex) {
            log.error("Error parsing upload file", ex);
            return List.of();
        }
    }

    private List<CustomerUploadRowDTO> parseExcel(MultipartFile file) throws Exception {
        List<CustomerUploadRowDTO> rows = new ArrayList<>();
        try (Workbook workbook = WorkbookFactory.create(file.getInputStream())) {
            Sheet sheet = workbook.getSheetAt(0);
            if (sheet == null) {
                return rows;
            }
            DataFormatter formatter = new DataFormatter();
            Map<Integer, String> headers = resolveHeaders(sheet.getRow(0), formatter);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) {
                    continue;
                }
                Map<String, String> values = new HashMap<>();
                for (Map.Entry<Integer, String> entry : headers.entrySet()) {
                    String value = formatter.formatCellValue(row.getCell(entry.getKey())).trim();
                    values.put(entry.getValue(), value);
                }
                if (isRowEmpty(values)) {
                    continue;
                }
                rows.add(buildRowDto(i + 1, values));
            }
        }
        return rows;
    }

    private List<CustomerUploadRowDTO> parseCsv(MultipartFile file) throws Exception {
        List<CustomerUploadRowDTO> rows = new ArrayList<>();
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            String headerLine = reader.readLine();
            if (headerLine == null) {
                return rows;
            }
            String[] headerParts = headerLine.split(",");
            Map<Integer, String> headers = new HashMap<>();
            for (int i = 0; i < headerParts.length; i++) {
                headers.put(i, normalizeHeader(headerParts[i]));
            }
            String line;
            int rowIndex = 2;
            while ((line = reader.readLine()) != null) {
                String[] parts = line.split(",", -1);
                Map<String, String> values = new HashMap<>();
                for (int i = 0; i < parts.length; i++) {
                    String header = headers.get(i);
                    if (header != null) {
                        values.put(header, parts[i].trim());
                    }
                }
                if (isRowEmpty(values)) {
                    rowIndex++;
                    continue;
                }
                rows.add(buildRowDto(rowIndex, values));
                rowIndex++;
            }
        }
        return rows;
    }

    private Map<Integer, String> resolveHeaders(Row headerRow, DataFormatter formatter) {
        Map<Integer, String> headers = new HashMap<>();
        if (headerRow == null) {
            return headers;
        }
        for (Cell cell : headerRow) {
            String header = normalizeHeader(formatter.formatCellValue(cell));
            headers.put(cell.getColumnIndex(), header);
        }
        return headers;
    }

    private CustomerUploadRowDTO buildRowDto(int rowNumber, Map<String, String> values) {
        List<String> errors = new ArrayList<>();
        for (String required : REQUIRED_HEADERS) {
            if (isBlank(values.get(required))) {
                errors.add(required + " is required");
            }
        }

        String pendingAmount = values.getOrDefault("pending_amount", "").trim();
        if (!isBlank(pendingAmount)) {
            try {
                new BigDecimal(pendingAmount);
            } catch (NumberFormatException ex) {
                errors.add("pending_amount must be a valid number");
            }
        }

        String email = values.getOrDefault("email", "").trim();
        if (!isBlank(email) && !email.contains("@")) {
            errors.add("email must be valid");
        }

        GeographyMatch match = validateGeography(values, errors);

        return CustomerUploadRowDTO.builder()
                .rowNumber(rowNumber)
                .customerName(values.getOrDefault("customer_name", ""))
                .phone(values.getOrDefault("phone", ""))
                .email(values.getOrDefault("email", ""))
                .pendingAmount(pendingAmount)
                .address(values.getOrDefault("address", ""))
                .cluster(values.getOrDefault("cluster", ""))
                .circle(values.getOrDefault("circle", ""))
                .zone(values.getOrDefault("zone", ""))
                .area(values.getOrDefault("area", ""))
                .store(values.getOrDefault("store", ""))
                .valid(errors.isEmpty() && match.valid())
                .errors(errors)
                .build();
    }

    private GeographyMatch validateGeography(Map<String, String> values, List<String> errors) {
        Cluster cluster = findCluster(values.get("cluster"));
        if (cluster == null && !isBlank(values.get("cluster"))) {
            errors.add("cluster not found");
        }
        Circle circle = findCircle(values.get("circle"));
        if (circle == null && !isBlank(values.get("circle"))) {
            errors.add("circle not found");
        }
        Zone zone = findZone(values.get("zone"));
        if (zone == null && !isBlank(values.get("zone"))) {
            errors.add("zone not found");
        }
        Area area = findArea(values.get("area"));
        if (area == null && !isBlank(values.get("area"))) {
            errors.add("area not found");
        }

        if (cluster != null && circle != null && circle.getCluster() != null
                && !cluster.getId().equals(circle.getCluster().getId())) {
            errors.add("circle does not belong to cluster");
        }
        if (circle != null && zone != null && zone.getCircle() != null
                && !circle.getId().equals(zone.getCircle().getId())) {
            errors.add("zone does not belong to circle");
        }
        if (zone != null && area != null && area.getZone() != null
                && !zone.getId().equals(area.getZone().getId())) {
            errors.add("area does not belong to zone");
        }
        return new GeographyMatch(cluster, circle, zone, area, errors.isEmpty());
    }

    private Customer buildCustomer(CustomerUploadRowDTO row, User createdBy) {
        Customer customer = new Customer();
        customer.setCustomerCode(generateCustomerCode(row));
        customer.setFirstName(row.getCustomerName());
        customer.setLastName(null);
        customer.setPhoneEncrypted(encryptionUtil.encrypt(row.getPhone()));
        customer.setEmailEncrypted(isBlank(row.getEmail()) ? null : encryptionUtil.encrypt(row.getEmail()));
        customer.setPendingAmount(new BigDecimal(row.getPendingAmount()));
        customer.setAddressLine1(row.getAddress());
        customer.setStoreName(row.getStore());
        Area area = findArea(row.getArea());
        if (area == null) {
            throw new IllegalArgumentException("Area not found for row " + row.getRowNumber());
        }
        customer.setArea(area);
        customer.setCreatedBy(createdBy);
        customer.setStatus("NEW");
        return customer;
    }

    private boolean allocateToAreaHead(Customer customer, User allocator) {
        if (customer == null || customer.getArea() == null) {
            return false;
        }
        List<User> areaHeads = userRepository.findByAreaIdAndUserType(customer.getArea().getId(), HierarchyUtil.AREA_HEAD);
        if (areaHeads.isEmpty()) {
            return false;
        }
        User assignee = areaHeads.get(0);
        CustomerAllocation allocation = new CustomerAllocation();
        allocation.setCustomer(customer);
        allocation.setUser(assignee);
        allocation.setRoleCode(HierarchyUtil.AREA_HEAD);
        allocation.setAllocationType("UPLOAD");
        allocation.setStatus("ACTIVE");
        allocation.setAllocatedBy(allocator);
        allocation.setAllocatedAt(LocalDateTime.now());
        allocationRepository.save(allocation);

        notificationService.notifyUser(
                assignee,
                "ASSIGNMENT",
                "New customer uploaded",
                String.format("Customer %s assigned from upload.", customer.getCustomerCode()),
                "CUSTOMER",
                customer.getId(),
                "INFO"
        );
        return true;
    }

    private void saveRowErrors(CustomerUpload upload, CustomerUploadRowDTO row) {
        if (row.getErrors() == null || row.getErrors().isEmpty()) {
            return;
        }
        for (String error : row.getErrors()) {
            CustomerUploadError uploadError = new CustomerUploadError();
            uploadError.setUpload(upload);
            uploadError.setRowNumber(row.getRowNumber());
            uploadError.setErrorCode("VALIDATION_ERROR");
            uploadError.setErrorMessage(error);
            uploadError.setRowData(row.toString());
            uploadErrorRepository.save(uploadError);
        }
    }

    private void saveRowError(CustomerUpload upload, CustomerUploadRowDTO row, String code, String message) {
        CustomerUploadError uploadError = new CustomerUploadError();
        uploadError.setUpload(upload);
        uploadError.setRowNumber(row.getRowNumber());
        uploadError.setErrorCode(code);
        uploadError.setErrorMessage(message);
        uploadError.setRowData(row.toString());
        uploadErrorRepository.save(uploadError);
    }

    private Cluster findCluster(String value) {
        if (isBlank(value)) {
            return null;
        }
        return clusterRepository.findByCode(value.trim())
                .orElseGet(() -> clusterRepository.findByNameIgnoreCase(value.trim()).orElse(null));
    }

    private Circle findCircle(String value) {
        if (isBlank(value)) {
            return null;
        }
        return circleRepository.findByCode(value.trim())
                .orElseGet(() -> circleRepository.findByNameIgnoreCase(value.trim()).orElse(null));
    }

    private Zone findZone(String value) {
        if (isBlank(value)) {
            return null;
        }
        return zoneRepository.findByCode(value.trim())
                .orElseGet(() -> zoneRepository.findByNameIgnoreCase(value.trim()).orElse(null));
    }

    private Area findArea(String value) {
        if (isBlank(value)) {
            return null;
        }
        return areaRepository.findByCode(value.trim())
                .orElseGet(() -> areaRepository.findByNameIgnoreCase(value.trim()).orElse(null));
    }

    private String generateCustomerCode(CustomerUploadRowDTO row) {
        return "CUST-" + UUID.randomUUID().toString().replace("-", "").substring(0, 10).toUpperCase();
    }

    private String normalizeHeader(String header) {
        if (header == null) {
            return "";
        }
        return header.trim()
                .toLowerCase()
                .replace(" ", "_")
                .replace("-", "_");
    }

    private boolean isRowEmpty(Map<String, String> values) {
        return values.values().stream().allMatch(this::isBlank);
    }

    private boolean isBlank(String value) {
        return value == null || value.trim().isEmpty();
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

    private CustomerUploadResultDTO toResultDTO(CustomerUpload upload) {
        List<CustomerUploadErrorDTO> errors = uploadErrorRepository
                .findByUploadIdOrderByRowNumberAsc(upload.getId())
                .stream()
                .map(err -> CustomerUploadErrorDTO.builder()
                        .rowNumber(err.getRowNumber())
                        .columnName(err.getColumnName())
                        .errorCode(err.getErrorCode())
                        .errorMessage(err.getErrorMessage())
                        .build())
                .collect(Collectors.toList());
        return CustomerUploadResultDTO.builder()
                .uploadId(upload.getId())
                .fileName(upload.getFileName())
                .totalRows(upload.getTotalRows())
                .successfulRows(upload.getSuccessfulRows())
                .failedRows(upload.getFailedRows())
                .status(upload.getUploadStatus())
                .uploadedAt(upload.getUploadedAt())
                .processedAt(upload.getProcessedAt())
                .errorSummary(upload.getErrorSummary())
                .errors(errors)
                .build();
    }

    private CustomerUploadHistoryDTO toHistoryDTO(CustomerUpload upload) {
        String uploaderName = upload.getUploadedBy() != null
                ? (upload.getUploadedBy().getFullName() != null
                ? upload.getUploadedBy().getFullName()
                : upload.getUploadedBy().getUsername())
                : null;
        String uploaderEmployeeId = upload.getUploadedBy() != null
                ? upload.getUploadedBy().getEmployeeId()
                : null;
        return CustomerUploadHistoryDTO.builder()
                .id(upload.getId())
                .fileName(upload.getFileName())
                .totalRows(upload.getTotalRows())
                .successfulRows(upload.getSuccessfulRows())
                .failedRows(upload.getFailedRows())
                .status(upload.getUploadStatus())
                .uploadedAt(upload.getUploadedAt())
                .processedAt(upload.getProcessedAt())
                .errorSummary(upload.getErrorSummary())
                .uploadedByName(uploaderName)
                .uploadedByEmployeeId(uploaderEmployeeId)
                .build();
    }

    private void applyDuplicateValidation(List<CustomerUploadRowDTO> rows) {
        if (rows == null || rows.isEmpty()) {
            return;
        }
        Map<String, Integer> phoneCounts = new HashMap<>();
        Map<String, Integer> emailCounts = new HashMap<>();
        for (CustomerUploadRowDTO row : rows) {
            if (!isBlank(row.getPhone())) {
                String encryptedPhone = encryptionUtil.encrypt(row.getPhone());
                phoneCounts.put(encryptedPhone, phoneCounts.getOrDefault(encryptedPhone, 0) + 1);
            }
            if (!isBlank(row.getEmail())) {
                String encryptedEmail = encryptionUtil.encrypt(row.getEmail());
                emailCounts.put(encryptedEmail, emailCounts.getOrDefault(encryptedEmail, 0) + 1);
            }
        }

        for (CustomerUploadRowDTO row : rows) {
            List<String> errors = row.getErrors() != null ? new ArrayList<>(row.getErrors()) : new ArrayList<>();
            boolean valid = row.isValid();
            if (!isBlank(row.getPhone())) {
                String encryptedPhone = encryptionUtil.encrypt(row.getPhone());
                if (phoneCounts.getOrDefault(encryptedPhone, 0) > 1) {
                    errors.add("duplicate phone in upload");
                    valid = false;
                }
                if (customerRepository.existsByPhoneEncrypted(encryptedPhone)) {
                    errors.add("phone already exists");
                    valid = false;
                }
            }
            if (!isBlank(row.getEmail())) {
                String encryptedEmail = encryptionUtil.encrypt(row.getEmail());
                if (emailCounts.getOrDefault(encryptedEmail, 0) > 1) {
                    errors.add("duplicate email in upload");
                    valid = false;
                }
                if (customerRepository.existsByEmailEncrypted(encryptedEmail)) {
                    errors.add("email already exists");
                    valid = false;
                }
            }
            row.setErrors(errors);
            row.setValid(valid);
        }
    }

    private record GeographyMatch(Cluster cluster, Circle circle, Zone zone, Area area, boolean valid) {}
}
