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
public class ClusterFilterDTO {
    private Long id;
    private String code;
    private String name;
    private List<CircleFilterDTO> circles;
}
