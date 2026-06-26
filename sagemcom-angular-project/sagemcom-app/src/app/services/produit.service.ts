import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Produit } from '../models/models';

@Injectable({ providedIn: 'root' })
export class ProduitService {
  private baseUrl = '/api/v1/produits';

  constructor(private http: HttpClient) {}

  getAll(): Observable<Produit[]> {
    return this.http.get<Produit[]>(this.baseUrl);
  }

  getById(id: number): Observable<Produit> {
    return this.http.get<Produit>(`${this.baseUrl}/${id}`);
  }

  create(produit: Produit, file?: File): Observable<Produit> {
    const formData = new FormData();
    formData.append('produit', new Blob([JSON.stringify(produit)], {
      type: 'application/json'
    }));
    
    if (file) {
      formData.append('file', file);
    }
    
    return this.http.post<Produit>(this.baseUrl, formData);
  }

  update(id: number, produit: Produit, file?: File): Observable<Produit> {
    const formData = new FormData();
    formData.append('produit', new Blob([JSON.stringify(produit)], {
      type: 'application/json'
    }));
    
    if (file) {
      formData.append('file', file);
    }

    return this.http.put<Produit>(`${this.baseUrl}/${id}`, formData);
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.baseUrl}/${id}`);
  }

  getDownloadUrl(fileName: string): string {
    return `${this.baseUrl}/download/${fileName}`;
  }

  downloadFile(fileName: string): Observable<Blob> {
    return this.http.get(`${this.baseUrl}/download/${fileName}`, {
      responseType: 'blob'
    });
  }
}
