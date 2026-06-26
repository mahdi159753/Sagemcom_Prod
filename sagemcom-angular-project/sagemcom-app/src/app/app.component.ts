import { Component, inject } from '@angular/core';
import { RouterOutlet } from '@angular/router';

import { PresenceService } from './services/presence.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  template: `<router-outlet />`
})
export class AppComponent {
  private presenceService = inject(PresenceService);
}
