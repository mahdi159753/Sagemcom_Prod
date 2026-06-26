package com.alibou.security.dailyproduction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class KpiSummaryResponse {
    // Current period KPIs
    private Double trg;
    private Double trs;
    private Double disponibilite;
    private Double performance;
    private Double qualite;
    private Long quantiteProduite;
    private Long objectifProduction;

    // Trends vs previous period (percentage change)
    private Double trgTrend;
    private Double disponibiliteTrend;
    private Double performanceTrend;
    private Double qualiteTrend;
    private Double productionTrend;

    // History for charts
    private List<DailyKpiPoint> trgHistory;
    private List<DailyKpiPoint> qualiteHistory;
    private List<DailyKpiPoint> dispHistory;
    private List<DailyKpiPoint> perfHistory;
    private List<DailyKpiPoint> productionHistory;

    // Historical table data
    private List<TrgHistoryEntry> tableHistory;
}
