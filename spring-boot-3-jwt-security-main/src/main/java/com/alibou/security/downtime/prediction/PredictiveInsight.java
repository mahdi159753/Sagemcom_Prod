package com.alibou.security.downtime.prediction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PredictiveInsight {
    private String ligne;
    private double probability;
    private String likely_type;
    private int estimated_time_to_failure_hours;
    private String recommended_action;
}
