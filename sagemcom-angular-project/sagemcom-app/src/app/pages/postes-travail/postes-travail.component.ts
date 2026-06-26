// src/app/pages/postes-travail/postes-travail.component.ts
import { Component, OnInit, inject } from '@angular/core';
import { CommonModule }      from '@angular/common';
import { FormsModule }       from '@angular/forms';
import { ActivatedRoute, Router } from '@angular/router';
import {
  PosteTravailService,
  PosteTravailResponse,
  PosteTravailRequest
} from '../../services/poste-travail.service';
import { ProductionLineService } from '../../services/production-line.service';
import { ProductionLine } from '../../models/models';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-postes-travail',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './postes-travail.component.html',
  styleUrls: ['./postes-travail.component.scss']
})
export class PostesTravailComponent implements OnInit {

  // ── STATE ──────────────────────────────────────────────────────────────────
  postes:        PosteTravailResponse[] = [];
  filtered:      PosteTravailResponse[] = [];
  selectedPoste: PosteTravailResponse | null = null;
  productionLines: ProductionLine[] = [];
  groupedPostes: { ligne: string, postes: (PosteTravailResponse | null)[] }[] = [];

  // Filter values
  search       = '';
  ligneFilter  = 'all';
  statutFilter = 'all';

  // Stats
  totalPostes    = 0;
  activePostes   = 0;
  inactivePostes = 0;
  avgTrg         = 0;
  avgFpy         = 0;

  // UI state
  loading      = false;
  errorMsg     = '';
  successMsg   = '';
  showModal    = false;
  isEditMode   = false;
  editingId:   number | null = null;
  showDeleteConfirm = false;
  deletingId:  number | null = null;

  // Form model
  form: PosteTravailRequest = this.emptyForm();

  // Dropdown options
  lignes: string[] = []; // dynamically populated
  types  = [
    { value: 'ASSEMBLAGE',      label: 'Assemblage'        },
    { value: 'TEST',            label: 'Test'              },
    { value: 'CONTROLE_QUALITE',label: 'Controle Qualite'  },
    { value: 'EMBALLAGE',       label: 'Emballage'         },
  ];

  private posteService = inject(PosteTravailService);
  private lineService = inject(ProductionLineService);
  public authService = inject(AuthService);
  private route = inject(ActivatedRoute);
  private notificationService = inject(NotificationService);

  // ── LIFECYCLE ──────────────────────────────────────────────────────────────
  ngOnInit(): void {
    this.loadProductionLines();
    this.route.queryParams.subscribe(params => {
      if (params['ligne']) {
        this.ligneFilter = params['ligne'];
      }
      this.loadPostes();
    });
  }

  // ── PERMISSIONS ───────────────────────────────────────────────────────────
  canWrite(): boolean {
    return this.authService.hasRole('ADMIN', 'RESPONSABLE_PRODUCTION');
  }
  canDelete(): boolean {
    return this.authService.hasRole('ADMIN');
  }

  // ── LOAD ───────────────────────────────────────────────────────────────────
  loadProductionLines(): void {
    this.lineService.getAll().subscribe({
      next: (data) => {
        this.productionLines = data;
        this.lignes = data.map(l => l.nom);
      },
      error: (err) => console.error("Failed to load lines", err)
    });
  }

  loadPostes(): void {
    this.loading  = true;
    this.errorMsg = '';
    this.posteService.getAll().subscribe({
      next: data => {
        this.postes   = data;
        this.applyFilter();
        this.loading  = false;
      },
      error: err => {
        this.errorMsg = 'Erreur lors du chargement des postes.';
        this.loading  = false;
        console.error(err);
      }
    });
  }

  // ── FILTER ─────────────────────────────────────────────────────────────────
  applyFilter(): void {
    this.filtered = this.postes.filter(p => {
      const matchSearch  = p.code.toLowerCase().includes(this.search.toLowerCase()) ||
                           p.libelle.toLowerCase().includes(this.search.toLowerCase());
      const matchLigne   = this.ligneFilter  === 'all' || p.ligne  === this.ligneFilter;
      const matchStatut  = this.statutFilter === 'all' ||
                           p.statut === this.statutFilter.toUpperCase();
      return matchSearch && matchLigne && matchStatut;
    });

    this.calculateStats();
    this.groupPostesByLine();
  }

  groupPostesByLine(): void {
    const order = ['ASSEMBLAGE', 'TEST', 'CONTROLE_QUALITE', 'EMBALLAGE'];
    const groups = new Map<string, (PosteTravailResponse | null)[]>();
    
    const visibleLignes = this.ligneFilter === 'all' ? this.lignes : [this.ligneFilter];

    visibleLignes.forEach(l => {
        groups.set(l, [null, null, null, null]);
    });

    this.filtered.forEach(p => {
        if (!groups.has(p.ligne)) {
            groups.set(p.ligne, [null, null, null, null]);
        }
        
        const idx = order.indexOf(p.type);
        if (idx !== -1) {
            const arr = groups.get(p.ligne)!;
            arr[idx] = p;
        }
    });

    this.groupedPostes = Array.from(groups.entries()).map(([ligne, postes]) => ({ ligne, postes }));
    
    // Sort so lines appear in a nice order (e.g., Ligne 1, Ligne 2)
    this.groupedPostes.sort((a, b) => a.ligne.localeCompare(b.ligne));
  }

  calculateStats() {
    this.totalPostes = this.filtered.length;
    this.activePostes = this.filtered.filter(p => p.statut === 'ACTIF').length;
    this.inactivePostes = this.filtered.filter(p => p.statut === 'INACTIF').length;

    const actives = this.filtered.filter(p => p.statut === 'ACTIF');
    if (actives.length > 0) {
      const totalTrg = actives.reduce((sum, p) => sum + (p.trg || 0), 0);
      const totalFpy = actives.reduce((sum, p) => sum + (p.fpy || 0), 0);
      this.avgTrg = Math.round(totalTrg / actives.length);
      this.avgFpy = Math.round(totalFpy / actives.length);
    } else {
      this.avgTrg = 0;
      this.avgFpy = 0;
    }
  }

  // ── OPEN MODAL ─────────────────────────────────────────────────────────────
  openCreate(): void {
    this.isEditMode  = false;
    this.editingId   = null;
    this.form        = this.emptyForm();
    this.form.ligne  = this.ligneFilter !== 'all' ? this.ligneFilter : '';
    this.showModal   = true;
    this.errorMsg    = '';
  }

  openEdit(p: PosteTravailResponse, event: Event): void {
    event.stopPropagation();
    this.isEditMode  = true;
    this.editingId   = p.id;
    this.form = {
      code:             p.code,
      libelle:          p.libelle,
      ligne:            p.ligne,
      type:             p.type,
      ficheInstruction: p.ficheInstruction ?? '',
      ficheVersion:     p.ficheVersion ?? '',
      operateur:        p.operateur ?? '',
      statut:           p.statut,
      trg:              p.trg,
      trs:              p.trs,
      fpy:              p.fpy,
    };
    this.showModal   = true;
    this.errorMsg    = '';
  }

  closeModal(): void { this.showModal = false; }

  // ── SAVE (create or update) ────────────────────────────────────────────────
  savePoste(): void {
    this.errorMsg = '';
    const obs = this.isEditMode && this.editingId != null
      ? this.posteService.update(this.editingId, this.form)
      : this.posteService.create(this.form);

    obs.subscribe({
      next: saved => {
        if (this.isEditMode) {
          const idx = this.postes.findIndex(p => p.id === saved.id);
          if (idx > -1) this.postes[idx] = saved;
        } else {
          this.postes.push(saved);
        }
        this.applyFilter();
        this.closeModal();
      },
      error: err => {
        this.errorMsg = err.error?.message ?? 'Une erreur est survenue.';
      }
    });
  }

  // ── TOGGLE STATUT ──────────────────────────────────────────────────────────
  toggleStatut(p: PosteTravailResponse, event: Event): void {
    event.stopPropagation();
    this.posteService.toggleStatut(p.id).subscribe({
      next: updated => {
        const idx = this.postes.findIndex(x => x.id === updated.id);
        if (idx > -1) this.postes[idx] = updated;
        this.applyFilter();
      },
      error: () => this.notificationService.showError('Erreur', 'Impossible de changer le statut.')
    });
  }

  // ── DELETE ─────────────────────────────────────────────────────────────────
  async confirmDelete(id: number, event: Event) {
    event.stopPropagation();
    const confirmed = await this.notificationService.confirmDelete(
      'Supprimer le poste ?',
      'Êtes-vous sûr de vouloir supprimer ce poste ?'
    );
    if (confirmed) {
      this.deletingId = id;
      this.doDelete();
    }
  }

  doDelete(): void {
    if (this.deletingId == null) return;
    this.posteService.delete(this.deletingId).subscribe({
      next: () => {
        this.postes = this.postes.filter(p => p.id !== this.deletingId);
        this.applyFilter();
        this.showDeleteConfirm = false;
        this.deletingId        = null;
        this.notificationService.showSuccess('Succès', 'Poste supprimé avec succès.');
      },
      error: () => this.notificationService.showError('Erreur', 'Impossible de supprimer ce poste.')
    });
  }

  cancelDelete(): void {
    this.showDeleteConfirm = false;
    this.deletingId        = null;
  }

  // ── HELPERS ────────────────────────────────────────────────────────────────
  getTypeLabel(type: string): string {
    const map: Record<string, string> = {
      ASSEMBLAGE:       'Assemblage',
      TEST:             'Test',
      CONTROLE_QUALITE: 'Contrôle Qualité',
      EMBALLAGE:        'Emballage',
    };
    return map[type] ?? type;
  }

  isVersionProd(version: string): boolean {
    return version?.includes('PROD') ?? false;
  }

  getTrgColor(val: number): string {
    if (val >= 85) return '#28A745';
    if (val >= 70) return '#FFC107';
    return '#DC3545';
  }

  private emptyForm(): PosteTravailRequest {
    return {
      code: '', libelle: '', ligne: '',
      type: 'ASSEMBLAGE', ficheInstruction: '',
      ficheVersion: '', operateur: '',
      statut: 'ACTIF', trg: 0, trs: 0, fpy: 0
    };
  }
}
