import { Component, OnDestroy, OnInit } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormBuilder, FormGroup, ReactiveFormsModule, Validators, AbstractControl, ValidationErrors } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { TransferService } from '../services/transfer.service';
import { AccountService } from '../services/account.service';
import { TransactionResponse } from '../models/transaction-response.model';
import { Router } from '@angular/router';

interface UiCategoryOption {
  value: string;
  label: string;
}

@Component({
  selector: 'app-transfer-money',
  standalone: true,
  imports: [CommonModule, ReactiveFormsModule],
  templateUrl: './transfer.component.html',
  styleUrls: ['./transfer.component.css'],
})
export class TransferComponent implements OnInit, OnDestroy {
  form: FormGroup;
  submitting = false;
  serverErrorMessage: string | null = null;
  successMessage: string | null = null;

  receiptVisible = false;
  receiptData: TransactionResponse | null = null;
  countdownSeconds = 0;
  private countdownTimer: any;

  accountLocked = false;
  showLockedModal = false;

  categories: UiCategoryOption[] = [
    { value: 'GROCERY', label: 'Grocery' },
    { value: 'STATIONERY', label: 'Stationery' },
    { value: 'RENT', label: 'Rent' },
    { value: 'SALARY', label: 'Salary' },
    { value: 'UTILITIES', label: 'Utilities' },
    { value: 'ENTERTAINMENT', label: 'Entertainment' },
    { value: 'OTHER', label: 'Other' },
  ];

  constructor(private fb: FormBuilder, private transferService: TransferService, private accountService: AccountService, private router: Router) {
    this.form = this.fb.group({
      toAccountId: ['', [Validators.required, this.accountIdValidator]],
      amount: [null, [Validators.required, Validators.min(0.01), this.amountPrecisionValidator]],
      category: ['RENT', [Validators.required]],
      note: ['', [Validators.maxLength(300)]],
    });
  }

  ngOnInit(): void {
    this.accountService.getMyDetails().subscribe({
      next: (account) => {
        if (account.status === 'LOCKED') {
          this.accountLocked = true;
          this.showLockedModal = true;
        }
      },
      error: () => {
        // Silently ignore - status check is a soft safeguard only
      }
    });
  }

  closeLockedModal(): void {
    this.showLockedModal = false;
  }

  // Custom validator for account ID format (ACC followed by 4 digits)
  accountIdValidator(control: AbstractControl): ValidationErrors | null {
    if (!control.value) {
      return null; // Let required validator handle empty value
    }
    const pattern = /^ACC\d{4}$/;
    return pattern.test(control.value) ? null : { invalidAccountId: true };
  }

  private amountPrecisionValidator(control: AbstractControl): ValidationErrors | null {
    const value = control.value;
    if (value === null || value === undefined || value === '') {
      return null;
    }

    const valueString = String(value);
    const decimalPart = valueString.split('.')[1];
    if (decimalPart && decimalPart.length > 2) {
      return { decimalPlaces: true };
    }

    return null;
  }

  onAmountInput(event: Event): void {
    const input = event.target as HTMLInputElement;
    if (!input.value.includes('.')) {
      return;
    }

    const [whole, fraction] = input.value.split('.');
    if (fraction.length > 2) {
      const truncated = `${whole}.${fraction.slice(0, 2)}`;
      input.value = truncated;
      this.form.get('amount')?.setValue(Number(truncated), { emitEvent: false });
    }
  }

  goBack(): void {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
    }
    this.receiptVisible = false;
    this.receiptData = null;
    this.countdownSeconds = 0;
    this.router.navigate(['/dashboard']);
  }

  get toAccountId() { return this.form.get('toAccountId')!; }
  get amount() { return this.form.get('amount')!; }
  get category() { return this.form.get('category')!; }
  get note() { return this.form.get('note')!; }

  private generateIdempotencyKey(): string {
    return 'tx-' + Date.now() + '-' + Math.random().toString(16).slice(2);
  }

  onSubmit(): void {
    if (this.accountLocked) {
      this.showLockedModal = true;
      return;
    }

    if (this.form.invalid || this.submitting) {
      this.form.markAllAsTouched();
      return;
    }

    this.submitting = true;
    this.serverErrorMessage = null;
    this.successMessage = null;

    const body = {
      toAccountId: this.toAccountId.value,
      amount: this.amount.value,
      category: this.category.value,
      note: this.note.value,
      idempotencyKey: this.generateIdempotencyKey(),
    };

    this.transferService.transferAsUser(body).subscribe({
      next: (res) => {
        this.submitting = false;
        this.receiptData = res;
        this.showReceiptWithCountdown(10);
        this.form.reset({ toAccountId: '', amount: null, category: 'RENT', note: '' });
      },
      error: (err: HttpErrorResponse) => {
        this.submitting = false;
        this.handleServerError(err);
      },
    });
  }

  private showReceiptWithCountdown(seconds: number): void {
    this.receiptVisible = true;
    this.countdownSeconds = seconds;
    if (this.countdownTimer) clearInterval(this.countdownTimer);

    this.countdownTimer = setInterval(() => {
      this.countdownSeconds -= 1;
      if (this.countdownSeconds <= 0) {
        clearInterval(this.countdownTimer);
        this.receiptVisible = false;
        this.receiptData = null;
        this.router.navigate(['/transfer']);
      }
    }, 1000);
  }

  private handleServerError(err: HttpErrorResponse): void {
    let message: string;

    if (typeof err.error === 'string') {
      // backend returned plain string
      message = err.error;
    } else if (err.error && (err.error.message || err.error.failureReason)) {
      // backend returned JSON object
      message = err.error.message || err.error.failureReason;
    } else {
      message = err.message || 'Transaction failed';
    }

    this.serverErrorMessage = message;
  }

  isControlInvalid(controlName: string): boolean {
    const ctrl = this.form.get(controlName);
    return !!ctrl && ctrl.invalid && (ctrl.touched || ctrl.dirty);
  }

  isSubmitDisabled(): boolean {
    return this.form.invalid || this.submitting || this.accountLocked;
  }

  ngOnDestroy(): void {
    if (this.countdownTimer) {
      clearInterval(this.countdownTimer);
      this.countdownTimer = null;
    }
  }
}