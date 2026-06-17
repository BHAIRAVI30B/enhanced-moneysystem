export interface SentVsReceived {
  totalSent: number;
  totalReceived: number;
}

export interface StatusCount {
  status: string;
  count: number;
}

export interface RewardPoint {
  receiver: string;
  amount: number;
  points: number;
  date: string;
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