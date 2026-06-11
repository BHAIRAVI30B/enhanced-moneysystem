import { Component } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';
import { Account } from '../models/account.model';
import { AccountService } from '../services/account.service';

@Component({
  selector: 'app-search-account',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './search-account.component.html',
  styleUrls: ['./search-account.component.css']
})
export class SearchAccountComponent {
  accountId = '';
  account: Account | null = null;
  loading = false;
  error: string | null = null;

  constructor(private accountService: AccountService) {}

  searchAccount() {
    if (!this.accountId.trim()) {
      this.error = 'Please enter an account ID';
      return;
    }
    this.loading = true;
    this.error = null;
    this.accountService.getAccountById(this.accountId).subscribe({
      next: (res) => {
        this.account = res;
        this.loading = false;
      },
      error: () => {
        this.error = 'Account not found';
        this.account = null;
        this.loading = false;
      }
    });
  }
}
