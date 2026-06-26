package com.alibou.security.downtime;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DowntimeStatsResponse {
    private String today;
    private String todayChange;
    private boolean todayIsUp;

    private String thisWeek;
    private String weekChange;
    private boolean weekIsUp;

    private String thisMonth;
    private String monthChange;
    private boolean monthIsUp;

    private String avgPerDay;
    private String avgChange;
    private boolean avgIsUp;
}
