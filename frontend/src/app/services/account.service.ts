import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Account } from '../models/account.model';
import { TransactionResponse } from '../models/transaction-response.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private baseUrl = 'http://localhost:8080/api/v1/accounts';
  private transferUrl = 'http://localhost:8080/api/v1/transfers';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = localStorage.getItem('jwt');
    return new HttpHeaders({
      Authorization: `Bearer ${token}`
    });
  }

  // USER endpoints
  getMyDetails(): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/my-details`, { headers: this.getAuthHeaders() });
  }

  getMyBalance(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/balance`, { headers: this.getAuthHeaders() });
  }

  getMyTransactions(): Observable<TransactionResponse[]> {
    return this.http.get<TransactionResponse[]>(`${this.baseUrl}/my-transactions`, { headers: this.getAuthHeaders() });
  }

  transferAsUser(request: any): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.transferUrl}/user`, request, { headers: this.getAuthHeaders() });
  }

  // ADMIN endpoints
  getAccountById(id: string): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/${id}`, { headers: this.getAuthHeaders() });
  }

  getTransactionsById(id: string): Observable<TransactionResponse[]> {
    return this.http.get<TransactionResponse[]>(`${this.baseUrl}/${id}/transactions`, { headers: this.getAuthHeaders() });
  }
}
