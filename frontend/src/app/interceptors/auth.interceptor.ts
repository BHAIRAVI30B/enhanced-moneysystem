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

@Injectable()
export class AuthInterceptor implements HttpInterceptor {

  constructor(private router: Router, private authService: AuthService) {}

  intercept(req: HttpRequest<any>, next: HttpHandler) {
    const token = localStorage.getItem('jwt');
    const cloned = token
      ? req.clone({ headers: req.headers.set('Authorization', `Bearer ${token}`) })
      : req;

    return next.handle(cloned).pipe(
      catchError((error: HttpErrorResponse) => {
        if (error.status === 401) {
          // Extract the message from the backend response
          const serverMessage: string =
            error.error?.message ||
            'Your session has expired. Please log in again.';

          // Clear local storage without calling logout API
          // (the session is already dead on the backend side)
          this.authService.clearSessionLocally();

          // Navigate to login and pass the message to display to the user
          this.router.navigate(['/login'], {
            state: { sessionMessage: serverMessage }
          });
        }
        return throwError(() => error);
      })
    );
  }
}

