import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { TransferRequest } from '../models/transfer-request.model';
import { TransactionResponse } from '../models/transaction-response.model';

@Injectable({ providedIn: 'root' })
export class TransferService {
  private transferUrl = 'http://localhost:8080/api/v1/transfers';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwt');
    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }

  // USER endpoint
  transferAsUser(request: TransferRequest): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(
      `${this.transferUrl}/user`,
      request,
      { headers: this.getAuthHeaders() }
    );
  }
 
}
