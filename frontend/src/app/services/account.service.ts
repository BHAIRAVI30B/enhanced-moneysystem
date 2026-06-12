import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Account } from '../models/account.model';
import { TransactionResponse } from '../models/transaction-response.model';
import { RewardResponse } from '../models/reward.model';

@Injectable({ providedIn: 'root' })
export class AccountService {
  private baseUrl = 'http://localhost:8080/api/v1/accounts';
  private transferUrl = 'http://localhost:8080/api/v1/transfers';

  constructor(private http: HttpClient) {}

  private getAuthHeaders(): HttpHeaders {
    const token = sessionStorage.getItem('jwt');
    return new HttpHeaders({ Authorization: `Bearer ${token}` });
  }

  getMyDetails(): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/my-details`, { headers: this.getAuthHeaders() });
  }

  getMyBalance(): Observable<number> {
    return this.http.get<number>(`${this.baseUrl}/balance`, { headers: this.getAuthHeaders() });
  }

  getMyTransactions(): Observable<TransactionResponse[]> {
    return this.http.get<TransactionResponse[]>(`${this.baseUrl}/my-transactions`, { headers: this.getAuthHeaders() });
  }

  getMyRewards(): Observable<RewardResponse> {
    return this.http.get<RewardResponse>(`${this.baseUrl}/my-rewards`, { headers: this.getAuthHeaders() });
  }

  transferAsUser(request: any): Observable<TransactionResponse> {
    return this.http.post<TransactionResponse>(`${this.transferUrl}/user`, request, { headers: this.getAuthHeaders() });
  }

  getAccountById(id: string): Observable<Account> {
    return this.http.get<Account>(`${this.baseUrl}/${id}`, { headers: this.getAuthHeaders() });
  }

  getTransactionsById(id: string): Observable<TransactionResponse[]> {
    return this.http.get<TransactionResponse[]>(`${this.baseUrl}/${id}/transactions`, { headers: this.getAuthHeaders() });
  }

  updateAccountStatus(id: string, status: string): Observable<Account> {
    return this.http.put<Account>(`${this.baseUrl}/${id}/status`, { status }, { headers: this.getAuthHeaders() });
  }
}