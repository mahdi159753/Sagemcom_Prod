import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DataService } from '../../services/data.service';
import { ProductionLineService } from '../../services/production-line.service';
import { DailyProductionService, KpiSummaryResponse } from '../../services/daily-production.service';
import { DashboardService, DashboardAlertDTO, DashboardPieChartDTO } from '../../services/dashboard.service';
import { PredictiveAgentService, PredictiveInsight } from '../../services/predictive-agent.service';
import { ProductionLine } from '../../models/models';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.scss']
})
export class DashboardComponent implements OnInit, AfterViewInit {
  @ViewChild('trgCanvas') trgCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('downtimeCanvas') downtimeCanvas!: ElementRef<HTMLCanvasElement>;

  lines: ProductionLine[] = [];
  alerts: DashboardAlertDTO[] = [];
  insights: PredictiveInsight[] = [];
  kpi: KpiSummaryResponse | null = null;

  chantier = 'INTEG';
  chantiers = ['INTEG', 'CMS1', 'CMS2'];

  // Computed display values
  productionValue = '—';
  productionTarget = '—';
  productionPct = 0;
  productionTrend = '';
  productionTrendClass = '';

  trgValue = '—';
  trgPct = 0;
  trgTrend = '';
  trgTrendClass = '';

  currentDate = '';
  currentTime = '';

  downtimeLegend: DashboardPieChartDTO[] = [];

  timelineBlocks = [
    { left: 0,     width: 27.8, color: '#28A745', label: 'Production: 150 units' },
    { left: 27.8,  width: 8.3,  color: '#DC3545', label: 'Machine Failure' },
    { left: 36.1,  width: 30.6, color: '#28A745', label: 'Production: 280 units' },
    { left: 66.7,  width: 5.6,  color: '#FFC107', label: 'Series Changeover' },
    { left: 72.3,  width: 27.7, color: '#28A745', label: 'Production: 200 units' },
  ];
  currentTimePos = 72.8;
  tooltipVisible: boolean[] = new Array(5).fill(false);

  private trgChart: Chart | null = null;
  private chartsReady = false;

  constructor(
    private data: DataService,
    private lineService: ProductionLineService,
    private dailyProdService: DailyProductionService,
    private dashboardService: DashboardService,
    private predictiveService: PredictiveAgentService
  ) {}

  ngOnInit() {
    // Static date/time
    const now = new Date();
    this.currentDate = now.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
    this.currentTime = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });

    this.lineService.getAll().subscribe({
      next: (data) => this.lines = data,
      error: (err) => console.error(err)
    });
    
    // Fetch dynamic alerts
    this.dashboardService.getAlerts().subscribe({
      next: (data) => this.alerts = data,
      error: (err) => console.error(err)
    });

    // Fetch AI Predictive Insights
    this.predictiveService.getPredictiveInsights().subscribe({
      next: (data) => this.insights = data,
      error: (err) => console.error('Predictive Agent Error', err)
    });

    this.loadKpiData();
  }

  ngAfterViewInit() {
    this.chartsReady = true;
    this.buildDowntimeChart();
    if (this.kpi) this.buildTrgChart();
  }

  onChantierChange() {
    this.loadKpiData();
  }

  private loadKpiData() {
    const today = new Date();
    const to = today.toISOString().split('T')[0];
    const from7 = new Date(today);
    from7.setDate(from7.getDate() - 7);
    const from = from7.toISOString().split('T')[0];

    this.dailyProdService.getKpiSummary(this.chantier, from, to).subscribe({
      next: (data) => {
        this.kpi = data;
        this.updateDisplayValues(data);
        if (this.chartsReady) this.buildTrgChart();
      },
      error: (err) => console.error('Dashboard KPI error', err)
    });
  }

  private updateDisplayValues(kpi: KpiSummaryResponse) {
    // Production card
    this.productionValue = kpi.quantiteProduite ? kpi.quantiteProduite.toLocaleString() : '0';
    this.productionTarget = kpi.objectifProduction ? kpi.objectifProduction.toLocaleString() : '—';
    this.productionPct = kpi.objectifProduction > 0
      ? Math.min(100, (kpi.quantiteProduite / kpi.objectifProduction) * 100)
      : (kpi.quantiteProduite > 0 ? 100 : 0);

    if (kpi.productionTrend != null) {
      const sign = kpi.productionTrend > 0 ? '+' : '';
      this.productionTrend = `${kpi.productionTrend > 0 ? '▲' : '▼'} ${sign}${kpi.productionTrend}%`;
      this.productionTrendClass = kpi.productionTrend >= 0 ? 'text-success' : 'text-danger';
    } else {
      this.productionTrend = '';
      this.productionTrendClass = 'text-muted';
    }

    // TRG card
    this.trgValue = kpi.trg != null ? kpi.trg.toFixed(1) : '—';
    this.trgPct = kpi.trg ?? 0;
    if (kpi.trgTrend != null) {
      const sign = kpi.trgTrend > 0 ? '+' : '';
      this.trgTrend = `${kpi.trgTrend > 0 ? '▲' : '▼'} ${sign}${kpi.trgTrend}%`;
      this.trgTrendClass = kpi.trgTrend >= 0 ? 'text-success' : 'text-danger';
    } else {
      this.trgTrend = '';
      this.trgTrendClass = 'text-muted';
    }
  }

  private buildTrgChart() {
    this.trgChart?.destroy();

    const history = this.kpi?.trgHistory || [];
    const labels = history.map(p => p.date.substring(5)); // "04-01"
    const values = history.map(p => p.value);

    this.trgChart = new Chart(this.trgCanvas.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          label: `TRG — ${this.chantier}`,
          data: values,
          borderColor: '#2E75B6',
          backgroundColor: 'rgba(46,117,182,.1)',
          tension: .3,
          fill: true,
          pointRadius: 4,
          borderWidth: 3
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { labels: { font: { family: 'IBM Plex Sans', size: 12 } } } },
        scales: {
          y: { min: 0, max: 100, grid: { color: '#F1F5F9' } },
          x: { grid: { display: false } }
        }
      }
    });
  }

  private buildDowntimeChart() {
    this.dashboardService.getDowntimePie().subscribe({
      next: (data) => {
        if (data.length === 0) {
          // If no data, show empty donut
          new Chart(this.downtimeCanvas.nativeElement, {
            type: 'doughnut',
            data: { labels: ['No Downtime'], datasets: [{ data: [100], backgroundColor: ['#CBD5E1'] }] },
            options: { responsive: false, plugins: { legend: { display: false } }, cutout: '62%' }
          });
          this.downtimeLegend = [];
          return;
        }

        const labels = data.map(d => d.label);
        const values = data.map(d => d.value);
        const colors = data.map(d => d.color);

        this.downtimeLegend = data;

        new Chart(this.downtimeCanvas.nativeElement, {
          type: 'doughnut',
          data: { labels, datasets: [{ data: values, backgroundColor: colors, borderWidth: 0 }] },
          options: {
            responsive: false,
            plugins: { legend: { display: false } },
            cutout: '62%'
          }
        });
      },
      error: (e) => console.error(e)
    });
  }

  getStatusClass(status: string): string {
    return { production: 'badge-green', stopped: 'badge-red', changeover: 'badge-yellow' }[status] ?? 'badge-gray';
  }
  getStatusDot(status: string): string {
    return { production: '#28A745', stopped: '#DC3545', changeover: '#FFC107' }[status] ?? '#94A3B8';
  }
  getTrgFill(trg: number): string {
    if (trg >= 80) return '#28A745';
    if (trg >= 60) return '#FFC107';
    return '#DC3545';
  }
  getPerformanceLabel(p: string): string {
    return { good: '▲ Good', critical: '⚠ Critical', low: '▼ Low', normal: '→ Normal' }[p] ?? p;
  }
  getPerformanceColor(p: string): string {
    return { good: '#28A745', critical: '#DC3545', low: '#DC3545', normal: '#94A3B8' }[p] ?? '#94A3B8';
  }
}
