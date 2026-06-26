package com.alibou.security.dailyproduction;

import com.alibou.security.downtime.Downtime;
import com.alibou.security.downtime.DowntimeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import org.springframework.web.client.RestTemplate;
import org.springframework.http.ResponseEntity;

@Service
@RequiredArgsConstructor
public class DailyProductionService {
    private final DailyProductionRepository repository;
    private final DowntimeRepository downtimeRepository;

    @Value("${predictive.agent.url:http://localhost:8000}")
    private String predictiveAgentUrl;

    private static final long SHIFT_MINUTES = 480; // 8 hours per shift

    // ── Existing CRUD methods ────────────────────────────────────────

    public List<DailyProductionIndicator> getIndicators(LocalDate date, String chantier) {
        return repository.findByDateAndChantier(date, chantier);
    }

    @Transactional
    public void deleteByDateAndChantier(LocalDate date, String chantier) {
        repository.deleteByDateAndChantier(date, chantier);
    }

    @Transactional
    public void deleteByIndicatorName(LocalDate date, String chantier, String indicatorName) {
        repository.deleteByDateAndChantierAndIndicatorName(date, chantier, indicatorName);
    }

    @Transactional
    public void saveBatch(List<DailyProductionDTO> batch) {
        for (DailyProductionDTO dto : batch) {
            DailyProductionIndicator indicator = repository
                .findByDateAndChantierAndLigneAndIndicatorName(dto.getDate(), dto.getChantier(), dto.getLigne(), dto.getIndicatorName())
                .orElse(DailyProductionIndicator.builder()
                    .date(dto.getDate())
                    .chantier(dto.getChantier())
                    .ligne(dto.getLigne())
                    .indicatorName(dto.getIndicatorName())
                    .build());
            indicator.setStringValue(dto.getValue());
            repository.save(indicator);
        }
    }

    public List<OeePrediction> getOeePredictions(String chantier) {
        // Fetch last 30 days of indicators
        LocalDate dateTo = LocalDate.now();
        LocalDate dateFrom = dateTo.minusDays(30);
        
        List<DailyProductionIndicator> indicators = repository.findByChantierAndDateBetween(chantier, dateFrom, dateTo);
        List<LocalDate> dates = repository.findDistinctDatesByChantier(chantier, dateFrom, dateTo);
        
        Map<LocalDate, List<DailyProductionIndicator>> byDate = indicators.stream()
                .collect(Collectors.groupingBy(DailyProductionIndicator::getDate));
                
        List<OeeKpiRecord> history = new ArrayList<>();
        
        for (LocalDate date : dates) {
            List<DailyProductionIndicator> dayIndicators = byDate.getOrDefault(date, Collections.emptyList());
            
            // Group by ligne (1 to 8)
            Map<Integer, List<DailyProductionIndicator>> byLigne = dayIndicators.stream()
                    .collect(Collectors.groupingBy(DailyProductionIndicator::getLigne));
                    
            for (int ligne = 1; ligne <= 8; ligne++) {
                List<DailyProductionIndicator> ligneInds = byLigne.getOrDefault(ligne, Collections.emptyList());
                if (ligneInds.isEmpty()) continue;
                
                Double trg = extractNumeric(ligneInds, "TRG (%)");
                Double fpyVision = extractNumeric(ligneInds, "FPY Vision / FCT2 (%)");
                Double txArrachement = extractNumeric(ligneInds, "Tx Arrachement (%)");
                Double encoursDepannage = extractNumeric(ligneInds, "Encours Depannage");
                Double efficience = extractNumeric(ligneInds, "Efficience / Ecart DMH");
                
                if (trg != null) {
                    history.add(OeeKpiRecord.builder()
                        .date(date.toString())
                        .ligne(ligne)
                        .trg(trg)
                        .fpy_vision(fpyVision != null ? fpyVision : 95.0)
                        .tx_arrachement(txArrachement != null ? txArrachement : 0.0)
                        .encours_depannage(encoursDepannage != null ? encoursDepannage : 0.0)
                        .efficience(efficience != null ? efficience : 100.0)
                        .build());
                }
            }
        }
        
        if (history.isEmpty()) {
            return Collections.emptyList();
        }
        
        RestTemplate restTemplate = new RestTemplate();
        OeePredictionRequest request = new OeePredictionRequest(history);
        try {
            ResponseEntity<OeePrediction[]> response = restTemplate.postForEntity(
                predictiveAgentUrl + "/predict-oee", request, OeePrediction[].class);
            if (response.getBody() != null) {
                return Arrays.asList(response.getBody());
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return Collections.emptyList();
    }
    
    private Double extractNumeric(List<DailyProductionIndicator> inds, String name) {
        return inds.stream()
            .filter(i -> name.equals(i.getIndicatorName()))
            .map(i -> parseDouble(i.getStringValue()))
            .filter(Objects::nonNull)
            .findFirst()
            .orElse(null);
    }

    // ── KPI Summary ──────────────────────────────────────────────────

    public KpiSummaryResponse getKpiSummary(String chantier, LocalDate dateFrom, LocalDate dateTo) {
        // 1. Fetch all indicators for the current period
        List<DailyProductionIndicator> indicators = repository.findByChantierAndDateBetween(chantier, dateFrom, dateTo);
        List<LocalDate> dates = repository.findDistinctDatesByChantier(chantier, dateFrom, dateTo);

        // 2. Fetch indicators for the previous period (same duration) for trend calculation
        long daysBetween = java.time.temporal.ChronoUnit.DAYS.between(dateFrom, dateTo) + 1;
        LocalDate prevFrom = dateFrom.minusDays(daysBetween);
        LocalDate prevTo = dateFrom.minusDays(1);
        List<DailyProductionIndicator> prevIndicators = repository.findByChantierAndDateBetween(chantier, prevFrom, prevTo);

        // 3. Group indicators by date
        Map<LocalDate, List<DailyProductionIndicator>> byDate = indicators.stream()
                .collect(Collectors.groupingBy(DailyProductionIndicator::getDate));

        // 4. Calculate current period KPIs
        double totalTrg = 0, totalQualite = 0, totalPerformance = 0;
        long totalProduced = 0, totalObjectif = 0;
        int trgCount = 0, qualiteCount = 0, perfCount = 0;

        List<DailyKpiPoint> trgHistory = new ArrayList<>();
        List<DailyKpiPoint> qualiteHistory = new ArrayList<>();
        List<DailyKpiPoint> perfHistory = new ArrayList<>();
        List<DailyKpiPoint> dispHistory = new ArrayList<>();
        List<DailyKpiPoint> productionHistory = new ArrayList<>();
        List<TrgHistoryEntry> tableHistory = new ArrayList<>();

        for (LocalDate date : dates) {
            List<DailyProductionIndicator> dayIndicators = byDate.getOrDefault(date, Collections.emptyList());

            // Extract daily values (averaged across all 8 lignes)
            Double dayTrg = avgNumericIndicator(dayIndicators, "TRG (%)");
            Double dayFpyVision = avgNumericIndicator(dayIndicators, "FPY Vision / FCT2 (%)");
            Double dayFpyTelecharge = avgNumericIndicator(dayIndicators, "FPY Telechargement (%)");
            Double dayFpyBouton = avgNumericIndicator(dayIndicators, "FPY Bouton / NFT (%)");
            Long dayProduced = sumNumericIndicator(dayIndicators, "Quantites Produites");
            Double dayObjectifPct = avgNumericIndicator(dayIndicators, "Objectif Planning (%)");

            // Quality = average of available FPY values
            List<Double> fpyValues = new ArrayList<>();
            if (dayFpyVision != null) fpyValues.add(dayFpyVision);
            if (dayFpyTelecharge != null) fpyValues.add(dayFpyTelecharge);
            if (dayFpyBouton != null) fpyValues.add(dayFpyBouton);
            Double dayQualite = fpyValues.isEmpty() ? null : fpyValues.stream().mapToDouble(v -> v).average().orElse(0);

            // Performance = Quantites Produites / Objectif (using objectif as target reference)
            // We derive target from objectif planning %: if objectif=100%, target=produced/1.0
            // Simple: performance = dayObjectifPct (represents how much of the planned objective was achieved)
            Double dayPerf = dayObjectifPct;

            // Disponibilité from downtime module
            Double dayDisp = calculateDisponibilite(chantier, date);

            // Accumulate totals
            if (dayTrg != null) { totalTrg += dayTrg; trgCount++; }
            if (dayQualite != null) { totalQualite += dayQualite; qualiteCount++; }
            if (dayPerf != null) { totalPerformance += dayPerf; perfCount++; }
            if (dayProduced != null) totalProduced += dayProduced;

            // Build history points
            String dateStr = date.toString();
            if (dayTrg != null) trgHistory.add(DailyKpiPoint.builder().date(dateStr).value(round2(dayTrg)).build());
            if (dayQualite != null) qualiteHistory.add(DailyKpiPoint.builder().date(dateStr).value(round2(dayQualite)).build());
            if (dayPerf != null) perfHistory.add(DailyKpiPoint.builder().date(dateStr).value(round2(dayPerf)).build());
            if (dayDisp != null) dispHistory.add(DailyKpiPoint.builder().date(dateStr).value(round2(dayDisp)).build());
            productionHistory.add(DailyKpiPoint.builder().date(dateStr).value(dayProduced != null ? dayProduced.doubleValue() : 0).build());

            // Build table history entry
            String downtimeStr = formatDowntime(chantier, date);
            tableHistory.add(TrgHistoryEntry.builder()
                    .date(dateStr)
                    .shift("All")
                    .produced(dayProduced != null ? dayProduced : 0)
                    .target(0L) // Will set below
                    .trg(dayTrg != null ? round2(dayTrg) : 0)
                    .trs(dayTrg != null && dayDisp != null && dayPerf != null && dayQualite != null
                            ? round2(dayDisp * dayPerf * dayQualite / 10000.0)
                            : 0)
                    .downtime(downtimeStr)
                    .build());
        }

        // 5. Compute period averages
        Double avgTrg = trgCount > 0 ? round2(totalTrg / trgCount) : null;
        Double avgQualite = qualiteCount > 0 ? round2(totalQualite / qualiteCount) : null;
        Double avgPerf = perfCount > 0 ? round2(totalPerformance / perfCount) : null;

        // Disponibilité: average across all days in the period
        Double avgDisp = dispHistory.isEmpty() ? null :
                round2(dispHistory.stream().mapToDouble(DailyKpiPoint::getValue).average().orElse(0));

        // TRS = Disponibilite × Performance × Qualite / 10000
        Double trs = (avgDisp != null && avgPerf != null && avgQualite != null)
                ? round2(avgDisp * avgPerf * avgQualite / 10000.0)
                : null;

        // 6. Calculate trends vs previous period
        Double prevTrg = calcPeriodAvg(prevIndicators, "TRG (%)");
        Double prevQualite = calcPeriodQualite(prevIndicators);
        Double prevPerf = calcPeriodAvg(prevIndicators, "Objectif Planning (%)");
        Long prevProduced = calcPeriodSum(prevIndicators, "Quantites Produites");

        Double trgTrend = trend(avgTrg, prevTrg);
        Double qualiteTrend = trend(avgQualite, prevQualite);
        Double perfTrend = trend(avgPerf, prevPerf);
        Double dispTrend = null; // No simple trend for dispo
        Double prodTrend = (prevProduced != null && prevProduced > 0 && totalProduced > 0)
                ? round2(((double)(totalProduced - prevProduced) / prevProduced) * 100)
                : null;

        return KpiSummaryResponse.builder()
                .trg(avgTrg)
                .trs(trs)
                .disponibilite(avgDisp)
                .performance(avgPerf)
                .qualite(avgQualite)
                .quantiteProduite(totalProduced)
                .objectifProduction(totalObjectif)
                .trgTrend(trgTrend)
                .disponibiliteTrend(dispTrend)
                .performanceTrend(perfTrend)
                .qualiteTrend(qualiteTrend)
                .productionTrend(prodTrend)
                .trgHistory(trgHistory)
                .qualiteHistory(qualiteHistory)
                .dispHistory(dispHistory)
                .perfHistory(perfHistory)
                .productionHistory(productionHistory)
                .tableHistory(tableHistory)
                .build();
    }

    // ── Helper methods ───────────────────────────────────────────────

    /**
     * Average numeric indicator across all lines for a given day
     */
    private Double avgNumericIndicator(List<DailyProductionIndicator> dayIndicators, String indicatorName) {
        List<Double> values = dayIndicators.stream()
                .filter(i -> indicatorName.equals(i.getIndicatorName()))
                .map(i -> parseDouble(i.getStringValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (values.isEmpty()) return null;
        return values.stream().mapToDouble(v -> v).average().orElse(0);
    }

    /**
     * Sum numeric indicator across all lines for a given day
     */
    private Long sumNumericIndicator(List<DailyProductionIndicator> dayIndicators, String indicatorName) {
        List<Double> values = dayIndicators.stream()
                .filter(i -> indicatorName.equals(i.getIndicatorName()))
                .map(i -> parseDouble(i.getStringValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (values.isEmpty()) return null;
        return values.stream().mapToLong(Double::longValue).sum();
    }

    /**
     * Calculate disponibilité for a given chantier and date.
     * Disponibilité = (Planned time - Downtime) / Planned time × 100
     * Planned time = number of shifts × SHIFT_MINUTES
     */
    private Double calculateDisponibilite(String chantier, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();

        List<Downtime> downtimes = downtimeRepository.findByChantierAndDateRange(chantier, dayStart, dayEnd);

        long totalDowntimeMinutes = 0;
        for (Downtime d : downtimes) {
            LocalDateTime start = d.getStartTime();
            LocalDateTime end = d.getEndTime() != null ? d.getEndTime() : LocalDateTime.now();

            // Clamp to this day
            LocalDateTime intersectStart = start.isAfter(dayStart) ? start : dayStart;
            LocalDateTime intersectEnd = end.isBefore(dayEnd) ? end : dayEnd;

            if (intersectStart.isBefore(intersectEnd)) {
                totalDowntimeMinutes += Duration.between(intersectStart, intersectEnd).toMinutes();
            }
        }

        // Assume 1 shift per day (8 hours = 480 min)
        long plannedMinutes = SHIFT_MINUTES;
        if (plannedMinutes <= 0) return 100.0;

        double dispo = ((double)(plannedMinutes - totalDowntimeMinutes) / plannedMinutes) * 100.0;
        return Math.max(0, Math.min(100, dispo));
    }

    private String formatDowntime(String chantier, LocalDate date) {
        LocalDateTime dayStart = date.atStartOfDay();
        LocalDateTime dayEnd = date.plusDays(1).atStartOfDay();
        List<Downtime> downtimes = downtimeRepository.findByChantierAndDateRange(chantier, dayStart, dayEnd);

        long totalMinutes = 0;
        for (Downtime d : downtimes) {
            LocalDateTime start = d.getStartTime();
            LocalDateTime end = d.getEndTime() != null ? d.getEndTime() : LocalDateTime.now();
            LocalDateTime intersectStart = start.isAfter(dayStart) ? start : dayStart;
            LocalDateTime intersectEnd = end.isBefore(dayEnd) ? end : dayEnd;
            if (intersectStart.isBefore(intersectEnd)) {
                totalMinutes += Duration.between(intersectStart, intersectEnd).toMinutes();
            }
        }

        if (totalMinutes == 0) return "0 min";
        long hours = totalMinutes / 60;
        long mins = totalMinutes % 60;
        if (hours == 0) return mins + " min";
        return hours + "h " + mins + " min";
    }

    private Double calcPeriodAvg(List<DailyProductionIndicator> indicators, String indicatorName) {
        List<Double> values = indicators.stream()
                .filter(i -> indicatorName.equals(i.getIndicatorName()))
                .map(i -> parseDouble(i.getStringValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (values.isEmpty()) return null;
        return values.stream().mapToDouble(v -> v).average().orElse(0);
    }

    private Double calcPeriodQualite(List<DailyProductionIndicator> indicators) {
        List<Double> fpyVision = indicators.stream()
                .filter(i -> "FPY Vision / FCT2 (%)".equals(i.getIndicatorName()))
                .map(i -> parseDouble(i.getStringValue()))
                .filter(Objects::nonNull).collect(Collectors.toList());
        List<Double> fpyTele = indicators.stream()
                .filter(i -> "FPY Telechargement (%)".equals(i.getIndicatorName()))
                .map(i -> parseDouble(i.getStringValue()))
                .filter(Objects::nonNull).collect(Collectors.toList());
        List<Double> fpyBouton = indicators.stream()
                .filter(i -> "FPY Bouton / NFT (%)".equals(i.getIndicatorName()))
                .map(i -> parseDouble(i.getStringValue()))
                .filter(Objects::nonNull).collect(Collectors.toList());

        List<Double> allFpy = new ArrayList<>();
        allFpy.addAll(fpyVision);
        allFpy.addAll(fpyTele);
        allFpy.addAll(fpyBouton);
        if (allFpy.isEmpty()) return null;
        return allFpy.stream().mapToDouble(v -> v).average().orElse(0);
    }

    private Long calcPeriodSum(List<DailyProductionIndicator> indicators, String indicatorName) {
        List<Double> values = indicators.stream()
                .filter(i -> indicatorName.equals(i.getIndicatorName()))
                .map(i -> parseDouble(i.getStringValue()))
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
        if (values.isEmpty()) return null;
        return values.stream().mapToLong(Double::longValue).sum();
    }

    private Double trend(Double current, Double previous) {
        if (current == null || previous == null || previous == 0) return null;
        return round2(current - previous);
    }

    private Double parseDouble(String val) {
        if (val == null || val.isBlank() || "-".equals(val.trim())) return null;
        try {
            return Double.parseDouble(val.trim());
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private double round2(double val) {
        return Math.round(val * 100.0) / 100.0;
    }
}
