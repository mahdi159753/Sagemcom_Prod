// src/app/components/layout/layout.component.ts
import { Component, OnInit, OnDestroy, inject } from '@angular/core';
import { RouterOutlet, RouterLink, RouterLinkActive, Router } from '@angular/router';
import { CommonModule } from '@angular/common';
import { AuthService } from '../../services/auth.service';
import { Role } from '../../models/models';
import { NotificationService, AppNotification } from '../../services/notification.service';
import { Subscription } from 'rxjs';
import { HostListener } from '@angular/core';

interface NavItem {
  label: string;
  path:  string;
  icon:  string;
  roles?: Role[];   // undefined = visible to ALL authenticated users
}

// ── Full nav definition with role restrictions ────────────────────────────────
const ALL_NAV_ITEMS: NavItem[] = [
  { label: 'Overview',           path: '/dashboard',        icon: 'ph-squares-four' },
  { label: 'KPI Dashboard',      path: '/kpi',              icon: 'ph-chart-bar' },
  { label: 'Production Lines',   path: '/production-lines', icon: 'ph-factory',       roles: ['ADMIN', 'RESPONSABLE_PRODUCTION'] },
  { label: 'Postes de Travail',  path: '/postes-travail',   icon: 'ph-wrench',        roles: ['ADMIN', 'RESPONSABLE_PRODUCTION'] },
  { label: 'Daily Production',   path: '/daily-production', icon: 'ph-clipboard-text',roles: ['ADMIN', 'RESPONSABLE_PRODUCTION', 'INGENIEUR_QUALITE', 'PREPARATEUR'] },
  { label: 'TRG/TRS Monitoring', path: '/trg-monitoring',   icon: 'ph-trend-up',      roles: ['ADMIN', 'RESPONSABLE_PRODUCTION'] },
  { label: 'Downtimes',          path: '/downtimes',        icon: 'ph-warning-octagon',roles: ['ADMIN', 'RESPONSABLE_PRODUCTION'] },
  { label: 'Non-Conformités',    path: '/non-conformites',  icon: 'ph-shield-warning', roles: ['ADMIN', 'INGENIEUR_QUALITE', 'RESPONSABLE_PRODUCTION', 'PREPARATEUR'] },
  { label: 'Instructions',       path: '/instructions',     icon: 'ph-file-text',     roles: ['ADMIN', 'INGENIEUR_QUALITE', 'PREPARATEUR', 'RESPONSABLE_PRODUCTION'] },
  { label: 'FI-Guide',           path: '/fi-guide',         icon: 'ph-list-checks',   roles: ['ADMIN', 'INGENIEUR_QUALITE', 'PREPARATEUR', 'RESPONSABLE_PRODUCTION'] },
  { label: 'Chat',               path: '/chat',             icon: 'ph-chat-circle-dots' },
  { label: 'Settings',           path: '/settings',         icon: 'ph-gear' },
  { label: 'Users',              path: '/utilisateurs',     icon: 'ph-users',         roles: ['ADMIN'] },
];

import { ChatWidgetComponent } from '../chat-widget/chat-widget.component';
import { CopilotWidgetComponent } from '../copilot-widget/copilot-widget.component';

@Component({
  selector: 'app-layout',
  standalone: true,
  imports: [RouterOutlet, RouterLink, RouterLinkActive, CommonModule, ChatWidgetComponent, CopilotWidgetComponent],
  templateUrl: './layout.component.html',
  styleUrls: ['./layout.component.scss']
})
export class LayoutComponent implements OnInit, OnDestroy {
  public authService = inject(AuthService);
  private router      = inject(Router);
  public notifService = inject(NotificationService);

  navItems: NavItem[] = [];
  currentRole: Role | null = null;
  userInitials = '';
  currentTime = '';
  currentDate = '';
  
  notifications: AppNotification[] = [];
  unreadCount = 0;
  showNotifDropdown = false;
  activeToasts: any[] = [];
  private subs: Subscription = new Subscription();
  private timer: any;

  ngOnInit() {
    this.currentRole  = this.authService.getRole();
    this.userInitials = this.buildInitials();
    this.navItems     = this.buildNav();
    this.updateDateTime();
    this.timer = setInterval(() => this.updateDateTime(), 1000);

    this.subs.add(this.notifService.unread$.subscribe(notifs => {
       this.notifications = notifs;
       this.unreadCount = notifs.length;
    }));
    
    this.subs.add(this.notifService.toast$.subscribe(toast => {
       if (toast) {
          const t = { ...toast, id: Date.now() };
          this.activeToasts.push(t);
          setTimeout(() => {
             this.activeToasts = this.activeToasts.filter(x => x.id !== t.id);
          }, 5000);
       }
    }));
  }

  ngOnDestroy() { 
    clearInterval(this.timer); 
    this.subs.unsubscribe();
  }

  toggleNotifDropdown(event: Event) {
    event.stopPropagation();
    this.showNotifDropdown = !this.showNotifDropdown;
  }

  @HostListener('document:click', ['$event'])
  onDocumentClick() {
    this.showNotifDropdown = false;
  }

  markAsRead(id: number, event: Event) {
    event.stopPropagation();
    this.notifService.markAsRead(id);
  }

  markAllAsRead(event: Event) {
    event.stopPropagation();
    this.notifService.markAllAsRead();
  }

  formatTimeAgo(dateString: string) {
    const d = new Date(dateString);
    const diff = Math.floor((new Date().getTime() - d.getTime()) / 1000);
    if (diff < 60) return "Just now";
    const mins = Math.floor(diff / 60);
    if (mins < 60) return `${mins}m ago`;
    const hrs = Math.floor(mins / 60);
    if (hrs < 24) return `${hrs}h ago`;
    return `${Math.floor(hrs / 24)}d ago`;
  }


  // ── Helpers ────────────────────────────────────────────────────────────────
  private buildNav(): NavItem[] {
    return ALL_NAV_ITEMS.filter(item =>
      !item.roles || this.authService.hasRole(...item.roles)
    );
  }

  private buildInitials(): string {
    const f = this.authService.getFirstname() ?? '';
    const l = this.authService.getLastname()  ?? '';
    return (f[0] ?? '') + (l[0] ?? '');
  }

  // ── Role display label ─────────────────────────────────────────────────────
  get roleLabel(): string {
    const map: Record<string, string> = {
      ADMIN:                   'Administrateur',
      RESPONSABLE_PRODUCTION:  'Resp. Production',
      INGENIEUR_QUALITE:       'Ingénieur Qualité',
      PREPARATEUR:             'Préparateur',
    };
    return map[this.currentRole ?? ''] ?? this.currentRole ?? '';
  }
  
  get roleBadgeColor(): string {
    const map: Record<string, string> = {
      ADMIN:                   'badge-red',
      RESPONSABLE_PRODUCTION:  'badge-blue',
      INGENIEUR_QUALITE:       'badge-green',
      PREPARATEUR:             'badge-orange',
    };
    return map[this.currentRole ?? ''] ?? 'badge-gray';
  }

  get username(): string {
    const f = this.authService.getFirstname() ?? '';
    const l = this.authService.getLastname()  ?? '';
    return `${f} ${l}`.trim() || this.authService.getUserEmail() || 'User';
  }

  get greeting(): string {
    const hour = new Date().getHours();
    if (hour < 12) return 'Good Morning';
    if (hour < 18) return 'Good Afternoon';
    return 'Good Evening';
  }

  private updateDateTime() {
    const now = new Date();
    this.currentTime = now.toLocaleTimeString('en-US', { hour: '2-digit', minute: '2-digit' });
    this.currentDate = now.toLocaleDateString('en-US', { weekday: 'long', year: 'numeric', month: 'long', day: 'numeric' });
  }

  logout() {
    this.authService.logout();
    this.router.navigate(['/login']);
  }
}
