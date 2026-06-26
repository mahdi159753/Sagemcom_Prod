import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DailyProductionService, KpiSummaryResponse, OeePrediction } from '../../services/daily-production.service';
import { KpiMetric } from '../../models/models';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-kpi-dashboard',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './kpi-dashboard.component.html',
  styleUrls: ['./kpi-dashboard.component.scss']
})
export class KpiDashboardComponent implements OnInit, AfterViewInit {
  @ViewChild('prodCanvas')    prodCanvas!:    ElementRef<HTMLCanvasElement>;
  @ViewChild('qualityCanvas') qualityCanvas!: ElementRef<HTMLCanvasElement>;

  metrics: KpiMetric[] = [];
  period = 'month';
  chantier = 'INTEG';
  chantiers = ['INTEG', 'CMS1', 'CMS2'];
  kpi: KpiSummaryResponse | null = null;
  oeePredictions: OeePrediction[] = [];

  private prodChart: Chart | null = null;
  private qualChart: Chart | null = null;
  private chartsReady = false;

  selectedMetric: KpiMetric | null = null;

  // Summary counts
  aboveTarget = 0;
  belowTarget = 0;
  critical = 0;
  aboveNames = '';
  belowNames = '';
  criticalNames = '';

  constructor(private dailyProdService: DailyProductionService) {}

  ngOnInit()       { this.loadData(); }

  ngAfterViewInit() {
    this.chartsReady = true;
    if (this.kpi) this.renderCharts();
  }

  onFilterChange() {
    this.loadData();
  }

  private loadData() {
    const { from, to } = this.getDateRange();
    this.dailyProdService.getKpiSummary(this.chantier, from, to).subscribe({
      next: (data) => {
        this.kpi = data;
        this.buildMetrics(data);
        if (this.chartsReady) this.renderCharts();
      },
      error: (err) => console.error('KPI Dashboard error', err)
    });

    this.dailyProdService.getOeePredictions(this.chantier).subscribe({
      next: (preds) => {
        this.oeePredictions = preds;
      },
      error: (err) => console.error('Error loading AI predictions', err)
    });
  }

  private getDateRange(): { from: string; to: string } {
    const today = new Date();
    const to = today.toISOString().split('T')[0];
    let days = 30;
    if (this.period === 'quarter') days = 90;
    else if (this.period === 'half') days = 180;
    else if (this.period === 'year') days = 365;
    const d = new Date(today);
    d.setDate(d.getDate() - days);
    return { from: d.toISOString().split('T')[0], to };
  }

  private buildMetrics(kpi: KpiSummaryResponse) {
    this.metrics = [];

    // OEE (TRG)
    const oee: KpiMetric = {
      title: 'Overall Equipment Effectiveness (OEE)',
      value: kpi.trg ?? 0,
      target: 85,
      trend: kpi.trgTrend ?? 0,
      unit: '%',
      status: this.getMetricStatus(kpi.trg ?? 0, 85)
    };
    this.metrics.push(oee);

    // First Pass Yield (Qualité)
    const fpy: KpiMetric = {
      title: 'First Pass Yield',
      value: kpi.qualite != null ? Math.round(kpi.qualite * 10) / 10 : 0,
      target: 95,
      trend: kpi.qualiteTrend ?? 0,
      unit: '%',
      status: this.getMetricStatus(kpi.qualite ?? 0, 95)
    };
    this.metrics.push(fpy);

    // Disponibilité
    const disp: KpiMetric = {
      title: 'Availability (Disponibilité)',
      value: kpi.disponibilite != null ? Math.round(kpi.disponibilite * 10) / 10 : 0,
      target: 90,
      trend: kpi.disponibiliteTrend ?? 0,
      unit: '%',
      status: this.getMetricStatus(kpi.disponibilite ?? 0, 90)
    };
    this.metrics.push(disp);

    // Performance
    const perf: KpiMetric = {
      title: 'Performance Rate',
      value: kpi.performance != null ? Math.round(kpi.performance * 10) / 10 : 0,
      target: 95,
      trend: kpi.performanceTrend ?? 0,
      unit: '%',
      status: this.getMetricStatus(kpi.performance ?? 0, 95)
    };
    this.metrics.push(perf);

    // TRS
    const trs: KpiMetric = {
      title: 'TRS (Overall OEE)',
      value: kpi.trs != null ? Math.round(kpi.trs * 10) / 10 : 0,
      target: 75,
      trend: 0,
      unit: '%',
      status: this.getMetricStatus(kpi.trs ?? 0, 75)
    };
    this.metrics.push(trs);

    // Production Volume
    const prod: KpiMetric = {
      title: 'Production Volume',
      value: kpi.quantiteProduite ?? 0,
      target: kpi.objectifProduction > 0 ? kpi.objectifProduction : 20000,
      trend: kpi.productionTrend ?? 0,
      unit: 'units',
      status: this.getMetricStatus(kpi.quantiteProduite ?? 0, kpi.objectifProduction > 0 ? kpi.objectifProduction : 20000)
    };
    this.metrics.push(prod);

    // Compute summary
    this.aboveTarget = this.metrics.filter(m => m.value >= m.target).length;
    this.belowTarget = this.metrics.filter(m => m.value < m.target && m.status !== 'critical').length;
    this.critical = this.metrics.filter(m => m.status === 'critical').length;
    this.aboveNames = this.metrics.filter(m => m.value >= m.target).map(m => m.title.split('(')[0].trim()).join(', ') || 'None';
    this.belowNames = this.metrics.filter(m => m.value < m.target && m.status !== 'critical').map(m => m.title.split('(')[0].trim()).join(', ') || 'None';
    this.criticalNames = this.metrics.filter(m => m.status === 'critical').map(m => m.title.split('(')[0].trim()).join(', ') || 'No critical metrics';
  }

  private getMetricStatus(value: number, target: number): 'excellent' | 'good' | 'warning' | 'critical' {
    const ratio = target > 0 ? value / target : 0;
    if (ratio >= 1) return 'excellent';
    if (ratio >= 0.9) return 'good';
    if (ratio >= 0.7) return 'warning';
    return 'critical';
  }

  private renderCharts() {
    this.prodChart?.destroy();
    this.qualChart?.destroy();

    // Production history chart
    const prodHist = this.kpi?.productionHistory || [];
    this.prodChart = new Chart(this.prodCanvas.nativeElement, {
      type: 'bar',
      data: {
        labels: prodHist.map(p => p.date.substring(5)),
        datasets: [{
          label: 'Production',
          data: prodHist.map(p => p.value),
          backgroundColor: '#0052CC',
          borderRadius: 4,
          barPercentage: 0.6
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { labels: { color: '#475569', font: { family: 'Inter, sans-serif' } } } },
        scales: { 
          y: { grid: { color: '#E2E8F0' }, ticks: { color: '#64748B' } }, 
          x: { grid: { display: false }, ticks: { color: '#64748B' } } 
        }
      }
    });

    // Quality history chart
    const qualHist = this.kpi?.qualiteHistory || [];
    this.qualChart = new Chart(this.qualityCanvas.nativeElement, {
      type: 'line',
      data: {
        labels: qualHist.map(p => p.date.substring(5)),
        datasets: [{
          label: 'Quality Rate (%)',
          data: qualHist.map(p => p.value),
          borderColor: '#059669',
          backgroundColor: 'rgba(5, 150, 105, 0.1)',
          tension: 0.4, 
          fill: true, 
          pointRadius: 0, 
          pointHoverRadius: 6,
          borderWidth: 3
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { labels: { color: '#475569', font: { family: 'Inter, sans-serif' } } } },
        scales: {
          y: { min: 90, max: 100, grid: { color: '#E2E8F0' }, ticks: { color: '#64748B' } },
          x: { grid: { display: false }, ticks: { color: '#64748B' } }
        }
      }
    });
  }

  getStatusColor(s: string): string {
    return { excellent:'#059669', good:'#059669', warning:'#D97706', critical:'#DC2626' }[s] ?? '#64748B';
  }
  getProgressFill(s: string): string { return this.getStatusColor(s); }
  getProgress(m: KpiMetric): number {
    if (m.unit === '%') return Math.min(100,(m.value / m.target) * 100);
    return m.value >= m.target ? 100 : (m.value / m.target) * 100;
  }
  getTrendSign(t: number): string { return t > 0 ? '+' : ''; }
  getTrendColor(t: number): string { return t > 0 ? '#059669' : t < 0 ? '#DC2626' : '#64748B'; }
  getTrendArrow(t: number): string { return t > 0 ? '▲' : t < 0 ? '▼' : '—'; }

  openMetricDetails(m: KpiMetric) {
    this.selectedMetric = m;
  }

  closeMetricDetails() {
    this.selectedMetric = null;
  }
}
