export interface SentVsReceived {
  totalSent: number;
  totalReceived: number;
}

export interface StatusCount {
  status: string;
  count: number;
}

export interface DailyFlow {
  date: string;
  expenditure: number;
  income: number;
}

export interface OverallStats {
  totalVolume: number;
  totalTransactions: number;
  successful: number;
  failed: number;
}

export interface TopSender {
  holderName: string;
  accountId: string;
  totalSent: number;
  transactionCount: number;
}

export interface CategoryCount {
  category: string;
  count: number;
  totalAmount: number;
}