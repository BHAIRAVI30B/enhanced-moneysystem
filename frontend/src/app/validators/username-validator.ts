import { AbstractControl, AsyncValidatorFn, ValidationErrors } from '@angular/forms';
import { Observable, of } from 'rxjs';
import { map, catchError, debounceTime, switchMap, first } from 'rxjs/operators';
import { AuthService } from '../services/auth.service';

export class UsernameValidator {
  static checkUsername(authService: AuthService): AsyncValidatorFn {
    return (control: AbstractControl): Observable<ValidationErrors | null> => {
      if (!control.value) {
        return of(null);
      }

      // Debounce to avoid too many API calls
      return of(control.value).pipe(
        debounceTime(500), // Wait 500ms after user stops typing
        switchMap(username => 
          authService.checkUsernameAvailability(username).pipe(
            map(isAvailable => {
              // If username is NOT available, return error
              return isAvailable ? null : { usernameTaken: true };
            }),
            catchError(() => of(null)) // On error, don't block the form
          )
        ),
        first() // Complete after first emission
      );
    };
  }
}