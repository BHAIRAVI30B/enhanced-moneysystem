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
          Validators.pattern(/^[a-zA-Z0-9_]+$/) // Only alphanumeric and underscore
        ],
        [UsernameValidator.checkUsername(this.authService)] // Async validator
      ],
      password: [
        '', 
        [
          Validators.required, 
          Validators.minLength(8),
          Validators.maxLength(15),
          Validators.pattern(/^(?=.*[a-z])(?=.*[A-Z])(?=.*\d).*$/) // At least 1 lowercase, 1 uppercase, 1 digit
        ]
      ],
      holderName: [
        '', 
        [
          Validators.required,
          Validators.minLength(4),
          Validators.maxLength(15),
          Validators.pattern(/^[a-zA-Z\s]+$/) // Only letters and spaces
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

  // Helper methods to check validation state
  get username() {
    return this.signupForm.get('username');
  }

  get password() {
    return this.signupForm.get('password');
  }

  get holderName() {
    return this.signupForm.get('holderName');
  }

  get minBalance() {
    return this.signupForm.get('minBalance');
  }

  // Check if username is being validated (async check in progress)
  get isCheckingUsername(): boolean {
    return this.username?.pending ?? false;
  }

  onSubmit(): void {
    this.errorMessage = null;

    // Mark all fields as touched to show validation errors
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