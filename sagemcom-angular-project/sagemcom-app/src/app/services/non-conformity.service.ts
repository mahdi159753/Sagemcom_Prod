import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { NonConformity } from '../models/models';

@Injectable({
  providedIn: 'root'
})
export class NonConformityService {
  private apiUrl = '/api/v1/nc';
  private http = inject(HttpClient);

  getAll(): Observable<NonConformity[]> {
    return this.http.get<NonConformity[]>(this.apiUrl);
  }

  getById(id: number): Observable<NonConformity> {
    return this.http.get<NonConformity>(`${this.apiUrl}/${id}`);
  }

  create(nc: NonConformity, file?: File): Observable<NonConformity> {
    const formData = new FormData();
    formData.append('nc', new Blob([JSON.stringify(nc)], { type: 'application/json' }));
    if (file) {
      formData.append('file', file);
    }
    return this.http.post<NonConformity>(this.apiUrl, formData);
  }

  update(id: number, nc: NonConformity, file?: File): Observable<NonConformity> {
    const formData = new FormData();
    formData.append('nc', new Blob([JSON.stringify(nc)], { type: 'application/json' }));
    if (file) {
      formData.append('file', file);
    }
    return this.http.put<NonConformity>(`${this.apiUrl}/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`);
  }

  downloadFile(fileName: string): Observable<Blob> {
    return this.http.get(`${this.apiUrl}/download/${fileName}`, { responseType: 'blob' });
  }

  assign(id: number, assigneeEmail: string): Observable<NonConformity> {
    return this.http.post<NonConformity>(`${this.apiUrl}/${id}/assign`, { assigneeEmail });
  }

  treat(id: number, actionCorrective: string): Observable<NonConformity> {
    return this.http.post<NonConformity>(`${this.apiUrl}/${id}/treat`, { actionCorrective });
  }

  validateAndClose(id: number): Observable<NonConformity> {
    return this.http.post<NonConformity>(`${this.apiUrl}/${id}/validate`, {});
  }

  analyzeNC(description: string, localisation: string): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/analyze`, { description, localisation });
  }
}
