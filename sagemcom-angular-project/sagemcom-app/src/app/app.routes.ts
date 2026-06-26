// src/app/app.routes.ts
import { Routes } from '@angular/router';
import { LayoutComponent }  from './components/layout/layout.component';
import { LoginComponent }   from './pages/login/login.component';
import { RegisterComponent } from './pages/register/register.component';
import { AuthGuard }  from './guards/auth.guard';
import { RoleGuard }  from './guards/role.guard';

export const routes: Routes = [
  { path: 'login',    component: LoginComponent },
  { path: 'register', component: RegisterComponent },

  {
    path: '',
    component: LayoutComponent,
    canActivate: [AuthGuard],
    children: [
      { path: '', redirectTo: 'dashboard', pathMatch: 'full' },

      // ── ALL authenticated users ─────────────────────────────────────────
      {
        path: 'dashboard',
        loadComponent: () => import('./pages/dashboard/dashboard.component').then(m => m.DashboardComponent)
      },
      {
        path: 'chat',
        loadComponent: () => import('./pages/chat/chat.component').then(m => m.ChatComponent)
      },
      {
        path: 'kpi',
        loadComponent: () => import('./pages/kpi-dashboard/kpi-dashboard.component').then(m => m.KpiDashboardComponent)
      },

      // ── ADMIN ONLY ──────────────────────────────────────────────────────
      {
        path: 'utilisateurs',
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN'] },
        loadComponent: () => import('./pages/utilisateurs/utilisateurs.component').then(m => m.UtilisateursComponent)
      },

      // ── ADMIN + RESPONSABLE_PRODUCTION ──────────────────────────────────
      {
        path: 'production-lines',
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'RESPONSABLE_PRODUCTION'] },
        loadComponent: () => import('./pages/production-lines/production-lines.component').then(m => m.ProductionLinesComponent)
      },
      {
        path: 'postes-travail',
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'RESPONSABLE_PRODUCTION'] },
        loadComponent: () => import('./pages/postes-travail/postes-travail.component').then(m => m.PostesTravailComponent)
      },
      {
        path: 'daily-production',
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'RESPONSABLE_PRODUCTION', 'INGENIEUR_QUALITE', 'PREPARATEUR'] },
        loadComponent: () => import('./pages/daily-production/daily-production.component').then(m => m.DailyProductionComponent)
      },
      {
        path: 'trg-monitoring',
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'RESPONSABLE_PRODUCTION'] },
        loadComponent: () => import('./pages/trg-monitoring/trg-monitoring.component').then(m => m.TrgMonitoringComponent)
      },
      {
        path: 'downtimes',
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'RESPONSABLE_PRODUCTION'] },
        loadComponent: () => import('./pages/downtimes/downtimes.component').then(m => m.DowntimesComponent)
      },

      // ── ADMIN + INGENIEUR_QUALITE ────────────────────────────────────────
      {
        path: 'instructions',
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'INGENIEUR_QUALITE', 'PREPARATEUR', 'RESPONSABLE_PRODUCTION'] },
        loadComponent: () => import('./pages/instruction-sheets/instruction-sheets.component').then(m => m.InstructionSheetsComponent)
      },
      {
        path: 'fi-guide',
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'INGENIEUR_QUALITE', 'PREPARATEUR', 'RESPONSABLE_PRODUCTION'] },
        loadComponent: () => import('./pages/fi-guide/fi-guide.component').then(m => m.FiGuideComponent)
      },
      {
        path: 'non-conformites',
        canActivate: [RoleGuard],
        data: { roles: ['ADMIN', 'INGENIEUR_QUALITE', 'RESPONSABLE_PRODUCTION', 'PREPARATEUR'] },
        loadComponent: () => import('./pages/non-conformities/non-conformities.component').then(m => m.NonConformitiesComponent)
      },

      // ── ALL roles (settings) ─────────────────────────────────────────────
      {
        path: 'settings',
        loadComponent: () => import('./pages/settings/settings.component').then(m => m.SettingsComponent)
      },
    ]
  },

  { path: '**', redirectTo: 'login' }
];
