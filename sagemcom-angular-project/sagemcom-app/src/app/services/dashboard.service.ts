import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface DashboardAlertDTO {
  id: string;
  title: string;
  description: string;
  severity: string;
  time: string;
}

export interface DashboardPieChartDTO {
  label: string;
  value: number;
  color: string;
}

@Injectable({
  providedIn: 'root'
})
export class DashboardService {
  private apiUrl = '/api/v1/dashboard';

  constructor(private http: HttpClient) {}

  getAlerts(): Observable<DashboardAlertDTO[]> {
    return this.http.get<DashboardAlertDTO[]>(`${this.apiUrl}/alerts`);
  }

  getDowntimePie(): Observable<DashboardPieChartDTO[]> {
    return this.http.get<DashboardPieChartDTO[]>(`${this.apiUrl}/downtime-pie`);
  }
}
