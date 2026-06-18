import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import {
  SentVsReceived,
  StatusCount,
  DailyFlow,
  OverallStats,
  TopSender,
  CategoryCount
} from '../models/analytics.model';

@Injectable({ providedIn: 'root' })
export class AnalyticsService {
  private baseUrl = 'http://localhost:8080/api/v1/analytics';

  constructor(private http: HttpClient) {}

  // User analytics
  getSentVsReceivedToday(): Observable<SentVsReceived> {
    return this.http.get<SentVsReceived>(`${this.baseUrl}/user/sent-vs-received-today`);
  }

  getUserStatusBreakdown(range: 'day' | 'week' | 'month' = 'day'): Observable<StatusCount[]> {
    return this.http.get<StatusCount[]>(`${this.baseUrl}/user/status-breakdown`, {
      params: { range }
    });
  }

  getWeeklyFlow(): Observable<DailyFlow[]> {
    return this.http.get<DailyFlow[]>(`${this.baseUrl}/user/weekly-flow`);
  }

  // Admin analytics
  getOverallStats(): Observable<OverallStats> {
    return this.http.get<OverallStats>(`${this.baseUrl}/admin/overall-stats`);
  }

  getAdminStatusBreakdown(): Observable<StatusCount[]> {
    return this.http.get<StatusCount[]>(`${this.baseUrl}/admin/status-breakdown`);
  }

  getTopSenders(): Observable<TopSender[]> {
    return this.http.get<TopSender[]>(`${this.baseUrl}/admin/top-senders`);
  }

  getCategoryBreakdown(): Observable<CategoryCount[]> {
    return this.http.get<CategoryCount[]>(`${this.baseUrl}/admin/category-breakdown`);
  }
}