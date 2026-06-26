package com.alibou.security.dailyproduction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyKpiPoint {
    private String date;    // "2026-04-01"
    private Double value;   // e.g. 82.5
}
