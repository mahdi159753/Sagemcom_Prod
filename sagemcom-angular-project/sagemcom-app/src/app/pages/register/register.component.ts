import { Component } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CommonModule, NgIf } from '@angular/common';
import { Router } from '@angular/router';
import { AuthService } from '../../services/auth.service';

@Component({
  selector: 'app-register',
  standalone: true,
  imports: [FormsModule, CommonModule, NgIf],
  templateUrl: './register.component.html',
  styleUrls: ['./register.component.scss']
})
export class RegisterComponent {
  firstname = '';
  lastname = '';
  email = '';
  password = '';
  poste = '';
  matricule = '';
  role = '';
  error = '';

  constructor(private authService: AuthService, public router: Router) {} // <-- router is public

  onSubmit() {
    this.authService.register(
    this.firstname,
    this.lastname,
    this.email,
    this.password,
    this.role || 'USER',
    this.poste , 
    this.matricule)
      .subscribe({
        next: () => this.router.navigate(['/login']),
        error: () => this.error = 'Registration failed'
      });
  }

  goToLogin() {
    this.router.navigate(['/login']);
  }
}