export interface TransactionResponse {
  fromAccountId: string;
  fromAccountHolderName: string;
  toAccountId: string;
  toAccountHolderName: string;
  amount: number;
  status: string;
  failureReason?: string | null;
  category?: string | null;
  note?: string | null;
  createdOn: string;
  pointsRedeemed?: number;
  discountAmount?: number;
  amountPaid?: number;
}