import { Injectable } from '@angular/core';
import {
  HttpInterceptor,
  HttpRequest,
  HttpHandler,
  HttpErrorResponse
} from '@angular/common/http';
import { catchError, throwError } from 'rxjs';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { SessionSocketService } from '../services/sessionsocket.service';

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(
    private router: Router,
    private authService: AuthService,
    private sessionSocket: SessionSocketService
  ) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = sessionStorage.getItem('jwt');
    const cloned = token
      ? req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) })
      : req;

    return next.handle(cloned).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // 401 here means the JWT has expired (token expiry)
          // Session kick is handled by WebSocket — not here
          this.sessionSocket.disconnect();
          this.authService.clearSessionLocally();
          this.router.navigate(['/login'], {
            state: {
              sessionMessage: 'Your session has expired. Please log in again.'
            }
          });
        }
        return throwError(() => error);
      })
    );
  }
}
