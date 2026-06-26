package com.alibou.security.dashboard;

import com.alibou.security.downtime.Downtime;
import com.alibou.security.downtime.DowntimeRepository;
import com.alibou.security.nc.NonConformity;
import com.alibou.security.nc.NonConformityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class DashboardService {

    private final DowntimeRepository downtimeRepository;
    private final NonConformityRepository ncRepository;

    public List<DashboardAlertDTO> getRecentAlerts() {
        List<DashboardAlertDTO> alerts = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, HH:mm");

        // 1. Fetch unresolved Non-Conformities (CRITIQUE or MAJEUR)
        List<NonConformity> openNcs = ncRepository.findByStatut("OUVERTE");
        for (NonConformity nc : openNcs) {
            String severity = "CRITIQUE".equalsIgnoreCase(nc.getGravite()) ? "critical" : "warning";
            alerts.add(DashboardAlertDTO.builder()
                    .id("NC-" + nc.getId())
                    .title("NC: " + nc.getReference() + " (" + nc.getOrigine() + ")")
                    .description(nc.getDescription())
                    .severity(severity)
                    .time(nc.getDateDeclaration() != null ? nc.getDateDeclaration().format(formatter) : "N/A")
                    .build());
        }

        // 2. Fetch ONGOING downtimes
        LocalDateTime now = LocalDateTime.now();
        List<Downtime> overLapping = downtimeRepository.findOverlapping(now.minusDays(1), now);
        for(Downtime d : overLapping) {
            if ("ONGOING".equalsIgnoreCase(d.getStatut())) {
                String severity = "CRITICAL".equalsIgnoreCase(d.getSeverity()) ? "critical" : "warning";
                alerts.add(DashboardAlertDTO.builder()
                        .id("DT-" + d.getId())
                        .title("Ongoing Downtime: " + d.getLigne())
                        .description(d.getType() + " - " + d.getDescription())
                        .severity(severity)
                        .time(d.getStartTime() != null ? d.getStartTime().format(formatter) : "N/A")
                        .build());
            }
        }

        // Return max 5 alerts
        if (alerts.size() > 5) {
            return alerts.subList(0, 5);
        }
        return alerts;
    }

    public List<DashboardPieChartDTO> getDowntimeCausesPie() {
        LocalDateTime to = LocalDateTime.now();
        LocalDateTime from = to.minusDays(7); // past week
        
        List<Downtime> downtimes = downtimeRepository.findOverlapping(from, to);
        
        Map<String, Long> durationByType = new HashMap<>();
        long totalDurationMinutes = 0;
        
        for (Downtime d : downtimes) {
            LocalDateTime start = d.getStartTime().isBefore(from) ? from : d.getStartTime();
            LocalDateTime end = (d.getEndTime() == null || d.getEndTime().isAfter(to)) ? to : d.getEndTime();
            
            if (start.isBefore(end)) {
                long mins = Duration.between(start, end).toMinutes();
                String type = d.getType() != null ? d.getType() : "Unknown";
                
                durationByType.put(type, durationByType.getOrDefault(type, 0L) + mins);
                totalDurationMinutes += mins;
            }
        }
        
        List<DashboardPieChartDTO> result = new ArrayList<>();
        if (totalDurationMinutes == 0) {
            return result;
        }

        // Colors pool
        String[] colors = {"#DC3545", "#FF8C42", "#FFC107", "#2E75B6", "#9CA3AF"};
        int colorIdx = 0;

        for (Map.Entry<String, Long> entry : durationByType.entrySet()) {
            long percentage = Math.round((entry.getValue() * 100.0) / totalDurationMinutes);
            result.add(DashboardPieChartDTO.builder()
                    .label(entry.getKey())
                    .value(percentage)
                    .color(colors[colorIdx % colors.length])
                    .build());
            colorIdx++;
        }
        
        // Sort descending
        result.sort((a,b) -> Long.compare(b.getValue(), a.getValue()));
        return result;
    }
}
