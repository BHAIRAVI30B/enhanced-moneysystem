import { Component, OnInit } from '@angular/core';
import { AccountService } from '../services/account.service';
import { TransactionResponse } from '../models/transaction-response.model';
import { Account } from '../models/account.model';
import { DatePipe, DecimalPipe, NgClass, NgFor, NgIf } from '@angular/common';
import { Router } from '@angular/router';

@Component({
  selector: 'app-history',
  standalone: true,
  imports:[DatePipe,NgIf,NgFor,NgClass, DecimalPipe],
  templateUrl: './history.component.html',
  styleUrls: ['./history.component.css']
})
export class HistoryComponent implements OnInit {
  transactions: TransactionResponse[] = [];
  outgoing: TransactionResponse[] = [];
  incoming: TransactionResponse[] = [];
  failed: TransactionResponse[] = [];
  activeTab: 'all' | 'outgoing' | 'incoming' | 'failed' = 'all';
  myAccountId: string = '';

  constructor(private accountService: AccountService, private router: Router) {}

  goBack(): void{
    this.router.navigate(['/dashboard']);
  }

  ngOnInit(): void {
    this.accountService.getMyDetails().subscribe({
      next: (account: Account) => {
        this.myAccountId = account.accountId;

        this.accountService.getMyTransactions().subscribe({
          next: (txs) => {
            this.transactions = txs;
            this.outgoing = txs.filter(t => t.fromAccountId === this.myAccountId && t.status === 'SUCCESS');
            this.incoming = txs.filter(t => t.toAccountId === this.myAccountId && t.status === 'SUCCESS');
            this.failed = txs.filter(t => t.status === 'FAILED');
          },
          error: (err) => console.error('Failed to fetch transactions', err)
        });
      },
      error: (err) => console.error('Failed to fetch account details', err)
    });
  }

  setTab(tab: 'all' | 'outgoing' | 'incoming' | 'failed'): void {
    this.activeTab = tab;
  }
}
