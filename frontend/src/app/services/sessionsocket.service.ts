import { Injectable } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

@Injectable({ providedIn: 'root' })
export class SessionSocketService {

  private socket: WebSocket | null = null;
  private readonly wsBaseUrl = 'ws://localhost:8080/ws/session';
  private isConnected = false; // tracks if connection was ever successfully opened

  constructor(private router: Router, private authService: AuthService) {}

  connect(): void {
    if (this.socket && this.socket.readyState === WebSocket.OPEN) return;

    const token = this.authService.getToken();
    if (!token) return;

    this.socket = new WebSocket(`${this.wsBaseUrl}?token=${token}`);

    this.socket.onopen = () => {
      this.isConnected = true;
      console.log('Session WebSocket connected');
    };

    this.socket.onmessage = (event) => {
      if (event.data === 'SESSION_KICKED') {
        this.isConnected = false;
        this.disconnect();
        this.authService.clearSessionLocally();
        this.router.navigate(['/login'], {
          state: {
            sessionMessage: 'Your session was closed because a new session was opened on another device or tab. Please log in again.'
          }
        });
      }
    };

    this.socket.onerror = (error) => {
      console.error('Session WebSocket error:', error);
    };

    this.socket.onclose = (event) => {
      console.log('Session WebSocket closed:', event.code, event.reason);

      // Only redirect on unexpected close if:
      // 1. Connection was successfully opened before (isConnected = true)
      // 2. User is still logged in
      // 3. It wasn't an intentional disconnect (we null onclose before calling close())
      if (this.isConnected && this.authService.isLoggedIn()) {
        this.isConnected = false;
        this.authService.clearSessionLocally();
        this.router.navigate(['/login'], {
          state: {
            sessionMessage: 'Your session has expired. Please log in again.'
          }
        });
      }

      this.isConnected = false;
    };
  }

  disconnect(): void {
    if (this.socket) {
      this.isConnected = false;
      this.socket.onclose = null; // prevent onclose from firing redirect
      this.socket.close();
      this.socket = null;
    }
  }
}