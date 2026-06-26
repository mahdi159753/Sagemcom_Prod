// src/app/app.config.ts
import { ApplicationConfig } from '@angular/core';
import { provideRouter } from '@angular/router';
import { provideAnimations } from '@angular/platform-browser/animations';
import { provideHttpClient, withInterceptors } from '@angular/common/http';
import { routes } from './app.routes';
import { JwtInterceptor } from './interceptors/jwt.interceptor';
 
export const appConfig: ApplicationConfig = {
  providers: [
    provideRouter(routes),
    provideAnimations(),
    // FIX: use withInterceptors() to register the functional JwtInterceptor
    // withInterceptorsFromDi() only works for class-based interceptors (HTTP_INTERCEPTORS token)
    provideHttpClient(withInterceptors([JwtInterceptor]))
  ]
};
 