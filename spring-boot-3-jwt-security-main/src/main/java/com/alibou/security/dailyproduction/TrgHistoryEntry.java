package com.alibou.security.dailyproduction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TrgHistoryEntry {
    private String date;
    private String shift;
    private Long produced;
    private Long target;
    private Double trg;
    private Double trs;
    private String downtime;
}
