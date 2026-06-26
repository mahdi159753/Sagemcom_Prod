package com.alibou.security.dailyproduction;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface DailyProductionRepository extends JpaRepository<DailyProductionIndicator, Long> {
    List<DailyProductionIndicator> findByDateAndChantier(LocalDate date, String chantier);
    Optional<DailyProductionIndicator> findByDateAndChantierAndLigneAndIndicatorName(LocalDate date, String chantier, Integer ligne, String indicatorName);
    void deleteByDateAndChantier(LocalDate date, String chantier);
    void deleteByDateAndChantierAndIndicatorName(LocalDate date, String chantier, String indicatorName);

    List<DailyProductionIndicator> findByChantierAndDateBetween(String chantier, LocalDate dateFrom, LocalDate dateTo);

    @Query("SELECT DISTINCT d.date FROM DailyProductionIndicator d WHERE d.chantier = :chantier AND d.date BETWEEN :dateFrom AND :dateTo ORDER BY d.date")
    List<LocalDate> findDistinctDatesByChantier(
            @Param("chantier") String chantier,
            @Param("dateFrom") LocalDate dateFrom,
            @Param("dateTo") LocalDate dateTo);
}
