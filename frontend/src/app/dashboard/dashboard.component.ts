import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AccountService } from '../services/account.service';
import { Account } from '../models/account.model';
import { JwtResponse } from '../models/jwt-response.model';
import { CurrencyPipe, NgIf } from '@angular/common';

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NgIf, CurrencyPipe],
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
    private accountService: AccountService
  ) {}

  ngOnInit(): void {
    // Retrieve JWT response from localStorage
    const storedUser = localStorage.getItem('user');
    if (storedUser) {
      const jwtResponse: JwtResponse = JSON.parse(storedUser);
      this.username = jwtResponse.username;
      this.role = jwtResponse.roles[0]; // assume single role

      if (this.role === 'ROLE_USER') {
        // Fetch account details for user
        this.accountService.getMyDetails().subscribe({
          next: (acc) => (this.account = acc),
          error: (err) => console.error('Failed to fetch account details', err)
        });
      }
    }
  }

  logout(): void {
    this.authService.logout();
    localStorage.removeItem('user'); // clear user info
    this.router.navigate(['/login']);
  }

  goTo(path: string): void {
    this.router.navigate([path]);
  }
}
