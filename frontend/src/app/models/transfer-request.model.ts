export interface TransferRequest {
  toAccountId: string;
  amount: number;
  category?: string;
  note?: string;
  idempotencyKey: string;
}
