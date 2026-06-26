import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { FiGuideService } from '../../services/fi-guide.service';
import { ProduitService } from '../../services/produit.service';
import { AuthService } from '../../services/auth.service';
import { Produit, FiDocument, InstructionStep, ControlSession, SessionReport, SessionReportStep } from '../../models/models';
import { NotificationService } from '../../services/notification.service';

@Component({
  selector: 'app-fi-guide',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './fi-guide.component.html',
  styleUrls: ['./fi-guide.component.scss']
})
export class FiGuideComponent implements OnInit {
  // Services
  private fiGuideService = inject(FiGuideService);
  private produitService = inject(ProduitService);
  public authService = inject(AuthService);
  private notificationService = inject(NotificationService);

  // Layout Tab
  activeTab: 'operator' | 'manager' | 'reports' = 'operator';

  // Products
  produits: Produit[] = [];
  filteredProduits: Produit[] = [];
  searchProductQuery = '';
  loadingProducts = false;

  // Active Instructions Cache
  // Maps product.id to { document: FiDocument | null, steps: InstructionStep[] }
  activeFiCache: Record<number, { document: FiDocument | null; steps: InstructionStep[] }> = {};

  // Operator Checklist State
  activeSession: ControlSession | null = null;
  activeSteps: InstructionStep[] = [];
  currentStepIndex = 0;
  
  // Validation Form for Current Step
  stepValidationStatus: 'CONFORME' | 'NON_CONFORME' | null = null;
  stepValidationComment = '';
  validationError = '';
  submittingStep = false;

  // Manager Upload State
  selectedUploadProduct: Produit | null = null;
  uploadVersion = '';
  selectedFile: File | null = null;
  selectedFileName = '';
  uploadError = '';
  uploadSuccess = '';
  submittingUpload = false;

  // History & Reports State
  sessionsHistory: any[] = [];
  filteredHistory: any[] = [];
  searchHistoryQuery = '';
  loadingHistory = false;
  
  // Report Detail Modal
  selectedReport: SessionReport | null = null;
  showReportModal = false;
  loadingReportDetails = false;

  // Lightbox Zoom State
  showLightbox = false;
  lightboxImageUrl = '';

  ngOnInit() {
    this.loadProduits();
    this.checkSavedSession();
    this.loadHistory();
  }

  // --- ACCESS CHECKS ---
  canManage(): boolean {
    return this.authService.isAdmin() || this.authService.isIngenieur();
  }

  canViewReports(): boolean {
    return this.authService.isAdmin() || this.authService.isIngenieur() || this.authService.isResponsable();
  }

  setTab(tab: 'operator' | 'manager' | 'reports') {
    if (tab === 'manager' && !this.canManage()) return;
    if (tab === 'reports' && !this.canViewReports()) return;
    this.activeTab = tab;
  }

  // --- PRODUCTS DATA ---
  loadProduits() {
    this.loadingProducts = true;
    this.produitService.getAll().subscribe({
      next: (data) => {
        this.produits = data;
        this.applyProductFilter();
        this.loadActiveFiInfoForProducts();
      },
      error: (err) => {
        console.error('Error loading products', err);
        this.loadingProducts = false;
      }
    });
  }

  applyProductFilter() {
    if (!this.searchProductQuery.trim()) {
      this.filteredProduits = [...this.produits];
    } else {
      const q = this.searchProductQuery.toLowerCase();
      this.filteredProduits = this.produits.filter(
        (p) => p.name.toLowerCase().includes(q) || p.reference.toLowerCase().includes(q)
      );
    }
  }

  loadActiveFiInfoForProducts() {
    let completedCount = 0;
    if (this.produits.length === 0) {
      this.loadingProducts = false;
      return;
    }

    this.produits.forEach((p) => {
      if (p.id) {
        this.fiGuideService.getActiveInstruction(p.id).subscribe({
          next: (res) => {
            this.activeFiCache[p.id!] = {
              document: res.document,
              steps: res.steps
            };
            completedCount++;
            if (completedCount === this.produits.length) {
              this.loadingProducts = false;
            }
          },
          error: (err) => {
            console.error(`Error loading FI for product ${p.id}`, err);
            this.activeFiCache[p.id!] = { document: null, steps: [] };
            completedCount++;
            if (completedCount === this.produits.length) {
              this.loadingProducts = false;
            }
          }
        });
      }
    });
  }

  hasActiveFi(produitId?: number): boolean {
    if (!produitId) return false;
    return !!this.activeFiCache[produitId]?.document;
  }

  getFiVersion(produitId?: number): string {
    if (!produitId) return '';
    return this.activeFiCache[produitId]?.document?.version || '';
  }

  getFiStepsCount(produitId?: number): number {
    if (!produitId) return 0;
    return this.activeFiCache[produitId]?.steps?.length || 0;
  }

  // --- OPERATOR WIZARD SESSIONS ---
  checkSavedSession() {
    const saved = localStorage.getItem('fi_guide_active_session');
    const savedSteps = localStorage.getItem('fi_guide_active_steps');
    const savedIdx = localStorage.getItem('fi_guide_active_idx');

    if (saved && savedSteps && savedIdx) {
      try {
        this.activeSession = JSON.parse(saved);
        this.activeSteps = JSON.parse(savedSteps);
        this.currentStepIndex = parseInt(savedIdx, 10);
        this.resetValidationForm();
      } catch (e) {
        this.clearSessionLocal();
      }
    }
  }

  saveSessionLocal() {
    if (this.activeSession) {
      localStorage.setItem('fi_guide_active_session', JSON.stringify(this.activeSession));
      localStorage.setItem('fi_guide_active_steps', JSON.stringify(this.activeSteps));
      localStorage.setItem('fi_guide_active_idx', this.currentStepIndex.toString());
    }
  }

  clearSessionLocal() {
    localStorage.removeItem('fi_guide_active_session');
    localStorage.removeItem('fi_guide_active_steps');
    localStorage.removeItem('fi_guide_active_idx');
    this.activeSession = null;
    this.activeSteps = [];
    this.currentStepIndex = 0;
  }

  startSession(produit: Produit) {
    if (!produit.id) return;
    this.validationError = '';
    
    this.fiGuideService.startSession(produit.id).subscribe({
      next: (session) => {
        this.activeSession = session;
        this.activeSteps = this.activeFiCache[produit.id!]?.steps || [];
        this.currentStepIndex = 0;
        this.resetValidationForm();
        this.saveSessionLocal();
      },
      error: (err) => {
        console.error('Error starting session', err);
        this.notificationService.showError('Erreur', err.error?.message || 'Impossible de démarrer la session de contrôle.');
      }
    });
  }

  resetValidationForm() {
    this.stepValidationStatus = null;
    this.stepValidationComment = '';
    this.validationError = '';
  }

  selectConforme() {
    this.stepValidationStatus = 'CONFORME';
    this.validationError = '';
  }

  selectNonConforme() {
    this.stepValidationStatus = 'NON_CONFORME';
    this.validationError = '';
  }

  get currentStep(): InstructionStep | null {
    if (this.activeSteps.length > 0 && this.currentStepIndex < this.activeSteps.length) {
      return this.activeSteps[this.currentStepIndex];
    }
    return null;
  }

  get criteriaLines(): string[] {
    const step = this.currentStep;
    if (!step || !step.description) return [];
    return step.description.split('\n').map((line) => line.trim()).filter((line) => line.length > 0);
  }

  get progressPercentage(): number {
    if (this.activeSteps.length === 0) return 0;
    return Math.round((this.currentStepIndex / this.activeSteps.length) * 100);
  }

  validateStep() {
    if (!this.activeSession?.id || !this.currentStep?.id) return;
    if (!this.stepValidationStatus) {
      this.validationError = 'Veuillez sélectionner Conforme ou Non-Conforme.';
      return;
    }
    if (this.stepValidationStatus === 'NON_CONFORME' && !this.stepValidationComment.trim()) {
      this.validationError = 'Un commentaire est obligatoire pour un statut Non-Conforme.';
      return;
    }

    this.submittingStep = true;
    this.validationError = '';

    this.fiGuideService.validateStep(
      this.activeSession.id,
      this.currentStep.id,
      this.stepValidationStatus,
      this.stepValidationComment
    ).subscribe({
      next: () => {
        this.submittingStep = false;
        
        // Go to next step or complete
        if (this.currentStepIndex < this.activeSteps.length - 1) {
          this.currentStepIndex++;
          this.resetValidationForm();
          this.saveSessionLocal();
        } else {
          // Last step completed! Automatically trigger completion
          this.completeSession();
        }
      },
      error: (err) => {
        console.error('Error validating step', err);
        this.validationError = err.error?.message || 'Erreur lors de la validation.';
        this.submittingStep = false;
      }
    });
  }

  completeSession() {
    if (!this.activeSession?.id) return;
    
    this.fiGuideService.completeSession(this.activeSession.id).subscribe({
      next: (completedSession) => {
        // Show report summary modal or success page
        this.clearSessionLocal();
        this.loadHistory();
        
        // Show the report that was just completed
        this.viewReport(completedSession.id!);
        this.notificationService.showSuccess(
          'Session terminée', 
          `Résultat : ${completedSession.conforme ? 'CONFORME' : 'NON-CONFORME'}`
        );
      },
      error: (err) => {
        console.error('Error completing session', err);
        this.notificationService.showError('Erreur', err.error?.message || 'Erreur lors de la clôture de la session.');
      }
    });
  }

  async cancelSession() {
    const confirmed = await this.notificationService.confirmAction(
      'Annuler la session ?',
      'Voulez-vous vraiment annuler cette session ? Vos validations actuelles seront perdues.'
    );
    if (confirmed) {
      this.clearSessionLocal();
    }
  }

  // --- MANAGER UPLOAD ---
  openUploadModal(produit: Produit) {
    this.selectedUploadProduct = produit;
    this.uploadVersion = '';
    this.selectedFile = null;
    this.selectedFileName = '';
    this.uploadError = '';
    this.uploadSuccess = '';
  }

  closeUploadModal() {
    this.selectedUploadProduct = null;
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.selectedFileName = file.name;
      this.uploadError = '';
    }
  }

  uploadFiche() {
    if (!this.selectedUploadProduct?.id) return;
    if (!this.uploadVersion.trim()) {
      this.uploadError = 'Veuillez saisir la version (ex: Indice D).';
      return;
    }
    if (!this.selectedFile) {
      this.uploadError = 'Veuillez sélectionner un fichier Excel (.xlsx).';
      return;
    }

    this.submittingUpload = true;
    this.uploadError = '';
    this.uploadSuccess = '';

    this.fiGuideService.uploadFiche(
      this.selectedUploadProduct.id,
      this.uploadVersion,
      this.selectedFile
    ).subscribe({
      next: (doc) => {
        this.submittingUpload = false;
        this.uploadSuccess = `Fichier "${doc.fileName}" importé et rendu avec succès !`;
        this.loadProduits();
        setTimeout(() => {
          this.closeUploadModal();
        }, 1500);
      },
      error: (err) => {
        console.error('Error uploading fiche', err);
        this.uploadError = err.error?.message || 'Erreur de lecture ou de traitement du fichier Excel.';
        this.submittingUpload = false;
      }
    });
  }

  // --- HISTORY & REPORTS ---
  loadHistory() {
    this.loadingHistory = true;
    this.fiGuideService.getAllSessions().subscribe({
      next: (data) => {
        this.sessionsHistory = data;
        this.applyHistoryFilter();
        this.loadingHistory = false;
      },
      error: (err) => {
        console.error('Error loading session history', err);
        this.loadingHistory = false;
      }
    });
  }

  applyHistoryFilter() {
    if (!this.searchHistoryQuery.trim()) {
      this.filteredHistory = [...this.sessionsHistory];
    } else {
      const q = this.searchHistoryQuery.toLowerCase();
      this.filteredHistory = this.sessionsHistory.filter(
        (s) =>
          s.produit?.name.toLowerCase().includes(q) ||
          s.produit?.reference.toLowerCase().includes(q) ||
          s.operator?.firstname.toLowerCase().includes(q) ||
          s.operator?.lastname.toLowerCase().includes(q) ||
          s.status.toLowerCase().includes(q)
      );
    }
  }

  viewReport(sessionId: number) {
    this.loadingReportDetails = true;
    this.selectedReport = null;
    this.showReportModal = true;

    this.fiGuideService.getSessionReport(sessionId).subscribe({
      next: (report) => {
        this.selectedReport = report;
        this.loadingReportDetails = false;
      },
      error: (err) => {
        console.error('Error loading session report', err);
        this.notificationService.showError('Erreur', 'Impossible de charger le rapport.');
        this.closeReportModal();
      }
    });
  }

  closeReportModal() {
    this.showReportModal = false;
    this.selectedReport = null;
  }

  printReport() {
    window.print();
  }

  getImageArray(urls?: string): string[] {
    if (!urls) return [];
    return urls.split(',').filter(u => u.trim() !== '');
  }

  // ── LIGHTBOX ZOOM MODAL ──────────────────────────────────────────

  openLightbox(url: string) {
    this.lightboxImageUrl = url;
    this.showLightbox = true;
  }

  closeLightbox() {
    this.showLightbox = false;
    this.lightboxImageUrl = '';
  }

  async deleteSession(sessionId: number) {
    const confirmed = await this.notificationService.confirmDelete(
      'Supprimer la session ?',
      `Êtes-vous sûr de vouloir supprimer la session #${sessionId} ainsi que toutes ses étapes de validation ?`
    );
    if (!confirmed) {
      return;
    }
    this.fiGuideService.deleteSession(sessionId).subscribe({
      next: () => {
        this.sessionsHistory = this.sessionsHistory.filter(s => s.id !== sessionId);
        this.applyHistoryFilter();
        this.notificationService.showSuccess('Succès', 'Session supprimée.');
      },
      error: (err) => {
        console.error('Error deleting session', err);
        this.notificationService.showError('Erreur', 'Erreur lors de la suppression de la session.');
      }
    });
  }
}
