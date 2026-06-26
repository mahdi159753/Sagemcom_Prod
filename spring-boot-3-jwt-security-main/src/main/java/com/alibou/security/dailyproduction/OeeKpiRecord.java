package com.alibou.security.dailyproduction;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class OeeKpiRecord {
    private String date;
    private Integer ligne;
    private Double trg;
    private Double fpy_vision;
    private Double tx_arrachement;
    private Double encours_depannage;
    private Double efficience;
}
