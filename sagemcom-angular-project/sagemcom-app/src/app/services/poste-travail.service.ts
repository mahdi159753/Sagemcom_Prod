// src/app/services/poste-travail.service.ts
import { Injectable } from '@angular/core';
import { HttpClient, HttpParams } from '@angular/common/http';
import { Observable } from 'rxjs';

export interface PosteTravailResponse {
  id:               number;
  code:             string;
  libelle:          string;
  ligne:            string;
  type:             'ASSEMBLAGE' | 'TEST' | 'CONTROLE_QUALITE' | 'EMBALLAGE';
  ficheInstruction: string;
  ficheVersion:     string;
  operateur:        string;
  statut:           'ACTIF' | 'INACTIF';
  trg:              number;
  trs:              number;
  fpy:              number;
}

export interface PosteTravailRequest {
  code:             string;
  libelle:          string;
  ligne:            string;
  type:             string;
  ficheInstruction: string;
  ficheVersion:     string;
  operateur:        string;
  statut:           string;
  trg?:             number;
  trs?:             number;
  fpy?:             number;
}

@Injectable({ providedIn: 'root' })
export class PosteTravailService {

  private readonly api = '/api/v1/postes';

  constructor(private http: HttpClient) {}

  // ── READ ──────────────────────────────────────────────────────────────────

  /** Get all postes — available to all authenticated roles */
  getAll(): Observable<PosteTravailResponse[]> {
    return this.http.get<PosteTravailResponse[]>(this.api);
  }

  getById(id: number): Observable<PosteTravailResponse> {
    return this.http.get<PosteTravailResponse>(`${this.api}/${id}`);
  }

  getByLigne(ligne: string): Observable<PosteTravailResponse[]> {
    return this.http.get<PosteTravailResponse[]>(`${this.api}/ligne/${encodeURIComponent(ligne)}`);
  }

  getByStatut(statut: 'ACTIF' | 'INACTIF'): Observable<PosteTravailResponse[]> {
    return this.http.get<PosteTravailResponse[]>(`${this.api}/statut/${statut}`);
  }

  // ── WRITE — ADMIN / RESPONSABLE only ─────────────────────────────────────

  create(dto: PosteTravailRequest): Observable<PosteTravailResponse> {
    return this.http.post<PosteTravailResponse>(this.api, dto);
  }

  update(id: number, dto: PosteTravailRequest): Observable<PosteTravailResponse> {
    return this.http.put<PosteTravailResponse>(`${this.api}/${id}`, dto);
  }

  /** Toggle Actif ↔ Inactif */
  toggleStatut(id: number): Observable<PosteTravailResponse> {
    return this.http.patch<PosteTravailResponse>(`${this.api}/${id}/toggle-statut`, {});
  }

  // ── KPI UPDATE — PREPARATEUR / RESPONSABLE / ADMIN ───────────────────────

  updateKpi(id: number, trg: number, trs: number, fpy: number): Observable<PosteTravailResponse> {
    return this.http.patch<PosteTravailResponse>(`${this.api}/${id}/kpi`, { trg, trs, fpy });
  }

  // ── DELETE — ADMIN only ───────────────────────────────────────────────────

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.api}/${id}`);
  }
}
