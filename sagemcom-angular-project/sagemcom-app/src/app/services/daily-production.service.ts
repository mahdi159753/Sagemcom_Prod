import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

// ── DTOs ─────────────────────────────────────────────────────────
export interface DailyProductionDTO {
  date: string;
  chantier: string;
  ligne: number;
  indicatorName: string;
  value: string;
}

export interface DailyProductionIndicator {
  id?: number;
  date: string;
  chantier: string;
  ligne: number;
  indicatorName: string;
  stringValue: string;
}

// ── KPI Summary types ────────────────────────────────────────────
export interface DailyKpiPoint {
  date: string;
  value: number;
}

export interface TrgHistoryEntry {
  date: string;
  shift: string;
  produced: number;
  target: number;
  trg: number;
  trs: number;
  downtime: string;
}

export interface KpiSummaryResponse {
  trg: number | null;
  trs: number | null;
  disponibilite: number | null;
  performance: number | null;
  qualite: number | null;
  quantiteProduite: number;
  objectifProduction: number;

  trgTrend: number | null;
  disponibiliteTrend: number | null;
  performanceTrend: number | null;
  qualiteTrend: number | null;
  productionTrend: number | null;

  trgHistory: DailyKpiPoint[];
  qualiteHistory: DailyKpiPoint[];
  dispHistory: DailyKpiPoint[];
  perfHistory: DailyKpiPoint[];
  productionHistory: DailyKpiPoint[];

  tableHistory: TrgHistoryEntry[];
}

export interface OeePrediction {
  ligne: number;
  predictedTrg: number;
  rootCauseIndicator: string;
  recommendation: string;
}

@Injectable({
  providedIn: 'root'
})
export class DailyProductionService {

  private apiUrl = '/api/v1/daily-production';

  constructor(private http: HttpClient) { }

  getIndicators(date: string, chantier: string): Observable<DailyProductionIndicator[]> {
    return this.http.get<DailyProductionIndicator[]>(`${this.apiUrl}?date=${date}&chantier=${chantier}`);
  }

  deleteByDateAndChantier(date: string, chantier: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}?date=${date}&chantier=${chantier}`);
  }

  deleteIndicator(date: string, chantier: string, indicatorName: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/indicator?date=${date}&chantier=${chantier}&indicatorName=${encodeURIComponent(indicatorName)}`);
  }

  saveBatch(batch: DailyProductionDTO[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/batch`, batch);
  }

  clearAll(date: string, chantier: string): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}?date=${date}&chantier=${chantier}`);
  }

  getKpiSummary(chantier: string, from: string, to: string): Observable<KpiSummaryResponse> {
    return this.http.get<KpiSummaryResponse>(
      `${this.apiUrl}/kpi-summary?chantier=${chantier}&from=${from}&to=${to}`
    );
  }

  getOeePredictions(chantier: string): Observable<OeePrediction[]> {
    return this.http.get<OeePrediction[]>(`${this.apiUrl}/ai/predict-oee?chantier=${chantier}`);
  }
}
