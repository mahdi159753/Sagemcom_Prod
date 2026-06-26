import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { User, Role, UserFormData } from '../../models/models';
import { AuthService } from 'src/app/services/auth.service';
import { NotificationService } from 'src/app/services/notification.service';

@Component({
  selector: 'app-utilisateurs',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './utilisateurs.component.html',
  styleUrls: ['./utilisateurs.component.scss'],
})
export class UtilisateursComponent implements OnInit {

  
  users: User[] = [];
  searchTerm = '';
  roleFilter = 'all';
  statutFilter = 'all';
  showModal = false;
  editingUser: User | null = null;

  // FIX: added 'password' field for new user creation
  formData: UserFormData & { password?: string } = {
    nom: '',
    prenom: '',
    email: '',
    password: '',
    role: 'PREPARATEUR',
    ligne: '',
    poste: '',
    statut: 'Actif',
  };

  constructor(private authService: AuthService, private notificationService: NotificationService) {}

  ngOnInit(): void {
    this.loadUsers();
  }

  loadUsers() {
    this.authService.getAll().subscribe({
      next: (data: any[]) => {
        // FIX: map backend User fields correctly (id is Integer from Java)
        this.users = data.map(u => ({
          id: u.id as number,
          nom: u.lastname ?? '',
          prenom: u.firstname ?? '',
          email: u.email ?? '',
          role: u.role as Role,
          ligne: u.ligne ?? '',
          poste: u.poste ?? '',
          statut: u.actif ? 'Actif' : 'Inactif',
          dateCreation: u.dateCreation ?? new Date().toISOString(),
          dernierAcces: u.dernierAcces ?? '-',
          lastActive: u.lastActive,
        }));
      },
      error: err => console.error('Erreur chargement utilisateurs:', err)
    });
  }

  get filteredUsers(): User[] {
    return this.users.filter(user => {
      const term = this.searchTerm.toLowerCase();
      const matchesSearch =
        user.nom.toLowerCase().includes(term) ||
        user.prenom.toLowerCase().includes(term) ||
        user.email.toLowerCase().includes(term);
      const matchesRole =
        this.roleFilter === 'all' || user.role === this.roleFilter;
      const matchesStatut =
        this.statutFilter === 'all' || user.statut === this.statutFilter;
      return matchesSearch && matchesRole && matchesStatut;
    });
  }

  get stats() {
    return {
      total:        this.users.length,
      actifs:       this.users.filter(u => u.statut === 'Actif').length,
      admins:       this.users.filter(u => u.role === 'ADMIN').length,
      responsables: this.users.filter(u => u.role === 'RESPONSABLE_PRODUCTION').length,
      ingenieurs:   this.users.filter(u => u.role === 'INGENIEUR_QUALITE').length,
      preparateurs: this.users.filter(u => u.role === 'PREPARATEUR').length,
    };
  }

  get showLignePoste(): boolean {
    return this.formData.role === 'RESPONSABLE_PRODUCTION' || this.formData.role === 'PREPARATEUR';
  }

  get showPoste(): boolean {
    return this.formData.role === 'PREPARATEUR';
  }

  getRoleLabel(role: Role): string {
    const labels: Record<Role, string> = {
      ADMIN: 'Administrateur',
      RESPONSABLE_PRODUCTION: 'Responsable Production',
      INGENIEUR_QUALITE: 'Ingénieur Qualité',
      PREPARATEUR: 'Préparateur',
    };
    return labels[role] ?? role;
  }

  getRoleClass(role: Role): string {
    const classes: Record<Role, string> = {
      ADMIN: 'badge-admin',
      RESPONSABLE_PRODUCTION: 'badge-responsable',
      INGENIEUR_QUALITE: 'badge-ingenieur',
      PREPARATEUR: 'badge-preparateur',
    };
    return classes[role] ?? '';
  }

  isUserOnline(lastActiveStr?: string): boolean {
    if (!lastActiveStr) return false;
    const lastActive = new Date(lastActiveStr).getTime();
    const now = new Date().getTime();
    const diffMin = (now - lastActive) / (1000 * 60);
    return diffMin < 5;
  }

  formatLastActive(lastActiveStr?: string): string {
    if (!lastActiveStr) return 'Jamais connecté';
    const lastActive = new Date(lastActiveStr).getTime();
    const now = new Date().getTime();
    const diffSec = Math.floor((now - lastActive) / 1000);
    const diffMin = Math.floor(diffSec / 60);
    const diffHour = Math.floor(diffMin / 60);
    const diffDay = Math.floor(diffHour / 24);

    if (diffMin < 5) return 'En ligne';
    if (diffMin < 60) return `il y a ${diffMin} min`;
    if (diffHour < 24) return `il y a ${diffHour}h`;
    return `il y a ${diffDay} jour${diffDay > 1 ? 's' : ''}`;
  }

  openModal(user?: User): void {
    if (user) {
      this.editingUser = { ...user };
      this.formData = {
        nom:      user.nom,
        prenom:   user.prenom,
        email:    user.email,
        password: '',          // never pre-fill password
        role:     user.role,
        ligne:    user.ligne ?? '',
        poste:    user.poste ?? '',
        statut:   user.statut,
      };
    } else {
      this.editingUser = null;
      this.formData = {
        nom:      '',
        prenom:   '',
        email:    '',
        password: '',
        role:     'PREPARATEUR',
        ligne:    '',
        poste:    '',
        statut:   'Actif',
      };
    }
    this.showModal = true;
  }

  closeModal(): void {
    this.showModal = false;
    this.editingUser = null;
  }

  saveUser(): void {
    if (this.editingUser) {
      // UPDATE: send to /api/v1/users/{id}
      const payload = {
        firstname: this.formData.prenom,
        lastname:  this.formData.nom,
        email:     this.formData.email,
        role:      this.formData.role,
        poste:     this.formData.poste  || null,
        matricule: this.formData.poste  || null,   // reuse poste as matricule if not separate
        actif:     this.formData.statut === 'Actif',
      };
      this.authService.update(this.editingUser.id, payload).subscribe({
        next: () => { 
          this.loadUsers(); 
          this.closeModal(); 
          this.notificationService.showSuccess('Succès', 'Utilisateur mis à jour.');
        },
        error: err => {
          console.error('Erreur mise à jour:', err);
          this.notificationService.showError('Erreur', 'Impossible de mettre à jour l\'utilisateur.');
        }
      });
    } else {
      // CREATE: calls /register — uses custom password if set, else default 'Temp@1234'
      const payload: any = {
        firstname: this.formData.prenom,
        lastname:  this.formData.nom,
        email:     this.formData.email,
        role:      this.formData.role,
        poste:     this.formData.poste || null,
        matricule: this.formData.poste || null,
      };
      if (this.formData.password && this.formData.password.trim()) {
        payload.password = this.formData.password.trim();
      }
      this.authService.create(payload).subscribe({
        next: () => { 
          this.loadUsers(); 
          this.closeModal(); 
          this.notificationService.showSuccess('Succès', 'Utilisateur créé.');
        },
        error: err => {
          console.error('Erreur création:', err);
          this.notificationService.showError('Erreur', 'Impossible de créer l\'utilisateur.');
        }
      });
    }
  }

  async deleteUser(userId: number) {
    const confirmed = await this.notificationService.confirmDelete(
      'Supprimer l\'utilisateur ?',
      'Êtes-vous sûr de vouloir supprimer cet utilisateur ?'
    );
    if (confirmed) {
      this.authService.delete(userId).subscribe({
        next: () => {
          this.loadUsers();
          this.notificationService.showSuccess('Succès', 'Utilisateur supprimé.');
        },
        error: err => {
          console.error('Erreur suppression:', err);
          this.notificationService.showError('Erreur', 'Impossible de supprimer cet utilisateur.');
        }
      });
    }
  }

  toggleStatut(user: User): void {
    const payload = {
      firstname: user.prenom,
      lastname:  user.nom,
      email:     user.email,
      role:      user.role,
      poste:     user.poste  || null,
      matricule: user.poste  || null,
      actif:     user.statut !== 'Actif',  // toggle
    };
    // FIX: user.id is already number — no casting needed
    this.authService.update(user.id, payload).subscribe({
      next: () => {
        this.loadUsers();
        this.notificationService.showSuccess('Succès', 'Statut mis à jour.');
      },
      error: err => {
        console.error('Erreur toggle statut:', err);
        this.notificationService.showError('Erreur', 'Impossible de changer le statut.');
      }
    });
  }
}
