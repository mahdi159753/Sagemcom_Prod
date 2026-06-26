import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Downtime, DowntimeStatsResponse } from '../models/models';

@Injectable({ providedIn: 'root' })
export class DowntimeService {
  private apiUrl = '/api/v1/downtimes';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Downtime[]> {
    return this.http.get<Downtime[]>(this.apiUrl);
  }

  declareDowntime(downtime: Downtime): Observable<Downtime> {
    return this.http.post<Downtime>(this.apiUrl, downtime);
  }

  resolveDowntime(id: number): Observable<Downtime> {
    return this.http.put<Downtime>(`${this.apiUrl}/${id}/resolve`, {});
  }

  deleteDowntime(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  getStats(): Observable<DowntimeStatsResponse> {
    return this.http.get<DowntimeStatsResponse>(`${this.apiUrl}/stats`);
  }
}
