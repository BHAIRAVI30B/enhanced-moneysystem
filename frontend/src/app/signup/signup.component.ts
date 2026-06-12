import { Component } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { SignupRequest } from '../models/signup-request.model';
import { UsernameValidator } from '../validators/username-validator';

@Component({
  selector: 'app-signup',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink],
  templateUrl: './signup.component.html',
  styleUrls: ['./signup.component.css']
})
export class SignupComponent {
  signupForm: FormGroup;
  errorMessage: string | null = null;
  showPassword: boolean = false; // NEW: password visibility toggle

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router
  ) {
    this.signupForm = this.fb.group({
      username: [
        '',
        [
          Validators.required,
          Validators.minLength(4),
          Validators.maxLength(25),
          Validators.pattern(/^[a-zA-Z0-9_]+$/)
        ],
        [UsernameValidator.checkUsername(this.authService)]
      ],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(15),
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).*$/)
        ]
      ],
      holderName: [
        '',
        [
          Validators.required,
          Validators.minLength(4),
          Validators.maxLength(15),
          Validators.pattern(/^[a-zA-Z\s]+$/)
        ]
      ],
      minBalance: [
        1000,
        [
          Validators.required,
          Validators.min(1000),
          Validators.max(1000000)
        ]
      ]
    });
  }

  get username() { return this.signupForm.get('username'); }
  get password() { return this.signupForm.get('password'); }
  get holderName() { return this.signupForm.get('holderName'); }
  get minBalance() { return this.signupForm.get('minBalance'); }

  get isCheckingUsername(): boolean {
    return this.username?.pending ?? false;
  }

  // NEW: toggle password visibility
  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  onSubmit(): void {
    this.errorMessage = null;

    Object.keys(this.signupForm.controls).forEach(key => {
      this.signupForm.get(key)?.markAsTouched();
    });

    if (this.signupForm.valid) {
      const request: SignupRequest = this.signupForm.value;

      this.authService.signup(request).subscribe({
        next: () => {
          this.router.navigate(['/login']);
        },
        error: (err) => {
          console.error('Signup failed', err);
          if (typeof err.error === 'string') {
            this.errorMessage = err.error;
          } else if (err.error?.message) {
            this.errorMessage = err.error.message;
          } else {
            this.errorMessage = 'Signup failed. Please try again.';
          }
        }
      });
    } else {
      this.errorMessage = 'Please fill all fields correctly.';
    }
  }
}
