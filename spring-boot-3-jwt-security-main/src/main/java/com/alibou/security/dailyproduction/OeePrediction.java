package com.alibou.security.dailyproduction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OeePrediction {
    private Integer ligne;
    private Double predictedTrg;
    private String rootCauseIndicator;
    private String recommendation;
}
