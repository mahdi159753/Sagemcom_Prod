import { Component, OnInit, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { ProduitService } from '../../services/produit.service';
import { Produit } from '../../models/models';
import { AuthService } from '../../services/auth.service';
import { NotificationService } from '../../services/notification.service';
import { DomSanitizer, SafeResourceUrl } from '@angular/platform-browser';

@Component({
  selector: 'app-instruction-sheets',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './instruction-sheets.component.html',
  styleUrls: ['./instruction-sheets.component.scss']
})
export class InstructionSheetsComponent implements OnInit {
  produits: Produit[] = [];
  filtered: Produit[] = [];
  search = '';
  statusFilter = 'all';

  // Modal states
  showModal = false;
  isEditing = false;
  selectedFile: File | null = null;
  selectedFileName = '';
  
  // Categories logic
  availableCategories: string[] = ['Gateway', 'Router'];
  newCategoryInputValue = '';
  
  // PDF Preview State
  showPdfModal = false;
  pdfUrlToView: SafeResourceUrl | null = null;

  // Form Model
  currentProduit: Produit = {
    name: '',
    reference: '',
    category: '',
    status: 'active',
    targetRate: 0,
    cycleTime: 0,
    totalProduced: 0
  };

  private produitService = inject(ProduitService);
  public authService = inject(AuthService); // Public for template access to role checks
  private sanitizer = inject(DomSanitizer);
  private notificationService = inject(NotificationService);

  ngOnInit() {
    this.loadProduits();
  }

  loadProduits() {
    this.produitService.getAll().subscribe({
      next: (data) => {
        this.produits = data;
        this.extractCategories();
        this.applyFilter();
      },
      error: (err) => {
        console.error('Erreur lors du chargement des produits', err);
        // Fallback or handle later
      }
    });
  }

  extractCategories() {
    const cats = this.produits.map(p => p.category).filter(c => c && c.trim() !== '');
    this.availableCategories = Array.from(new Set([...cats, 'Gateway', 'Router']));
  }

  applyFilter() {
    this.filtered = this.produits.filter(p => {
      const matchSearch = p.name.toLowerCase().includes(this.search.toLowerCase()) ||
                          p.reference.toLowerCase().includes(this.search.toLowerCase());
      const matchStatus = this.statusFilter === 'all' || p.status === this.statusFilter;
      return matchSearch && matchStatus;
    });
  }

  // --- STATS LOGIC ---
  get totalProducts(): number { return this.produits.length; }
  get activeProducts(): number { return this.produits.filter(p => p.status === 'active').length; }
  get avgCycleTime(): number { 
    if (this.totalProducts === 0) return 0;
    const sum = this.produits.reduce((acc, p) => acc + p.cycleTime, 0);
    return Math.round(sum / this.totalProducts);
  }

  getStatusClass(status: string): string {
    return { 'active': 'badge-green', 'development': 'badge-blue', 'archived': 'badge-gray' }[status] ?? 'badge-gray';
  }

  canEdit(): boolean {
    return this.authService.isAdmin() || this.authService.isIngenieur();
  }

  // --- MODAL & FORM LOGIC ---
  openAddModal() {
    this.isEditing = false;
    this.currentProduit = {
      name: '', reference: '', category: '', status: 'active', targetRate: 0, cycleTime: 0, totalProduced: 0
    };
    if (this.availableCategories.length > 0) {
      this.currentProduit.category = this.availableCategories[0];
    }
    this.newCategoryInputValue = '';
    this.selectedFile = null;
    this.selectedFileName = '';
    this.showModal = true;
  }

  openEditModal(p: Produit) {
    this.isEditing = true;
    this.currentProduit = { ...p };
    
    // Check if category is in list, otherwise add it temporarily
    if (this.currentProduit.category && !this.availableCategories.includes(this.currentProduit.category)) {
      this.availableCategories.push(this.currentProduit.category);
    }
    
    this.newCategoryInputValue = '';
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

  saveProduit() {
    if (this.currentProduit.category === 'other' && this.newCategoryInputValue.trim() !== '') {
      this.currentProduit.category = this.newCategoryInputValue.trim();
    } else if (this.currentProduit.category === 'other') {
      this.currentProduit.category = 'Unknown';
    }

    // Ensure fields are perfectly trimmed so the backend finds exact matches for the upsert logic
    if (this.currentProduit.name) this.currentProduit.name = this.currentProduit.name.trim();
    if (this.currentProduit.reference) this.currentProduit.reference = this.currentProduit.reference.trim();

    if (this.isEditing && this.currentProduit.id) {
      this.produitService.update(this.currentProduit.id, this.currentProduit, this.selectedFile || undefined)
        .subscribe(() => {
          this.loadProduits();
          this.closeModal();
        });
    } else {
      this.produitService.create(this.currentProduit, this.selectedFile || undefined)
        .subscribe(() => {
          this.loadProduits();
          this.closeModal();
        });
    }
  }

  async deleteProduit(id?: number) {
    if (!id) return;
    const confirmed = await this.notificationService.confirmDelete(
      'Supprimer le produit ?',
      'Voulez-vous vraiment supprimer ce produit et ses instructions ?'
    );
    if (confirmed) {
      this.produitService.delete(id).subscribe(() => {
        this.loadProduits();
        this.notificationService.showSuccess('Succès', 'Produit supprimé.');
      });
    }
  }

  // --- PDF VIEW LOGIC ---
  viewPdf(produit: Produit) {
    if (!produit.instructionPdfPath) {
      this.notificationService.showInfo('Aucun document', 'Aucun fichier PDF/Doc associé à cette instruction.');
      return;
    }
    
    const pathLower = produit.instructionPdfPath.toLowerCase();
    if (!pathLower.endsWith('.pdf')) {
      // Browsers cannot render Word/Excel files in iframes directly.
      // Trigger download immediately instead of showing broken viewer.
      this.downloadPdf(produit);
      return;
    }

    // Security Fix: An iframe cannot send the JWT authorization header! 
    // We must download the PDF as a Blob first, then create an object URL for the iframe.
    this.produitService.downloadFile(produit.instructionPdfPath).subscribe({
      next: (blob) => {
        const file = new Blob([blob], { type: 'application/pdf' });
        const fileURL = URL.createObjectURL(file);
        this.pdfUrlToView = this.sanitizer.bypassSecurityTrustResourceUrl(fileURL);
        this.showPdfModal = true;
      },
      error: (err) => {
        console.error('Erreur lors du téléchargement du PDF', err);
        this.notificationService.showError('Erreur', 'Impossible de charger le fichier (Accès refusé ou introuvable).');
      }
    });
  }

  downloadPdf(produit: Produit) {
    if (!produit.instructionPdfPath) {
      this.notificationService.showInfo('Aucun document', 'Aucun fichier PDF associé à cette instruction.');
      return;
    }
    
    // Security Fix: We must download the file as a Blob via HttpClient to pass the JWT Bearer token
    this.produitService.downloadFile(produit.instructionPdfPath).subscribe({
      next: (blob) => {
        const url = window.URL.createObjectURL(blob);
        const a = document.createElement('a');
        a.href = url;
        a.download = produit.instructionPdfPath || 'document';
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
  }
}
