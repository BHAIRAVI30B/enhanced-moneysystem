import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { finalize } from 'rxjs/operators';
import { Router } from '@angular/router';
import { LoginRequest } from '../models/login-request.model';
import { SignupRequest } from '../models/signup-request.model';
import { JwtResponse } from '../models/jwt-response.model';
import { SignupResponse } from '../models/signup-response.model';

@Injectable({ providedIn: 'root' })
export class AuthService {
  private baseUrl = 'http://localhost:8080/api/auth';

  constructor(private http: HttpClient, private router: Router) {}

  login(request: LoginRequest): Observable<JwtResponse> {
    return this.http.post<JwtResponse>(`${this.baseUrl}/signin`, request);
  }

  signup(request: SignupRequest): Observable<SignupResponse> {
    return this.http.post<SignupResponse>(`${this.baseUrl}/signup`, request);
  }

  checkUsernameAvailability(username: string): Observable<boolean> {
    return this.http.get<boolean>(`${this.baseUrl}/check-username`, {
      params: { username }
    });
  }

  saveToken(token: string): void {
    sessionStorage.setItem('jwt', token);
  }

  getToken(): string | null {
    return sessionStorage.getItem('jwt');
  }

  isLoggedIn(): boolean {
    return !!this.getToken();
  }

  // NEW: calls backend to clear sessionId, then cleans up locally
  logout(): void {
    this.http.post(`${this.baseUrl}/logout`, {}).pipe(
      finalize(() => {
        sessionStorage.removeItem('jwt');
        sessionStorage.removeItem('user');
        this.router.navigate(['/login']);
      })
    ).subscribe({ error: () => {} }); // finalize always runs even on error
  }

  // NEW: used by interceptor when session is killed remotely — skips API call
  clearSessionLocally(): void {
    sessionStorage.removeItem('jwt');
    sessionStorage.removeItem('user');
  }
}
