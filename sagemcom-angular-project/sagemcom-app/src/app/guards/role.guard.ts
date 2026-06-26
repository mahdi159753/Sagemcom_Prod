// src/app/guards/role.guard.ts
import { CanActivateFn, Router, ActivatedRouteSnapshot } from '@angular/router';
import { inject } from '@angular/core';
import { AuthService } from '../services/auth.service';
import { Role } from '../models/models';

/**
 * Usage in routes:
 *   canActivate: [AuthGuard, RoleGuard],
 *   data: { roles: ['ADMIN'] }
 */
export const RoleGuard: CanActivateFn = (route: ActivatedRouteSnapshot) => {
  const auth   = inject(AuthService);
  const router = inject(Router);

  const allowedRoles: Role[] = route.data['roles'] ?? [];
  if (allowedRoles.length === 0) return true;           // no restriction

  if (auth.hasRole(...allowedRoles)) return true;

  // Redirect to dashboard (or a 403 page) instead of login
  router.navigate(['/dashboard']);
  return false;
};
