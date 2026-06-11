import { Component, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Account } from '../models/account.model';
import { AccountService } from '../services/account.service';
import { Router } from '@angular/router';

@Component({
  selector: 'app-details',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './details.component.html',
  styleUrls: ['./details.component.css']
})
export class DetailsComponent implements OnInit {
  account: Account | null = null;
  loading = true;
  error: string | null = null;

  constructor(private accountService: AccountService, private router: Router) {}
  
    goBack(): void{
      this.router.navigate(['/dashboard']);
    }

  ngOnInit(): void {
    this.accountService.getMyDetails().subscribe({
      next: (res) => {
        this.account = res;
        this.loading = false;
      },
      error: () => {
        this.error = 'Failed to load account details';
        this.loading = false;
      }
    });
  }
}
