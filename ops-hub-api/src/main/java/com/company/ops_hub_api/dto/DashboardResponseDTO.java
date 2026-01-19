package com.company.ops_hub_api.dto;

import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class DashboardResponseDTO {
    private String userType;
    private String userName;
    private List<DashboardCardDTO> cards;
    private List<DashboardMetricDTO> metrics;
    private List<DashboardActivityDTO> recentActivity;
    private List<DashboardActivityDTO> alerts;
}
