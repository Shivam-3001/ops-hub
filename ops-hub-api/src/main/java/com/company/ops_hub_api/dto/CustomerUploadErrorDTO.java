package com.company.ops_hub_api.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class CustomerUploadErrorDTO {
    private Integer rowNumber;
    private String columnName;
    private String errorCode;
    private String errorMessage;
}
