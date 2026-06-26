package com.alibou.security.downtime;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface DowntimeRepository extends JpaRepository<Downtime, Integer> {
    
    @Query("SELECT d FROM Downtime d WHERE d.startTime <= :endDate AND (d.endTime IS NULL OR d.endTime >= :startDate)")
    List<Downtime> findOverlapping(@Param("startDate") LocalDateTime startDate, @Param("endDate") LocalDateTime endDate);

    @Query("SELECT d FROM Downtime d WHERE d.chantier = :chantier AND d.startTime <= :endDate AND (d.endTime IS NULL OR d.endTime >= :startDate)")
    List<Downtime> findByChantierAndDateRange(
            @Param("chantier") String chantier,
            @Param("startDate") LocalDateTime startDate,
            @Param("endDate") LocalDateTime endDate);
}
