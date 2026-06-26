import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PredictiveInsight {
  ligne: string;
  probability: number;
  likely_type: string;
  estimated_time_to_failure_hours: number;
  recommended_action: string;
}

@Injectable({
  providedIn: 'root'
})
export class PredictiveAgentService {

  private apiUrl = '/api/v1/downtimes/predict';

  constructor(private http: HttpClient) { }

  getPredictiveInsights(): Observable<PredictiveInsight[]> {
    return this.http.get<PredictiveInsight[]>(this.apiUrl);
  }
}
