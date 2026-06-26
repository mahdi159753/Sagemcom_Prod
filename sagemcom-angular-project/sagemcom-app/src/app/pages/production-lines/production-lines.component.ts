import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProductionLineService } from '../../services/production-line.service';
import { ProduitService } from '../../services/produit.service';
import { AuthService } from '../../services/auth.service';
import { ProductionLine, LigneStatut, Produit } from '../../models/models';
import { Router } from '@angular/router';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-production-lines',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './production-lines.component.html',
  styleUrls: ['./production-lines.component.scss']
})
export class ProductionLinesComponent implements OnInit {
  lines: ProductionLine[] = [];
  filtered: ProductionLine[] = [];
  products: Produit[] = [];
  
  search = '';
  statusFilter = 'all';
  productFilter = 'all';

  // Stats
  totalLines = 0;
  activeLines = 0;
  stoppedLines = 0;
  avgEfficiency = 0;

  // Modals
  showModal = false;
  showStatusModal = false;
  isEditing = false;
  
  currentLine: any = {
    code: '', nom: '', chantier: 'INTEG', produitId: null, statut: 'EN_PRODUCTION',
    shiftActuel: '', responsable: '', cadenceObjectif: 200, cadenceReelle: 0
  };

  statusUpdateData = { id: 0, statut: 'EN_PRODUCTION', cause: '' };

  private lineService = inject(ProductionLineService);
  private produitService = inject(ProduitService);
  public authService = inject(AuthService); // for HTML
  private router = inject(Router);
  private notificationService = inject(NotificationService);

  ngOnInit() {
    this.loadData();
    this.loadProducts();
  }

  viewPostes(line: ProductionLine) {
    // Navigate to the postes-travail page and pass the line name as a query parameter
    this.router.navigate(['/postes-travail'], { queryParams: { ligne: line.nom } });
  }

  loadData() {
    this.lineService.getAll().subscribe({
      next: (data) => {
        this.lines = data;
        this.applyFilter();
        this.calculateStats();
      },
      error: (err) => console.error('Failed to load lines', err)
    });
  }

  loadProducts() {
    this.produitService.getAll().subscribe({
      next: (data) => this.products = data,
      error: (err) => console.error('Failed to load products', err)
    });
  }

  applyFilter() {
    this.filtered = this.lines.filter(line => {
      const matchSearch = line.nom.toLowerCase().includes(this.search.toLowerCase()) || 
                          line.code.toLowerCase().includes(this.search.toLowerCase());
      const matchStatus = this.statusFilter === 'all' || line.statut === this.statusFilter;
      const matchProduct = this.productFilter === 'all' || 
                          (line.produitId && line.produitId.toString() === this.productFilter);
      return matchSearch && matchStatus && matchProduct;
    });
  }

  calculateStats() {
    this.totalLines = this.lines.length;
    this.activeLines = this.lines.filter(l => l.statut === 'EN_PRODUCTION').length;
    this.stoppedLines = this.lines.filter(l => l.statut === 'ARRETEE').length;
    
    const linesWithTargets = this.lines.filter(l => l.cadenceObjectif > 0);
    if (linesWithTargets.length > 0) {
      const totalEff = linesWithTargets.reduce((sum, l) => sum + (l.efficiency || 0), 0);
      this.avgEfficiency = Math.round(totalEff / linesWithTargets.length);
    } else {
      this.avgEfficiency = 0;
    }
  }

  // --- UI Helpers ---
  getStatutColor(statut: string): string {
    return { 'EN_PRODUCTION': '#28A745', 'ARRETEE': '#DC3545', 'CHANGEMENT_SERIE': '#FFC107' }[statut] ?? '#94A3B8';
  }

  getStatutLabel(statut: string): string {
    return { 'EN_PRODUCTION': 'En Production', 'ARRETEE': 'Arrêtée', 'CHANGEMENT_SERIE': 'Changement Série' }[statut] ?? statut;
  }

  getTrgColor(trg: number): string {
    if (trg >= 80) return '#28A745';
    if (trg >= 60) return '#FFC107';
    return '#DC3545';
  }

  // --- Modal Logic ---
  openEditModal(line: ProductionLine) {
    this.isEditing = true;
    this.currentLine = { ...line };
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
  }

  saveLine() {
    if (!this.currentLine.code || !this.currentLine.nom) {
      this.notificationService.showError("Erreur", "Code and Name are required!");
      return;
    }

    if (this.currentLine.produitId) {
      this.currentLine.produitId = Number(this.currentLine.produitId);
    } else {
      this.currentLine.produitId = null;
    }

    if (this.currentLine.id) {
      this.lineService.update(this.currentLine.id, this.currentLine).subscribe({
        next: () => {
          this.loadData();
          this.closeModal();
          this.notificationService.showSuccess("Succès", "Ligne mise à jour.");
        },
        error: err => this.notificationService.showError("Erreur", "Update failed: " + err.message)
      });
    }
  }

  // --- Status Change Logic ---
  openStatusModal(line: ProductionLine) {
    this.statusUpdateData = { id: line.id!, statut: line.statut, cause: '' };
    this.showStatusModal = true;
  }

  closeStatusModal() {
    this.showStatusModal = false;
  }

  updateStatus() {
    this.lineService.changeStatut(this.statusUpdateData.id, this.statusUpdateData.statut, this.statusUpdateData.cause)
      .subscribe({
        next: () => {
          this.loadData();
          this.closeStatusModal();
          this.notificationService.showSuccess("Succès", "Statut mis à jour.");
        }, 
        error: err => this.notificationService.showError("Erreur", "Status change failed")
      });
  }
}
