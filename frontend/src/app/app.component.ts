import { Component, OnInit } from '@angular/core';
import { RouterOutlet } from '@angular/router';
import { SessionSocketService } from './services/sessionsocket.service';
import { AuthService } from './services/auth.service';

@Component({
  selector: 'app-root',
  standalone: true,
  imports: [RouterOutlet],
  templateUrl: './app.component.html',
  styleUrl: './app.component.css'
})
export class AppComponent implements OnInit {
  title = 'frontend';

  constructor(
    private sessionSocket: SessionSocketService,
    private authService: AuthService
  ) {}

  ngOnInit(): void {
    // On every app/tab load, reconnect the WebSocket if already logged in.
    // This covers: same device new tab, page refresh, browser restart.
    if (this.authService.isLoggedIn()) {
      this.sessionSocket.connect();
    }
  }
}
