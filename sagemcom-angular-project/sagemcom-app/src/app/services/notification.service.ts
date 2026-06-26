import { Injectable } from '@angular/core';
import Swal from 'sweetalert2';
import { Subject, BehaviorSubject } from 'rxjs';

export interface AppNotification {
  id: number;
  type: string;
  message: string;
  createdAt: string;
  read: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class NotificationService {

  public unread$ = new BehaviorSubject<AppNotification[]>([]);
  public toast$ = new Subject<any>();

  constructor() { }

  markAsRead(id: number) {
    const current = this.unread$.value;
    this.unread$.next(current.filter(n => n.id !== id));
  }

  markAllAsRead() {
    this.unread$.next([]);
  }

  /**
   * Show a beautiful confirmation dialog.
   * Returns a Promise that resolves to true if confirmed, false otherwise.
   */
  async confirmAction(title: string, text: string = '', confirmButtonText = 'Oui, confirmer', cancelButtonText = 'Annuler'): Promise<boolean> {
    const result = await Swal.fire({
      title: title,
      text: text,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3b82f6', // Primary Blue
      cancelButtonColor: '#94a3b8', // Gray
      confirmButtonText: confirmButtonText,
      cancelButtonText: cancelButtonText,
      customClass: {
        popup: 'sweet-glass-popup',
        confirmButton: 'sweet-btn-confirm',
        cancelButton: 'sweet-btn-cancel'
      }
    });

    return result.isConfirmed;
  }

  /**
   * Show a beautiful destructive confirmation dialog.
   */
  async confirmDelete(title: string, text: string = '', confirmButtonText = 'Oui, supprimer', cancelButtonText = 'Annuler'): Promise<boolean> {
    const result = await Swal.fire({
      title: title,
      text: text,
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#ef4444', // Danger Red
      cancelButtonColor: '#94a3b8', // Gray
      confirmButtonText: confirmButtonText,
      cancelButtonText: cancelButtonText,
      customClass: {
        popup: 'sweet-glass-popup',
        confirmButton: 'sweet-btn-danger',
        cancelButton: 'sweet-btn-cancel'
      }
    });

    return result.isConfirmed;
  }

  /**
   * Show a success toast/alert
   */
  showSuccess(title: string, text: string = '') {
    Swal.fire({
      title: title,
      text: text,
      icon: 'success',
      timer: 3000,
      showConfirmButton: false,
      toast: true,
      position: 'top-end',
      customClass: {
        popup: 'sweet-glass-toast'
      }
    });
  }

  /**
   * Show an error alert
   */
  showError(title: string, text: string = '') {
    Swal.fire({
      title: title,
      text: text,
      icon: 'error',
      confirmButtonColor: '#3b82f6',
      customClass: {
        popup: 'sweet-glass-popup'
      }
    });
  }

  /**
   * Show an info toast
   */
  showInfo(title: string, text: string = '') {
    Swal.fire({
      title: title,
      text: text,
      icon: 'info',
      timer: 3000,
      showConfirmButton: false,
      toast: true,
      position: 'top-end',
      customClass: {
        popup: 'sweet-glass-toast'
      }
    });
  }
}
