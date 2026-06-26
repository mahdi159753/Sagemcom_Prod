import { Injectable, inject } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { ProductionLine } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ProductionLineService {

  private readonly API = '/api/v1/lignes';
  private http = inject(HttpClient);

  getAll(): Observable<ProductionLine[]> {
    return this.http.get<ProductionLine[]>(this.API);
  }

  getById(id: number): Observable<ProductionLine> {
    return this.http.get<ProductionLine>(`${this.API}/${id}`);
  }

  getByStatut(statut: string): Observable<ProductionLine[]> {
    return this.http.get<ProductionLine[]>(`${this.API}/statut/${statut}`);
  }

  getByChantier(chantier: string): Observable<ProductionLine[]> {
    return this.http.get<ProductionLine[]>(`${this.API}/chantier/${chantier}`);
  }

  create(line: any): Observable<ProductionLine> {
    return this.http.post<ProductionLine>(this.API, line);
  }

  update(id: number, line: any): Observable<ProductionLine> {
    return this.http.put<ProductionLine>(`${this.API}/${id}`, line);
  }

  changeStatut(id: number, statut: string, cause?: string): Observable<ProductionLine> {
    return this.http.patch<ProductionLine>(`${this.API}/${id}/statut`, { statut, cause });
  }

  updateKpi(id: number, kpi: any): Observable<ProductionLine> {
    return this.http.patch<ProductionLine>(`${this.API}/${id}/kpi`, kpi);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.API}/${id}`);
  }
}
