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
public class CustomerUploadRowDTO {
    private Integer rowNumber;
    private String customerName;
    private String phone;
    private String email;
    private String pendingAmount;
    private String address;
    private String cluster;
    private String circle;
    private String zone;
    private String area;
    private boolean valid;
    private List<String> errors;
}
