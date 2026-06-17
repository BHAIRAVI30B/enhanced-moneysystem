import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { AccountService } from '../services/account.service';
import { AnalyticsService } from '../services/analytics.service';
import { SessionSocketService } from '../services/sessionsocket.service';
import { Account } from '../models/account.model';
import { SentVsReceived, StatusCount, RewardPoint, OverallStats, TopSender } from '../models/analytics.model';
import { CurrencyPipe, NgIf, NgFor, CommonModule } from '@angular/common';

declare var Chart: any;

@Component({
  selector: 'app-dashboard',
  standalone: true,
  imports: [NgIf, NgFor, CurrencyPipe, CommonModule],
  templateUrl: './dashboard.component.html',
  styleUrls: ['./dashboard.component.css']
})
export class DashboardComponent implements OnInit {
  username: string = '';
  role: string = '';
  account: Account | null = null;

  showAnalytics = true;

  // User analytics
  sentVsReceived: SentVsReceived | null = null;
  userStatusBreakdown: StatusCount[] = [];
  rewardPoints: RewardPoint[] = [];
  totalRewardPoints = 0;
  userAnalyticsError = false;

  // Admin analytics
  overallStats: OverallStats | null = null;
  adminStatusBreakdown: StatusCount[] = [];
  topSenders: TopSender[] = [];
  adminAnalyticsError = false;

  private charts: any[] = [];

  constructor(
    private router: Router,
    private authService: AuthService,
    private accountService: AccountService,
    private analyticsService: AnalyticsService,
    private sessionSocket: SessionSocketService
  ) {}

  ngOnInit(): void {
    const token = this.authService.getToken();
    if (!token) { this.router.navigate(['/login']); return; }

    const payload = JSON.parse(atob(token.split('.')[1]));
    this.username = payload.sub;
    this.role = payload.roles?.[0] ?? '';

    if (this.role === 'ROLE_USER') {
      this.accountService.getMyDetails().subscribe({
        next: (acc) => (this.account = acc),
        error: (err) => console.error('Failed to fetch account details', err)
      });
      this.loadUserAnalytics();
    } else if (this.role === 'ROLE_ADMIN') {
      this.loadAdminAnalytics();
    }
  }

  private loadUserAnalytics(): void {
    this.analyticsService.getSentVsReceived().subscribe({
      next: (data) => { this.sentVsReceived = data; setTimeout(() => this.renderChart1(), 200); },
      error: () => { this.userAnalyticsError = true; }
    });

    this.analyticsService.getUserStatusBreakdown().subscribe({
      next: (data) => { this.userStatusBreakdown = data; setTimeout(() => this.renderChart2(), 200); },
      error: () => { this.userAnalyticsError = true; }
    });

    this.analyticsService.getRewardPoints().subscribe({
      next: (data) => {
        this.rewardPoints = data;
        this.totalRewardPoints = data.reduce((sum, r) => sum + r.points, 0);
        setTimeout(() => this.renderChart3(), 200);
      },
      error: () => { this.userAnalyticsError = true; }
    });
  }

  private loadAdminAnalytics(): void {
    this.analyticsService.getOverallStats().subscribe({
      next: (data) => { this.overallStats = data; },
      error: () => { this.adminAnalyticsError = true; }
    });

    this.analyticsService.getAdminStatusBreakdown().subscribe({
      next: (data) => { this.adminStatusBreakdown = data; setTimeout(() => this.renderAdminChart1(), 200); },
      error: () => { this.adminAnalyticsError = true; }
    });

    this.analyticsService.getTopSenders().subscribe({
      next: (data) => { this.topSenders = data; setTimeout(() => this.renderAdminChart2(), 200); },
      error: () => { this.adminAnalyticsError = true; }
    });
  }

  // ── USER CHARTS ──────────────────────────────────────────────

  private renderChart1(): void {
    const ctx = document.getElementById('sentVsReceivedChart') as HTMLCanvasElement;
    if (!ctx || !this.sentVsReceived) return;
    new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: ['Sent', 'Received'],
        datasets: [{
          data: [this.sentVsReceived.totalSent, this.sentVsReceived.totalReceived],
          backgroundColor: ['#3b82f6', '#22c55e'],
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
              label: (ctx: any) => ` ${ctx.label}: ₹${ctx.parsed.toLocaleString('en-IN', { minimumFractionDigits: 2 })}`
            }
          }
        }
      }
    });
  }

  private renderChart2(): void {
    const ctx = document.getElementById('userStatusChart') as HTMLCanvasElement;
    if (!ctx || !this.userStatusBreakdown.length) return;
    const colorMap: Record<string, string> = {
      SUCCESS: '#22c55e', FAILED: '#ef4444', UNKNOWN: '#94a3b8'
    };
    new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: this.userStatusBreakdown.map(s => s.status),
        datasets: [{
          data: this.userStatusBreakdown.map(s => s.count),
          backgroundColor: this.userStatusBreakdown.map(s => colorMap[s.status] || '#94a3b8'),
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
              label: (ctx: any) => ` ${ctx.label}: ${ctx.parsed} transactions`
            }
          }
        }
      }
    });
  }

  private renderChart3(): void {
    const ctx = document.getElementById('rewardPointsChart') as HTMLCanvasElement;
    if (!ctx || !this.rewardPoints.length) return;
    new Chart(ctx, {
      type: 'bar',
      data: {
        labels: this.rewardPoints.map(r => r.receiver),
        datasets: [{
          label: 'Points Earned',
          data: this.rewardPoints.map(r => r.points),
          backgroundColor: 'rgba(234, 179, 8, 0.7)',
          borderColor: '#ca8a04',
          borderWidth: 1,
          borderRadius: 6
        }]
      },
      options: {
        responsive: true,
        maintainAspectRatio: false,
        plugins: {
          legend: { display: false },
          tooltip: {
            callbacks: {
              label: (ctx: any) => ` ${ctx.parsed.y} pts (₹${this.rewardPoints[ctx.dataIndex].amount.toLocaleString('en-IN')})`
            }
          }
        },
        scales: {
          y: { beginAtZero: true, ticks: { stepSize: 1 }, grid: { color: 'rgba(0,0,0,0.05)' } },
          x: { grid: { display: false } }
        }
      }
    });
  }

  // ── ADMIN CHARTS ─────────────────────────────────────────────

  private renderAdminChart1(): void {
    const ctx = document.getElementById('adminStatusChart') as HTMLCanvasElement;
    if (!ctx || !this.adminStatusBreakdown.length) return;
    // Account status breakdown: ACTIVE=green, LOCKED=amber, CLOSED=red
    const colorMap: Record<string, string> = {
      ACTIVE: '#22c55e', LOCKED: '#f59e0b', CLOSED: '#ef4444', UNKNOWN: '#94a3b8'
    };
    new Chart(ctx, {
      type: 'doughnut',
      data: {
        labels: this.adminStatusBreakdown.map(s => s.status),
        datasets: [{
          data: this.adminStatusBreakdown.map(s => s.count),
          backgroundColor: this.adminStatusBreakdown.map(s => colorMap[s.status] || '#94a3b8'),
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
    new Chart(ctx, {
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
            ticks: { callback: (val: number) => `₹${(val/1000).toFixed(0)}k` }
          },
          y: { grid: { display: false } }
        }
      }
    });
  }

  logout(): void {
    this.sessionSocket.disconnect();
    this.authService.logout();
    this.router.navigate(['/login']);
  }

  goTo(path: string): void {
    this.router.navigate([path]);
  }
}