import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AnalyticsService } from '../services/analytics.service';
import { SentVsReceived, StatusCount, DailyFlow, OverallStats, TopSender, CategoryCount } from '../models/analytics.model';
import { CommonModule, CurrencyPipe } from '@angular/common';
import { NgIf, NgFor } from '@angular/common';

declare var Chart: any;

@Component({
  selector: 'app-analytics',
  standalone: true,
  imports: [CommonModule, NgIf, NgFor, CurrencyPipe],
  templateUrl: './analytics.component.html',
  styleUrls: ['./analytics.component.css']
})
export class AnalyticsComponent implements OnInit {
  username: string = '';
  role: string = '';
  isLoading = true;
  analyticsError = false;

  // User analytics
  sentVsReceivedToday: SentVsReceived | null = null;
  userStatusBreakdown: StatusCount[] = [];
  statusRange: 'day' | 'week' | 'month' = 'day';
  statusRangeOptions: Array<'day' | 'week' | 'month'> = ['day', 'week', 'month'];
  weeklyFlow: DailyFlow[] = [];
  userAnalyticsLoading = false;

  // Admin analytics
  overallStats: OverallStats | null = null;
  adminStatusBreakdown: StatusCount[] = [];
  topSenders: TopSender[] = [];
  categoryBreakdown: CategoryCount[] = [];
  adminAnalyticsLoading = false;

  private charts: Record<string, any> = {};

  constructor(
    private router: Router,
    private authService: AuthService,
    private analyticsService: AnalyticsService
  ) {}

  ngOnInit(): void {
    const token = this.authService.getToken();
    if (!token) { this.router.navigate(['/login']); return; }

    const payload = JSON.parse(atob(token.split('.')[1]));
    this.username = payload.sub;
    this.role = payload.roles?.[0] ?? '';

    if (this.role === 'ROLE_USER') {
      this.loadUserAnalytics();
    } else if (this.role === 'ROLE_ADMIN') {
      this.loadAdminAnalytics();
    }
  }

  onStatusRangeChange(range: 'day' | 'week' | 'month'): void {
    this.statusRange = range;
    this.userAnalyticsLoading = true;
    this.analyticsService.getUserStatusBreakdown(range).subscribe({
      next: (data) => { this.userStatusBreakdown = data; this.userAnalyticsLoading = false; setTimeout(() => this.renderChart2(), 50); },
      error: () => { this.analyticsError = true; this.userAnalyticsLoading = false; }
    });
  }

  private loadUserAnalytics(): void {
    this.userAnalyticsLoading = true;
    this.analyticsService.getSentVsReceivedToday().subscribe({
      next: (data) => { this.sentVsReceivedToday = data; setTimeout(() => this.renderChart1(), 200); },
      error: () => { this.analyticsError = true; }
    });

    this.analyticsService.getUserStatusBreakdown(this.statusRange).subscribe({
      next: (data) => { this.userStatusBreakdown = data; setTimeout(() => this.renderChart2(), 200); },
      error: () => { this.analyticsError = true; }
    });

    this.analyticsService.getWeeklyFlow().subscribe({
      next: (data: DailyFlow[]) => { this.weeklyFlow = data; setTimeout(() => this.renderChart3(), 200); },
      error: () => { this.analyticsError = true; }
    });

    setTimeout(() => { this.userAnalyticsLoading = false; this.isLoading = false; }, 300);
  }

  private loadAdminAnalytics(): void {
    this.adminAnalyticsLoading = true;
    this.analyticsService.getOverallStats().subscribe({
      next: (data) => { this.overallStats = data; },
      error: () => { this.analyticsError = true; }
    });

    this.analyticsService.getAdminStatusBreakdown().subscribe({
      next: (data) => { this.adminStatusBreakdown = data; setTimeout(() => this.renderAdminChart1(), 200); },
      error: () => { this.analyticsError = true; }
    });

    this.analyticsService.getTopSenders().subscribe({
      next: (data) => { this.topSenders = data; setTimeout(() => this.renderAdminChart2(), 200); },
      error: () => { this.analyticsError = true; }
    });

    this.analyticsService.getCategoryBreakdown().subscribe({
      next: (data) => { this.categoryBreakdown = data; setTimeout(() => this.renderAdminChart3(), 200); },
      error: () => { this.analyticsError = true; }
    });

    setTimeout(() => { this.adminAnalyticsLoading = false; this.isLoading = false; }, 300);
  }

  // ── USER CHARTS ──────────────────────────────────────────

  private destroyChart(key: string): void {
    if (this.charts[key]) {
      this.charts[key].destroy();
      delete this.charts[key];
    }
  }

  private renderChart1(): void {
    const ctx = document.getElementById('sentVsReceivedChart') as HTMLCanvasElement;
    if (!ctx || !this.sentVsReceivedToday) return;
    this.destroyChart('chart1');
    this.charts['chart1'] = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: ['Today'],
        datasets: [
          {
            label: 'Sent',
            data: [this.sentVsReceivedToday.totalSent],
            backgroundColor: '#3b82f6',
            borderRadius: 8,
            maxBarThickness: 60
          },
          {
            label: 'Received',
            data: [this.sentVsReceivedToday.totalReceived],
            backgroundColor: '#22c55e',
            borderRadius: 8,
            maxBarThickness: 60
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 } } },
          tooltip: {
            callbacks: {
              label: (ctx: any) => ` ${ctx.dataset.label}: ₹${ctx.parsed.y.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
            }
          }
        },
        scales: {
          y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' }, ticks: { callback: (val: number) => `₹${val}` } },
          x: { grid: { display: false } }
        }
      }
    });
  }

  private renderChart2(): void {
    const ctx = document.getElementById('userStatusChart') as HTMLCanvasElement;
    if (!ctx) return;
    this.destroyChart('chart2');
    if (!this.userStatusBreakdown.length) return;

    const colorMap: Record<string, string> = {
      SUCCESS: '#22c55e', FAILED: '#ef4444', UNKNOWN: '#94a3b8'
    };
    this.charts['chart2'] = new Chart(ctx, {
      type: 'pie',
      data: {
        labels: this.userStatusBreakdown.map(s => s.status),
        datasets: [{
          data: this.userStatusBreakdown.map(s => s.count),
          backgroundColor: this.userStatusBreakdown.map(s => colorMap[s.status?.toUpperCase()] || '#94a3b8'),
          borderWidth: 0,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 } } },
          tooltip: {
            callbacks: {
              label: (ctx: any) => ` ${ctx.label}: ${ctx.parsed} transactions`
            }
          }
        }
      }
    });
  }

  private renderChart3(): void {
    const ctx = document.getElementById('weeklyFlowChart') as HTMLCanvasElement;
    if (!ctx || !this.weeklyFlow.length) return;
    this.destroyChart('chart3');
    this.charts['chart3'] = new Chart(ctx, {
      type: 'line',
      data: {
        labels: this.weeklyFlow.map(f => f.date),
        datasets: [
          {
            label: 'Expenditure',
            data: this.weeklyFlow.map(f => f.expenditure),
            borderColor: '#ef4444',
            backgroundColor: 'rgba(239, 68, 68, 0.08)',
            tension: 0.35,
            fill: true,
            pointRadius: 4,
            pointBackgroundColor: '#ef4444'
          },
          {
            label: 'Income',
            data: this.weeklyFlow.map(f => f.income),
            borderColor: '#22c55e',
            backgroundColor: 'rgba(34, 197, 94, 0.08)',
            tension: 0.35,
            fill: true,
            pointRadius: 4,
            pointBackgroundColor: '#22c55e'
          }
        ]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 } } },
          tooltip: {
            callbacks: {
              label: (ctx: any) => ` ${ctx.dataset.label}: ₹${ctx.parsed.y.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
            }
          }
        },
        scales: {
          y: { beginAtZero: true, grid: { color: 'rgba(0,0,0,0.05)' }, ticks: { callback: (val: number) => `₹${val}` } },
          x: { grid: { display: false } }
        }
      }
    });
  }

  // ── ADMIN CHARTS ─────────────────────────────────────────────

  private renderAdminChart1(): void {
    const ctx = document.getElementById('adminStatusChart') as HTMLCanvasElement;
    if (!ctx || !this.adminStatusBreakdown.length) return;
    this.destroyChart('admin1');
    const colorMap: Record<string, string> = {
      ACTIVE: '#22c55e', LOCKED: '#f59e0b', CLOSED: '#ef4444', UNKNOWN: '#94a3b8'
    };
    this.charts['admin1'] = new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: this.adminStatusBreakdown.map(s => s.status),
        datasets: [{
          data: this.adminStatusBreakdown.map(s => s.count),
          backgroundColor: this.adminStatusBreakdown.map(s => colorMap[s.status?.toUpperCase()] || '#94a3b8'),
          borderWidth: 0,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        cutout: '65%',
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 } } },
          tooltip: {
            callbacks: {
              label: (ctx: any) => ` ${ctx.label}: ${ctx.parsed} accounts`
            }
          }
        }
      }
    });
  }

  private renderAdminChart2(): void {
    const ctx = document.getElementById('topSendersChart') as HTMLCanvasElement;
    if (!ctx || !this.topSenders.length) return;
    this.destroyChart('admin2');
    this.charts['admin2'] = new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.topSenders.map(s => s.holderName),
        datasets: [{
          label: 'Total Sent (₹)',
          data: this.topSenders.map(s => s.totalSent),
          backgroundColor: this.topSenders.map((_, i) =>
            `rgba(59, 130, 246, ${1 - i * 0.15})`
          ),
          borderColor: '#1e40af',
          borderWidth: 1,
          borderRadius: 6
        }]
      },
      options: {
        indexAxis: 'y',
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx: any) => ` ₹${ctx.parsed.x.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
            }
          }
        },
        scales: {
          x: {
            beginAtZero: true,
            grid: { color: 'rgba(0,0,0,0.05)' },
            ticks: {
              callback: (val: number) => {
                if (val >= 1000) return `₹${(val/1000).toFixed(0)}k`;
                else if (val > 0) return `₹${val.toFixed(0)}`;
                else return '₹0';
              }
            }
          },
          y: { grid: { display: false } }
        }
      }
    });
  }

  private renderAdminChart3(): void {
    const ctx = document.getElementById('categoryBreakdownChart') as HTMLCanvasElement;
    if (!ctx || !this.categoryBreakdown.length) return;
    this.destroyChart('admin3');
    const palette = ['#3b82f6', '#22c55e', '#f59e0b', '#ef4444', '#8b5cf6', '#ec4899', '#14b8a6', '#64748b'];
    this.charts['admin3'] = new Chart(ctx, {
      type: 'pie',
      data: {
        labels: this.categoryBreakdown.map(c => c.category),
        datasets: [{
          data: this.categoryBreakdown.map(c => c.totalAmount),
          backgroundColor: this.categoryBreakdown.map((_, i) => palette[i % palette.length]),
          borderWidth: 0,
          hoverOffset: 8
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { position: 'bottom', labels: { padding: 16, font: { size: 12 } } },
          tooltip: {
            callbacks: {
              label: (ctx: any) => ` ${ctx.label}: ₹${ctx.parsed.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
            }
          }
        }
      }
    });
  }

  back(): void {
    this.router.navigate(['/dashboard']);
  }
}
