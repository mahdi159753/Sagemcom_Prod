package com.alibou.security.downtime;

import com.alibou.security.notification.NotificationService;
import com.alibou.security.notification.NotificationType;
import com.alibou.security.downtime.prediction.PredictiveInsight;
import com.alibou.security.downtime.prediction.PredictiveAgentClient;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class DowntimeService {

    private final DowntimeRepository repository;
    private final NotificationService notificationService;
    private final PredictiveAgentClient predictiveClient;

    public Downtime declareDowntime(Downtime downtime) {
        Downtime saved = repository.save(downtime);
        notificationService.sendAlert("Un arrêt (Downtime) de criticité " + saved.getSeverity() + " a été déclaré sur " + saved.getLigne(), NotificationType.DOWNTIME);
        return saved;
    }

    public List<Downtime> getAllDowntimes() {
        return repository.findAll();
    }

    public List<PredictiveInsight> getPredictiveInsights() {
        List<Downtime> history = repository.findAll();
        return predictiveClient.getPredictions(history);
    }

    public Downtime resolveDowntime(Integer id) {
        Downtime downtime = repository.findById(id).orElseThrow();
        if ("ONGOING".equals(downtime.getStatut())) {
            downtime.setStatut("RESOLVED");
            downtime.setEndTime(LocalDateTime.now());
            return repository.save(downtime);
        }
        return downtime;
    }

    public void deleteDowntime(Integer id) {
        repository.deleteById(id);
    }
    
    public Downtime updateDowntime(Integer id, Downtime updatedDowntime) {
        Downtime downtime = repository.findById(id).orElseThrow();
        downtime.setLigne(updatedDowntime.getLigne());
        downtime.setProduit(updatedDowntime.getProduit());
        downtime.setType(updatedDowntime.getType());
        downtime.setDescription(updatedDowntime.getDescription());
        downtime.setOperateur(updatedDowntime.getOperateur());
        downtime.setSeverity(updatedDowntime.getSeverity());
        downtime.setStatut(updatedDowntime.getStatut());
        downtime.setStartTime(updatedDowntime.getStartTime());
        downtime.setEndTime(updatedDowntime.getEndTime());
        return repository.save(downtime);
    }

    public DowntimeStatsResponse getStats() {
        LocalDateTime now = LocalDateTime.now();
        
        // Today
        long todayDowntime = calculateDowntimeMinutes(LocalDate.now().atStartOfDay(), now);
        // Yesterday
        long yesterdayDowntime = calculateDowntimeMinutes(LocalDate.now().minusDays(1).atStartOfDay(), LocalDate.now().atStartOfDay());
        
        // This Week (last 7 days mapping to "Week")
        long thisWeekDowntime = calculateDowntimeMinutes(now.minusDays(7), now);
        // Last Week
        long lastWeekDowntime = calculateDowntimeMinutes(now.minusDays(14), now.minusDays(7));
        
        // This Month (last 30 days)
        long thisMonthDowntime = calculateDowntimeMinutes(now.minusDays(30), now);
        // Last Month
        long lastMonthDowntime = calculateDowntimeMinutes(now.minusDays(60), now.minusDays(30));

        // Average per day over last 30 days
        long avgToday = thisMonthDowntime / 30;
        long avgPastMonth = lastMonthDowntime / 30;

        return DowntimeStatsResponse.builder()
                .today(formatMinutes(todayDowntime))
                .todayChange(formatChangeString(todayDowntime, yesterdayDowntime))
                .todayIsUp(todayDowntime > yesterdayDowntime)
                
                .thisWeek(formatMinutes(thisWeekDowntime))
                .weekChange(formatChangeString(thisWeekDowntime, lastWeekDowntime))
                .weekIsUp(thisWeekDowntime > lastWeekDowntime)
                
                .thisMonth(formatMinutes(thisMonthDowntime))
                .monthChange(formatChangeString(thisMonthDowntime, lastMonthDowntime))
                .monthIsUp(thisMonthDowntime > lastMonthDowntime)
                
                .avgPerDay(formatMinutes(avgToday))
                .avgChange(formatChangeString(avgToday, avgPastMonth))
                .avgIsUp(avgToday > avgPastMonth)
                .build();
    }

    private long calculateDowntimeMinutes(LocalDateTime from, LocalDateTime to) {
        List<Downtime> downtimes = repository.findOverlapping(from, to);
        
        long totalMinutes = 0;
        for (Downtime d : downtimes) {
            LocalDateTime start = d.getStartTime();
            LocalDateTime end = d.getEndTime() != null ? d.getEndTime() : LocalDateTime.now();
            
            // Intersection with [from, to]
            LocalDateTime intersectStart = start.isAfter(from) ? start : from;
            LocalDateTime intersectEnd = end.isBefore(to) ? end : to;
            
            if (intersectStart.isBefore(intersectEnd)) {
                totalMinutes += Duration.between(intersectStart, intersectEnd).toMinutes();
            }
        }
        return totalMinutes;
    }

    private String formatMinutes(long totalMinutes) {
        long hours = totalMinutes / 60;
        long minutes = totalMinutes % 60;
        if (hours == 0) return minutes + "m";
        return hours + "h " + minutes + "m";
    }

    private String formatChangeString(long current, long previous) {
        if (previous == 0) return current > 0 ? "+100%" : "0%";
        double pct = ((double)(current - previous) / previous) * 100;
        String sign = pct > 0 ? "+" : "";
        return sign + Math.round(pct) + "%";
    }
}
