import { AbstractControl, ValidationErrors, ValidatorFn } from '@angular/forms';

export class DecimalPlacesValidator {
  // Rejects values with more than `maxDecimals` digits after the decimal point.
  // Whole numbers (1000) and up to `maxDecimals` decimals (1000.25) are valid.
  static maxDecimalPlaces(maxDecimals: number = 2): ValidatorFn {
    return (control: AbstractControl): ValidationErrors | null => {
      const value = control.value;

      if (value === null || value === undefined || value === '') {
        return null; // let Validators.required handle emptiness
      }

      // Use the raw string form so trailing typed digits (e.g. "1000.255")
      // are checked exactly as entered, not after any numeric rounding.
      const stringValue = String(value);
      const decimalPart = stringValue.split('.')[1];

      if (decimalPart && decimalPart.length > maxDecimals) {
        return { decimalPlaces: { maxDecimals, actual: decimalPart.length } };
      }

      return null;
    };
  }
}