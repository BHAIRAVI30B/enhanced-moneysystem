export interface RewardEntry {
  toAccountHolderName: string;
  toAccountId: string;
  amount: number;
  points: number;
  createdOn: string;
}

export interface RewardResponse {
  totalPoints: number;
  entries: RewardEntry[];
}
