import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { NonConformityService } from '../../services/non-conformity.service';
import { NonConformity } from '../../models/models';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-non-conformities',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './non-conformities.component.html',
  styleUrls: ['./non-conformities.component.scss']
})
export class NonConformitiesComponent implements OnInit {
  ncs: NonConformity[] = [];
  filtered: NonConformity[] = [];
  search = '';
  statusFilter = 'all';
  origineFilter = 'all';

  // Modal states
  showModal = false;
  isEditing = false;
  selectedFile: File | null = null;
  selectedFileName = '';
  
  // PDF Preview State
  showPdfModal = false;
  pdfUrlToView: SafeResourceUrl | null = null;
  currentPreviewNc: NonConformity | null = null;

  // Form Model
  currentNc: NonConformity = {
    reference: '',
    origine: 'USINE',
    localisation: '',
    gravite: 'MINEUR',
    statut: 'OUVERTE',
    description: '',
    actionCorrective: '',
    procedureType: '',
    lotConcerne: '',
    valideeParClient: false
  };

  // AI Suggestions State
  aiSuggestion: any = null;
  isAnalyzing = false;
  private analyzeTimeout: any;

  private ncService = inject(NonConformityService);
  public authService = inject(AuthService); // For template access
  private sanitizer = inject(DomSanitizer);
  private notificationService = inject(NotificationService);

  ngOnInit() {
    this.loadNcs();
  }

  loadNcs() {
    this.ncService.getAll().subscribe({
      next: (data) => {
        this.ncs = data;
        this.applyFilter();
      },
      error: (err) => {
        console.error('Erreur lors du chargement des NC', err);
      }
    });
  }

  applyFilter() {
    this.filtered = this.ncs.filter(nc => {
      const matchSearch = nc.reference.toLowerCase().includes(this.search.toLowerCase()) || 
                          (nc.description && nc.description.toLowerCase().includes(this.search.toLowerCase()));
      const matchStatus = this.statusFilter === 'all' || nc.statut === this.statusFilter;
      const matchOrigine = this.origineFilter === 'all' || nc.origine === this.origineFilter;
      return matchSearch && matchStatus && matchOrigine;
    });
  }

  // --- STATS LOGIC ---
  get totalNcs(): number { return this.ncs.length; }
  get openNcs(): number { return this.ncs.filter(nc => nc.statut === 'OUVERTE').length; }
  get clientNcs(): number { return this.ncs.filter(nc => nc.origine === 'CLIENT').length; }
  get criticalNcs(): number { return this.ncs.filter(nc => nc.gravite === 'CRITIQUE').length; }

  getGraviteClass(gravite: string): string {
    return { 'CRITIQUE': 'badge-red', 'MAJEUR': 'badge-orange', 'MINEUR': 'badge-blue' }[gravite] ?? 'badge-gray';
  }

  getStatutClass(statut: string): string {
    return { 'OUVERTE': 'badge-gray', 'EN_TRAITEMENT': 'badge-orange', 'CLOTUREE': 'badge-green' }[statut] ?? 'badge-gray';
  }
  
  getOrigineClass(origine: string): string {
    return origine === 'CLIENT' ? 'badge-purple' : 'badge-blue';
  }

  onOrigineChange() {
    if (this.currentNc.origine === 'CLIENT') {
      this.currentNc.gravite = 'CRITIQUE'; // Ensure client is critical
    }
  }

  // --- MODAL & FORM LOGIC ---
  openAddModal() {
    this.isEditing = false;
    this.currentNc = {
      reference: '', origine: 'USINE', localisation: '', gravite: 'MINEUR', statut: 'OUVERTE',
      description: '', actionCorrective: '', procedureType: '', lotConcerne: '', valideeParClient: false
    };
    this.selectedFile = null;
    this.selectedFileName = '';
    this.aiSuggestion = null;
    this.showModal = true;
  }

  openEditModal(nc: NonConformity) {
    this.isEditing = true;
    this.currentNc = { ...nc };
    this.selectedFile = null;
    this.selectedFileName = '';
    this.showModal = true;
  }

  closeModal() {
    this.showModal = false;
  }

  onFileSelected(event: any) {
    const file = event.target.files[0];
    if (file) {
      this.selectedFile = file;
      this.selectedFileName = file.name;
    }
  }

  saveNc() {
    // Client enforcement
    if (this.currentNc.origine === 'CLIENT') {
      this.currentNc.gravite = 'CRITIQUE';
      if (!this.currentNc.lotConcerne) {
        this.notificationService.showError('Erreur', 'Le Numéro de Lot est obligatoire pour une réclamation Client.');
        return;
      }
    }

    if (this.currentNc.reference) this.currentNc.reference = this.currentNc.reference.trim();

    if (this.isEditing && this.currentNc.id) {
      this.ncService.update(this.currentNc.id, this.currentNc, this.selectedFile || undefined)
        .subscribe(() => {
          this.loadNcs();
          this.closeModal();
        });
    } else {
      this.ncService.create(this.currentNc, this.selectedFile || undefined)
        .subscribe(() => {
          this.loadNcs();
          this.closeModal();
        });
    }
  }

  // --- AI LOGIC ---
  onDescriptionChange() {
    if (!this.currentNc.description || this.currentNc.description.length < 10) {
      this.aiSuggestion = null;
      return;
    }
    
    if (this.analyzeTimeout) {
      clearTimeout(this.analyzeTimeout);
    }
    
    this.analyzeTimeout = setTimeout(() => {
      this.isAnalyzing = true;
      this.ncService.analyzeNC(this.currentNc.description, this.currentNc.localisation || '').subscribe({
        next: (res) => {
          this.isAnalyzing = false;
          if (res && res.suggested_root_cause) {
             this.aiSuggestion = res;
          } else {
             this.aiSuggestion = null;
          }
        },
        error: () => {
          this.isAnalyzing = false;
          this.aiSuggestion = null;
        }
      });
    }, 1000);
  }

  applyAiSuggestion() {
    if (this.aiSuggestion) {
      if (this.aiSuggestion.recommended_action) {
        this.currentNc.actionCorrective = this.aiSuggestion.recommended_action;
      }
      if (this.aiSuggestion.recommended_assignee) {
        this.currentNc.assigneeEmail = this.aiSuggestion.recommended_assignee;
      }
    }
  }

  async deleteNc(id?: number) {
    if (!id) return;
    const confirmed = await this.notificationService.confirmDelete(
      'Supprimer la NC ?',
      'Voulez-vous vraiment supprimer cette Non-Conformité ?'
    );
    if (confirmed) {
      this.ncService.delete(id).subscribe(() => {
        this.loadNcs();
        this.notificationService.showSuccess('Succès', 'Non-Conformité supprimée.');
      });
    }
  }

  // --- PDF VIEW LOGIC ---
  viewPdf(nc: NonConformity) {
    if (!nc.reportPdfPath) {
      this.notificationService.showInfo('Aucun document', 'Aucun fichier PDF associé.');
      return;
    }
    
    const pathLower = nc.reportPdfPath.toLowerCase();
    if (!pathLower.endsWith('.pdf')) {
      this.downloadPdf(nc);
      return;
    }

    this.currentPreviewNc = nc;
    this.ncService.downloadFile(nc.reportPdfPath).subscribe({
      next: (blob) => {
        const file = new Blob([blob], { type: 'application/pdf' });
        const fileURL = URL.createObjectURL(file);
        this.pdfUrlToView = this.sanitizer.bypassSecurityTrustResourceUrl(fileURL);
        this.showPdfModal = true;
      },
      error: (err) => {
        console.error('Erreur lors du téléchargement du PDF', err);
        this.notificationService.showError('Erreur', 'Impossible de charger le fichier.');
      }
    });
  }

  downloadPdf(nc: NonConformity | null) {
    if (!nc || !nc.reportPdfPath) return;
    
    this.ncService.downloadFile(nc.reportPdfPath).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = nc.reportPdfPath || 'document_nc';
        document.body.appendChild(a);
        a.click();
        document.body.removeChild(a);
        window.URL.revokeObjectURL(url);
      },
      error: (err) => {
        console.error('Erreur lors du téléchargement: ', err);
        this.notificationService.showError('Erreur', 'Impossible de télécharger le fichier.');
      }
    });
  }

  closePdfModal() {
    this.showPdfModal = false;
    this.pdfUrlToView = null;
    this.currentPreviewNc = null;
  }

  // --- WORKFLOW LOGIC ---
  showAssignModal = false;
  showTreatModal = false;
  workflowNc: NonConformity | null = null;
  assignEmailInput = '';
  actionCorrectiveInput = '';

  openAssignModal(nc: NonConformity) {
    this.workflowNc = nc;
    this.assignEmailInput = nc.assigneeEmail || '';
    this.showAssignModal = true;
  }

  closeAssignModal() {
    this.showAssignModal = false;
    this.workflowNc = null;
  }

  submitAssign() {
    if (this.workflowNc && this.workflowNc.id) {
       this.ncService.assign(this.workflowNc.id, this.assignEmailInput).subscribe(() => {
           this.loadNcs();
           this.closeAssignModal();
       });
    }
  }

  openTreatModal(nc: NonConformity) {
    this.workflowNc = nc;
    this.actionCorrectiveInput = nc.actionCorrective || '';
    this.showTreatModal = true;
  }

  closeTreatModal() {
    this.showTreatModal = false;
    this.workflowNc = null;
  }

  submitTreat() {
    if (this.workflowNc && this.workflowNc.id) {
       this.ncService.treat(this.workflowNc.id, this.actionCorrectiveInput).subscribe(() => {
           this.loadNcs();
           this.closeTreatModal();
       });
    }
  }

  async validateNc(nc: NonConformity) {
    if (nc.id) {
       const confirmed = await this.notificationService.confirmAction(
         'Clôturer la NC ?',
         'Confirmer la validation et la clôture de cette NC ?'
       );
       if (confirmed) {
         this.ncService.validateAndClose(nc.id).subscribe(() => {
             this.loadNcs();
             this.notificationService.showSuccess('Succès', 'NC clôturée avec succès.');
         });
       }
    }
  }
}
