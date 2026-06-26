package com.alibou.security.dashboard;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardPieChartDTO {
    private String label;
    private Long value; // Can be total minutes or percentage. For simplicity, percentage is good, but I'll return percentage
    private String color;
}
