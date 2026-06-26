package com.alibou.security.dailyproduction;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "daily_production_indicators", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"date", "chantier", "ligne", "indicator_name"})
})
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyProductionIndicator {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private LocalDate date;

    @Column(nullable = false, length = 50)
    private String chantier;

    @Column(nullable = false)
    private Integer ligne; // 1 to 8

    @Column(name = "indicator_name", nullable = false, length = 100)
    private String indicatorName; // e.g. "TRG (%)"

    @Column(length = 100)
    private String stringValue; // To store numeric, blank, or text

    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
