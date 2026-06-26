import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { FiDocument, InstructionStep, ControlSession, ControlStepValidation, SessionReport } from '../models/models';

@Injectable({ providedIn: 'root' })
export class FiGuideService {
  private http = inject(HttpClient);
  private baseUrl = '/api/v1/fi-guide';

  uploadFiche(produitId: number, version: string, file: File): Observable<FiDocument> {
    const formData = new FormData();
    formData.append('produitId', produitId.toString());
    formData.append('version', version);
    formData.append('file', file);
    return this.http.post<FiDocument>(`${this.baseUrl}/upload`, formData);
  }

  getActiveInstruction(produitId: number): Observable<{ document: FiDocument | null; steps: InstructionStep[] }> {
    return this.http.get<{ document: FiDocument | null; steps: InstructionStep[] }>(
      `${this.baseUrl}/produit/${produitId}/active`
    );
  }

  startSession(produitId: number): Observable<ControlSession> {
    return this.http.post<ControlSession>(`${this.baseUrl}/session/start?produitId=${produitId}`, {});
  }

  validateStep(
    sessionId: number,
    stepId: number,
    status: 'CONFORME' | 'NON_CONFORME',
    comment?: string
  ): Observable<ControlStepValidation> {
    let url = `${this.baseUrl}/session/${sessionId}/validate-step?stepId=${stepId}&status=${status}`;
    if (comment) {
      url += `&comment=${encodeURIComponent(comment)}`;
    }
    return this.http.post<ControlStepValidation>(url, {});
  }

  completeSession(sessionId: number): Observable<ControlSession> {
    return this.http.post<ControlSession>(`${this.baseUrl}/session/${sessionId}/complete`, {});
  }

  getSessionReport(sessionId: number): Observable<SessionReport> {
    return this.http.get<SessionReport>(`${this.baseUrl}/session/${sessionId}/report`);
  }

  getAllSessions(): Observable<ControlSession[]> {
    return this.http.get<ControlSession[]>(`${this.baseUrl}/sessions`);
  }

  // ── Image Assignment ──────────────────────────────────────────────────

  getExtractedImages(docId: number): Observable<string[]> {
    return this.http.get<string[]>(`${this.baseUrl}/documents/${docId}/extracted-images`);
  }

  assignImage(stepId: number, imageUrl: string): Observable<InstructionStep> {
    return this.http.post<InstructionStep>(`${this.baseUrl}/steps/${stepId}/assign-image`, { imageUrl });
  }

  removeImage(stepId: number, imageUrl: string): Observable<InstructionStep> {
    return this.http.post<InstructionStep>(`${this.baseUrl}/steps/${stepId}/remove-image`, { imageUrl });
  }

  deleteSession(sessionId: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/session/${sessionId}`);
  }
}
