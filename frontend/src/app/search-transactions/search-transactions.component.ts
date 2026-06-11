import { Component } from '@angular/core';
import { AccountService } from '../services/account.service';
import { TransactionResponse } from '../models/transaction-response.model';
import { CommonModule, DatePipe, NgClass, NgFor, NgIf } from '@angular/common';
import { FormsModule } from '@angular/forms';
// import { Router } from 'express';
import { Router } from '@angular/router';

@Component({
  selector: 'app-search-transactions',
  standalone: true,
  imports: [CommonModule, FormsModule, DatePipe, NgIf, NgFor, NgClass],
  templateUrl: './search-transactions.component.html',
  styleUrls: ['./search-transactions.component.css']
})
export class SearchTransactionsComponent {
  accountId: string = '';
  transactions: TransactionResponse[] = [];
  outgoing: TransactionResponse[] = [];
  incoming: TransactionResponse[] = [];
  failed: TransactionResponse[] = [];
  activeTab: 'all' | 'outgoing' | 'incoming' | 'failed' = 'all';
  loading = false;
  error: string | null = null;

  constructor(private accountService: AccountService, private router: Router) {}
  
  goBack(): void {
    this.router.navigate(['/dashboard']);
  }

  searchTransactions(): void {
    if (!this.accountId.trim()) {
      this.error = 'Please enter an account ID';
      return;
    }
    this.loading = true;
    this.error = null;
    this.accountService.getTransactionsById(this.accountId).subscribe({
      next: (txs) => {
        this.transactions = txs;
        this.outgoing = txs.filter(t => t.fromAccountId === this.accountId && t.status === 'SUCCESS');
        this.incoming = txs.filter(t => t.toAccountId === this.accountId && t.status === 'SUCCESS');
        this.failed = txs.filter(t => t.status === 'FAILED');
        this.loading = false;
      },
      error: () => {
        this.error = 'No transactions found';
        this.transactions = [];
        this.outgoing = [];
        this.incoming = [];
        this.failed = [];
        this.loading = false;
      }
    });
  }

  setTab(tab: 'all' | 'outgoing' | 'incoming' | 'failed'): void {
    this.activeTab = tab;
  }
}
