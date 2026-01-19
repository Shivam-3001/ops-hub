package com.company.ops_hub_api.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class DashboardCardDTO {
    private String id;
    private String title;
    private String value;
    private String subtitle;
    private String icon;
    private String color;
    private Integer trend;
}
