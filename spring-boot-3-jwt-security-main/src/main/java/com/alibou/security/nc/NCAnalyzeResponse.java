package com.alibou.security.nc;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NCAnalyzeResponse {
    private String suggested_root_cause;
    private String recommended_action;
    private String recommended_assignee;
    private double confidence_score;
}
