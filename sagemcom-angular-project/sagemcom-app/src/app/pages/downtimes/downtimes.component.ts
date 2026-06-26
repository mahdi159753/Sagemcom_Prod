import { Component, OnInit, OnDestroy } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { DowntimeService } from '../../services/downtime.service';
import { ProduitService } from '../../services/produit.service';
import { NotificationService } from '../../services/notification.service';
import { Downtime, DowntimeStatsResponse, Produit } from '../../models/models';

export interface GroupedDowntime {
  ligne: string;
  latestEvent: Downtime;
  count: number;
  events: Downtime[];
}

@Component({
  selector: 'app-downtimes',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './downtimes.component.html',
  styleUrls: ['./downtimes.component.scss']
})
export class DowntimesComponent implements OnInit, OnDestroy {
  events: Downtime[] = [];
  filtered: Downtime[] = [];
  groupedDowntimes: GroupedDowntime[] = [];
  statusFilter = 'all';

  stats: DowntimeStatsResponse | null = null;
  timerInterval: any;

  showModal = false;
  showDetailsModal = false;
  selectedGroup: GroupedDowntime | null = null;
  isEditing = false;
  currentDowntime: Partial<Downtime> = { severity: 'MEDIUM' };

  produits: Produit[] = [];

  constructor(
    private downtimeService: DowntimeService,
    private produitService: ProduitService,
    private notificationService: NotificationService
  ) {}

  ngOnInit() {
    this.produitService.getAll().subscribe({
      next: (res) => this.produits = res,
      error: (err) => console.error(err)
    });
    this.loadData();
    // Update ongoing durations every minute
    this.timerInterval = setInterval(() => {
      this.updateOngoingDurations();
    }, 60000); // 1 minute
  }

  ngOnDestroy() {
    if (this.timerInterval) {
      clearInterval(this.timerInterval);
    }
  }

  loadData() {
    this.downtimeService.getStats().subscribe({
      next: (stats) => this.stats = stats,
      error: (e) => console.error("Error loading stats", e)
    });

    this.downtimeService.getAll().subscribe({
      next: (res) => {
        this.events = res.map(e => this.enrichDowntime(e));
        this.events.sort((a,b) => new Date(b.startTime!).getTime() - new Date(a.startTime!).getTime());
        this.applyFilter();
      },
      error: (e) => console.error("Error loading downtimes", e)
    });
  }

  enrichDowntime(e: Downtime): Downtime {
    e.dateStr = this.formatDateStr(e.startTime);
    e.duration = this.calculateDuration(e.startTime, e.endTime);
    return e;
  }

  updateOngoingDurations() {
    let hasChanges = false;
    this.events.forEach(e => {
      if (e.statut === 'ONGOING') {
        e.duration = this.calculateDuration(e.startTime, e.endTime);
        hasChanges = true;
      }
    });
    if (hasChanges) {
      this.applyFilter();
    }
  }

  calculateDuration(start?: string | null, end?: string | null): string {
    if (!start) return '';
    const d1 = new Date(start);
    const d2 = end ? new Date(end) : new Date();
    
    // Total minutes
    const diffMs = d2.getTime() - d1.getTime();
    if (diffMs < 0) return '0m';
    
    const totalMins = Math.floor(diffMs / 60000);
    const hours = Math.floor(totalMins / 60);
    const mins = totalMins % 60;
    
    if (hours === 0) return `${mins}m`;
    return `${hours}h ${mins}m`;
  }

  formatDateStr(start?: string | null): string {
    if (!start) return '';
    const d = new Date(start);
    return d.toLocaleDateString() + ' ' + d.toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' });
  }

  applyFilter() {
    this.filtered = this.statusFilter === 'all'
      ? this.events
      : this.events.filter(e => e.statut?.toLowerCase() === this.statusFilter.toLowerCase());

    const groups = new Map<string, Downtime[]>();
    for (const e of this.filtered) {
      const ligne = e.ligne || 'Ligne Inconnue';
      if (!groups.has(ligne)) {
        groups.set(ligne, []);
      }
      groups.get(ligne)!.push(e);
    }

    this.groupedDowntimes = Array.from(groups.entries()).map(([ligne, events]) => {
      return {
        ligne,
        latestEvent: events[0],
        count: events.length,
        events: events
      };
    });
  }

  getSeverityClass(s: string): string {
    return { CRITICAL:'badge-red', HIGH:'badge-red', MEDIUM:'badge-yellow', LOW:'badge-green' }[s] ?? 'badge-gray';
  }
  
  getStatusClass(s: string | undefined): string {
    return s === 'ONGOING' ? 'badge-red animate-pulse' : 'badge-green';
  }
  
  getBorderColor(s: string | undefined): string {
    return s === 'ONGOING' ? '#DC3545' : '#28A745';
  }

  openAddModal() {
    this.isEditing = false;
    this.currentDowntime = { severity: 'MEDIUM', statut: 'ONGOING' };
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
  }

  openDetailsModal(group: GroupedDowntime) {
    this.selectedGroup = group;
    this.showDetailsModal = true;
  }

  closeDetailsModal() {
    this.showDetailsModal = false;
    this.selectedGroup = null;
  }

  saveDowntime() {
    if (!this.currentDowntime.ligne || !this.currentDowntime.type) return;

    this.downtimeService.declareDowntime(this.currentDowntime as Downtime).subscribe({
      next: () => {
        this.closeModal();
        this.loadData();
      },
      error: (e) => console.error("Error saving", e)
    });
  }

  resolveDowntime(id?: number) {
    if (!id) return;
    this.downtimeService.resolveDowntime(id).subscribe({
      next: () => {
        this.loadData();
      },
      error: (e) => console.error("Error resolving", e)
    });
  }

  async deleteDowntime(id?: number) {
    if (!id) return;
    const confirmed = await this.notificationService.confirmDelete(
      'Supprimer l\'arrêt ?',
      'Voulez-vous vraiment supprimer cet événement d\'arrêt ?'
    );
    if (confirmed) {
      this.downtimeService.deleteDowntime(id).subscribe({
        next: () => {
          this.loadData();
          this.notificationService.showSuccess('Succès', 'Arrêt supprimé avec succès.');
        },
        error: (e) => console.error("Error deleting", e)
      });
    }
  }
}
