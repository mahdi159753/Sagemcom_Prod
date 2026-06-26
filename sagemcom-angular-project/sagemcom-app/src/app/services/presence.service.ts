import { Injectable, NgZone } from '@angular/core';
import { AuthService } from './auth.service';
import { Subject, Subscription } from 'rxjs';
import { throttleTime } from 'rxjs/operators';

@Injectable({
  providedIn: 'root'
})
export class PresenceService {
  private activitySubject = new Subject<void>();
  private pingSubscription?: Subscription;

  constructor(
    private authService: AuthService,
    private ngZone: NgZone
  ) {
    this.setupListeners();
    this.setupPingInterval();
  }

  private setupListeners() {
    this.ngZone.runOutsideAngular(() => {
      const events = ['mousemove', 'keydown', 'click', 'scroll', 'touchstart'];
      events.forEach(eventName => {
        window.addEventListener(eventName, () => {
          if (this.authService.isLoggedIn()) {
            this.activitySubject.next();
          }
        }, { passive: true });
      });
    });
  }

  private setupPingInterval() {
    // Throttle to max 1 ping every 60 seconds (60000ms)
    this.pingSubscription = this.activitySubject
      .pipe(
        throttleTime(60000)
      )
      .subscribe(() => {
        this.ngZone.run(() => {
          this.authService.pingActivity().subscribe({
             next: () => {},
             error: err => console.error('Erreur ping', err)
          });
        });
      });
  }
}
