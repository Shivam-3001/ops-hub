package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerUploadPreviewResponseDTO {
    private String fileName;
    private Integer totalRows;
    private Integer validRows;
    private Integer invalidRows;
    private List<CustomerUploadRowDTO> rows;
    private List<String> errors;
}
