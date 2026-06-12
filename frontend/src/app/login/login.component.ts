import { Component, OnInit } from '@angular/core';
import { FormBuilder, FormGroup, Validators, ReactiveFormsModule } from '@angular/forms';
import { CommonModule } from '@angular/common';
import { Router, RouterOutlet, RouterLink } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { SessionSocketService } from '../services/sessionsocket.service';
import { LoginRequest } from '../models/login-request.model';
import { JwtResponse } from '../models/jwt-response.model';

@Component({
  selector: 'app-login',
  standalone: true,
  imports: [ReactiveFormsModule, CommonModule, RouterLink, RouterOutlet],
  templateUrl: './login.component.html',
  styleUrls: ['./login.component.css']
})
export class LoginComponent implements OnInit {
  loginForm: FormGroup;
  errorMessage: string | null = null;
  showPassword: boolean = false;
  showClosedAccountModal: boolean = false;

  constructor(
    private fb: FormBuilder,
    private authService: AuthService,
    private router: Router,
    private sessionSocket: SessionSocketService
  ) {
    this.loginForm = this.fb.group({
      username: [
        '',
        [
          Validators.required,
          Validators.minLength(3),
          Validators.maxLength(15),
          Validators.pattern(/^[a-zA-Z0-9_]+$/)
        ]
      ],
      password: [
        '',
        [
          Validators.required,
          Validators.minLength(8),
          Validators.maxLength(15)
        ]
      ]
    });
  }

  ngOnInit(): void {
    // Disconnect any lingering socket when arriving at login page
    this.sessionSocket.disconnect();

    // Read state from history.state — works even after navigation is complete
    const state = history.state as { sessionMessage?: string };
    if (state?.sessionMessage) {
      this.errorMessage = state.sessionMessage;
    }
  }

  togglePasswordVisibility(): void {
    this.showPassword = !this.showPassword;
  }

  closeClosedAccountModal(): void {
    this.showClosedAccountModal = false;
  }

  get username() { return this.loginForm.get('username'); }
  get password() { return this.loginForm.get('password'); }

  onSubmit(): void {
    this.errorMessage = null;

    if (this.loginForm.valid) {
      const request: LoginRequest = this.loginForm.value;

      this.authService.login(request).subscribe({
        next: (response: JwtResponse) => {
          this.authService.saveToken(response.accessToken);
          sessionStorage.setItem('user', JSON.stringify(response));
          this.sessionSocket.connect();
          this.router.navigate(['/dashboard']);
        },
        error: (err) => {
          console.error('Login failed', err);
          let message: string;
          if (typeof err.error === 'string') {
            message = err.error;
          } else if (err.error?.message) {
            message = err.error.message;
          } else {
            message = 'Login failed. Please try again.';
          }

          if (err.status === 403 && message.toLowerCase().includes('closed')) {
            this.showClosedAccountModal = true;
            this.errorMessage = null;
          } else {
            this.errorMessage = message;
          }
        }
      });
    } else {
      this.errorMessage = 'Please enter both username and password.';
    }
  }
}