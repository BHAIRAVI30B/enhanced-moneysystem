import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AccountService } from '../services/account.service';
import { SessionSocketService } from '../services/sessionsocket.service';
import { Account } from '../models/account.model';
import { CurrencyPipe, NgIf, NgFor, CommonModule } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NgIf, NgFor, CurrencyPipe, CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  username: string = '';
  role: string = '';
  account: Account | null = null;

  constructor(
    private router: Router,
    private authService: AuthService,
    private accountService: AccountService,
    private sessionSocket: SessionSocketService
  ) {}

  ngOnInit(): void {
    const token = this.authService.getToken();
    if (!token) { this.router.navigate(['/login']); return; }

    const payload = JSON.parse(atob(token.split('.')[1]));
    this.username = payload.sub;
    this.role = payload.roles?.[0] ?? '';

    if (this.role === 'ROLE_USER') {
      this.accountService.getMyDetails().subscribe({
        next: (acc) => (this.account = acc),
        error: (err) => console.error('Failed to fetch account details', err)
      });
    }
  }

  viewAnalytics(): void {
    this.router.navigate(['/analytics']);
  }

  logout(): void {
    this.sessionSocket.disconnect();
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goTo(path: string): void {
    this.router.navigate([path]);
  }
}