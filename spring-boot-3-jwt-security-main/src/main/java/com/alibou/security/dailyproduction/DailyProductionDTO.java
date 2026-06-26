package com.alibou.security.dailyproduction;

import lombok.Data;
import java.time.LocalDate;

@Data
public class DailyProductionDTO {
    private LocalDate date;
    private String chantier;
    private Integer ligne;
    private String indicatorName;
    private String value;
}
