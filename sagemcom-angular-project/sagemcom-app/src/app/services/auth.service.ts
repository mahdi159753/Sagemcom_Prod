// src/app/services/auth.service.ts
import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, tap } from 'rxjs';
import { Role, AuthResponse } from '../models/models';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private baseUrl = '/api/v1/auth';
  private usersUrl = '/api/v1/users';

  constructor(private http: HttpClient) {}

  // ── User CRUD ─────────────────────────────────────────────────────────────
  getAll(): Observable<any[]> {
    return this.http.get<any[]>(this.usersUrl);
  }

  /**
   * Create user via /register so the backend hashes the password.
   * A temporary password is set; the user should change it on first login.
   */
  create(user: {
    firstname: string;
    lastname: string;
    email: string;
    role: string;
    poste?: string;
    matricule?: string;
  }): Observable<any> {
    const payload = { ...user, password: 'Temp@1234' };
    return this.http.post(`${this.baseUrl}/register`, payload);
  }

  update(id: number, user: any): Observable<any> {
    return this.http.put(`${this.usersUrl}/${id}`, user);
  }

  delete(id: number): Observable<any> {
    return this.http.delete(`${this.usersUrl}/${id}`);
  }

  pingActivity(): Observable<any> {
    return this.http.post(`${this.usersUrl}/ping`, {});
  }

  // ── Login ─────────────────────────────────────────────────────────────────
  login(email: string, password: string): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/authenticate`, { email, password }).pipe(
      tap(res => this.storeSession(res))
    );
  }

  // ── Register ──────────────────────────────────────────────────────────────
  register(
    firstname: string, lastname: string, email: string,
    password: string, role: string, poste: string, matricule: string
  ): Observable<AuthResponse> {
    return this.http.post<AuthResponse>(`${this.baseUrl}/register`,
      { firstname, lastname, email, password, role, poste, matricule }
    ).pipe(tap(res => this.storeSession(res)));
  }

  // ── Store session ─────────────────────────────────────────────────────────
  storeSession(res: AuthResponse): void {
    localStorage.setItem('access_token',  res.access_token);
    localStorage.setItem('refresh_token', res.refresh_token);
    if (res.id)         localStorage.setItem('user_id',        res.id.toString());
    if (res.role)       localStorage.setItem('user_role',      res.role);
    if (res.firstname)  localStorage.setItem('user_firstname', res.firstname);
    if (res.lastname)   localStorage.setItem('user_lastname',  res.lastname);
    if (res.email)      localStorage.setItem('user_email',     res.email);
  }

  setTokens(access: string, refresh: string): void {
    localStorage.setItem('access_token',  access);
    localStorage.setItem('refresh_token', refresh);
  }

  getToken(): string | null        { return localStorage.getItem('access_token'); }
  getUserId(): number | null       { const id = localStorage.getItem('user_id'); return id ? parseInt(id, 10) : null; }
  getRole(): Role | null           { return localStorage.getItem('user_role') as Role | null; }
  getFirstname(): string | null    { return localStorage.getItem('user_firstname'); }
  getLastname(): string | null     { return localStorage.getItem('user_lastname'); }
  getUserEmail(): string | null    { return localStorage.getItem('user_email'); }
  isLoggedIn(): boolean            { return !!this.getToken(); }

  isAdmin(): boolean                  { return this.getRole() === 'ADMIN'; }
  isResponsable(): boolean            { return this.getRole() === 'RESPONSABLE_PRODUCTION'; }
  isIngenieur(): boolean              { return this.getRole() === 'INGENIEUR_QUALITE'; }
  isPreparateur(): boolean            { return this.getRole() === 'PREPARATEUR'; }

  hasRole(...roles: Role[]): boolean  { const r = this.getRole(); return !!r && roles.includes(r); }

  logout(): void {
    this.http.post(`${this.baseUrl}/logout`, {}).subscribe({
      next: () => this.clearSession(),
      error: () => this.clearSession()
    });
  }

  private clearSession(): void {
    ['access_token','refresh_token','user_id','user_role','user_firstname','user_lastname','user_email']
      .forEach(k => localStorage.removeItem(k));
  }
}
