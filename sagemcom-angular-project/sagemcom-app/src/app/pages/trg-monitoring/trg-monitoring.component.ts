import { Component, OnInit, AfterViewInit, ElementRef, ViewChild } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DailyProductionService, KpiSummaryResponse, TrgHistoryEntry } from '../../services/daily-production.service';
import Chart from 'chart.js/auto';

@Component({
  selector: 'app-trg-monitoring',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './trg-monitoring.component.html',
  styleUrls: ['./trg-monitoring.component.scss']
})
export class TrgMonitoringComponent implements OnInit, AfterViewInit {
  @ViewChild('availCanvas') availCanvas!: ElementRef<HTMLCanvasElement>;
  @ViewChild('perfCanvas')  perfCanvas!:  ElementRef<HTMLCanvasElement>;
  @ViewChild('qualCanvas')  qualCanvas!:  ElementRef<HTMLCanvasElement>;
  @ViewChild('mainChartCanvas') mainChartCanvas!: ElementRef<HTMLCanvasElement>;

  dateRange = '7days';
  chantier = 'INTEG';
  chantiers = ['INTEG', 'CMS1', 'CMS2'];

  history: TrgHistoryEntry[] = [];
  kpi: KpiSummaryResponse | null = null;

  // Gauge
  trgValue = 0;
  gaugeStrokeDasharray = '0 691.15';
  gaugeStrokeDashoffset = 0;

  // Charts
  private availChart: Chart | null = null;
  private perfChart: Chart | null = null;
  private qualChart: Chart | null = null;
  private mainChart: Chart | null = null;
  private chartsReady = false;

  constructor(private dailyProdService: DailyProductionService) {}

  ngOnInit() {
    this.loadData();
  }

  ngAfterViewInit() {
    this.chartsReady = true;
    if (this.kpi) this.renderCharts();
  }

  onFilterChange() {
    this.loadData();
  }

  loadData() {
    const { from, to } = this.getDateRange();
    this.dailyProdService.getKpiSummary(this.chantier, from, to).subscribe({
      next: (data) => {
        this.kpi = data;
        this.history = data.tableHistory || [];
        this.trgValue = data.trg ?? 0;
        this.updateGauge();
        if (this.chartsReady) this.renderCharts();
      },
      error: (err) => console.error('KPI Summary error', err)
    });
  }

  private getDateRange(): { from: string; to: string } {
    const today = new Date();
    const to = today.toISOString().split('T')[0];
    let from: string;
    if (this.dateRange === 'today') {
      from = to;
    } else if (this.dateRange === '30days') {
      const d = new Date(today);
      d.setDate(d.getDate() - 30);
      from = d.toISOString().split('T')[0];
    } else {
      // default 7days
      const d = new Date(today);
      d.setDate(d.getDate() - 7);
      from = d.toISOString().split('T')[0];
    }
    return { from, to };
  }

  private updateGauge() {
    const circumference = 2 * Math.PI * 110;
    const fraction = Math.min(1, Math.max(0, this.trgValue / 100));
    this.gaugeStrokeDasharray = `${circumference * fraction} ${circumference}`;
    this.gaugeStrokeDashoffset = circumference * 0.25;
  }

  private renderCharts() {
    // Destroy previous
    this.availChart?.destroy();
    this.perfChart?.destroy();
    this.qualChart?.destroy();
    this.mainChart?.destroy();

    const dispData = this.kpi?.dispHistory || [];
    const perfData = this.kpi?.perfHistory || [];
    const qualData = this.kpi?.qualiteHistory || [];

    this.availChart = this.buildMiniChart(this.availCanvas, dispData.map(p => p.value), '#2E75B6');
    this.perfChart  = this.buildMiniChart(this.perfCanvas,  perfData.map(p => p.value), '#DC3545');
    this.qualChart  = this.buildMiniChart(this.qualCanvas,  qualData.map(p => p.value), '#28A745');

    this.renderMainChart();
  }

  private renderMainChart() {
    if (!this.mainChartCanvas || this.history.length === 0) return;

    // Sort history by date oldest to newest
    const sortedHistory = [...this.history].sort((a, b) => new Date(a.date).getTime() - new Date(b.date).getTime());
    
    const labels = sortedHistory.map(h => h.date);
    const trgData = sortedHistory.map(h => h.trg);
    const trsData = sortedHistory.map(h => h.trs);

    // Calculate Y min
    let minVal = Math.min(...trgData, ...trsData);
    if (isNaN(minVal) || !isFinite(minVal)) minVal = 0;
    const yMin = Math.max(0, minVal - 10);

    this.mainChart = new Chart(this.mainChartCanvas.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [
          {
            label: 'TRG %',
            data: trgData,
            borderColor: '#3b82f6',
            backgroundColor: 'rgba(59, 130, 246, 0.1)',
            borderWidth: 3,
            tension: 0.4,
            fill: true,
            pointBackgroundColor: '#3b82f6',
            pointRadius: 4,
            pointHoverRadius: 6
          },
          {
            label: 'TRS %',
            data: trsData,
            borderColor: '#8b5cf6',
            backgroundColor: 'transparent',
            borderWidth: 2,
            borderDash: [5, 5],
            tension: 0.4,
            fill: false,
            pointBackgroundColor: '#8b5cf6',
            pointRadius: 3
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        interaction: { mode: 'index', intersect: false },
        plugins: {
          legend: { position: 'top', labels: { usePointStyle: true, boxWidth: 8, font: { family: 'Inter', weight: 'bold' } } },
          tooltip: { backgroundColor: 'rgba(15, 23, 42, 0.9)', titleFont: { size: 13, family: 'Inter' }, bodyFont: { size: 13, family: 'Inter' }, padding: 12, cornerRadius: 8 }
        },
        scales: {
          y: { min: yMin, max: 100, grid: { color: 'rgba(0,0,0,0.04)' } },
          x: { grid: { display: false } }
        }
      }
    });
  }

  private buildMiniChart(ref: ElementRef<HTMLCanvasElement>, data: number[], color: string): Chart {
    const labels = data.map(() => '');
    return new Chart(ref.nativeElement, {
      type: 'line',
      data: {
        labels,
        datasets: [{
          data,
          borderColor: color,
          backgroundColor: color.replace(')', ',.15)').replace('rgb', 'rgba'),
          tension: .4, fill: true, pointRadius: 0, borderWidth: 2
        }]
      },
      options: {
        responsive: true,
        plugins: { legend: { display: false } },
        scales: { x: { display: false }, y: { display: false } },
        animation: false
      }
    });
  }

  getTrgBadgeColor(trg: number | null | undefined): string {
    if (trg == null) return '#64748B'; // Default gray for null
    if (trg >= 80) return '#28A745';
    if (trg >= 60) return '#FFC107';
    return '#DC3545';
  }

  getTrendArrow(val: number | null | undefined): string {
    if (val == null) return '';
    return val > 0 ? '▲' : val < 0 ? '▼' : '—';
  }

  getTrendClass(val: number | null | undefined): string {
    if (val == null) return 'text-muted';
    return val > 0 ? 'text-success' : val < 0 ? 'text-danger' : 'text-muted';
  }

  formatTrend(val: number | null | undefined): string {
    if (val == null) return 'N/A';
    const sign = val > 0 ? '+' : '';
    return `${sign}${val}%`;
  }

  exportToExcel() {
    if (!this.history || this.history.length === 0) {
      alert("No data available to export.");
      return;
    }
    
    // Create CSV content
    const headers = ['Date', 'Shift', 'Units Produced', 'TRG (%)', 'TRS (%)', 'Downtime Status'];
    const rows = this.history.map(row => [
      row.date,
      row.shift,
      row.produced,
      row.trg,
      row.trs,
      `"${(row.downtime || '').replace(/"/g, '""')}"` // Escape quotes for CSV
    ]);
    
    let csvContent = headers.join(',') + '\n';
    rows.forEach(rowArray => {
      csvContent += rowArray.join(',') + '\n';
    });

    // Download CSV
    const blob = new Blob([csvContent], { type: 'text/csv;charset=utf-8;' });
    const url = URL.createObjectURL(blob);
    const link = document.createElement('a');
    link.setAttribute('href', url);
    link.setAttribute('download', `TRG_Report_${this.chantier}_${new Date().toISOString().slice(0,10)}.csv`);
    document.body.appendChild(link);
    link.click();
    document.body.removeChild(link);
  }

  exportToPdf() {
    // Uses the browser's native print-to-pdf functionality
    // (CSS @media print rules can be added to format this cleanly)
    window.print();
  }
}
