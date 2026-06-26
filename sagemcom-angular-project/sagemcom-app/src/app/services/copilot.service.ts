import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface CopilotRequest {
  message: string;
}

export interface CopilotResponse {
  reply: string;
}

@Injectable({
  providedIn: 'root'
})
export class CopilotService {
  private apiUrl = '/api/v1/copilot';

  constructor(private http: HttpClient) {}

  sendMessage(message: string): Observable<CopilotResponse> {
    return this.http.post<CopilotResponse>(`${this.apiUrl}/chat`, { message });
  }
}
