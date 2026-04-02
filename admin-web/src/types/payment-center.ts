export interface PaymentDetailQueryParams {
  page: number;
  size: number;
  orderNo?: string;
  userId?: number;
  storeId?: number;
  payMode?: string;
  paymentStatus?: string;
}

export interface WalletTransactionQueryParams {
  page: number;
  size: number;
  userId?: number;
  storeId?: number;
  bizType?: string;
  relatedOrderNo?: string;
}

export interface CardUsageQueryParams {
  page: number;
  size: number;
  userId?: number;
  storeId?: number;
  cardNo?: string;
  orderNo?: string;
}

export interface AdminPaymentDetailItem {
  id: number;
  orderId?: number | null;
  orderNo?: string;
  userId?: number | null;
  storeId?: number | null;
  storeName?: string;
  payMode?: string;
  paymentStatus?: string;
  amountType?: string;
  userCardId?: number | null;
  cardNo?: string;
  amount?: number | null;
  deductTimes?: number | null;
  paymentSeq?: number | null;
  settleStage?: string;
  allocationStrategy?: string;
  refundedAmount?: number | null;
  createdAt?: string;
}

export interface AdminWalletTransactionCenterItem {
  id: number;
  transactionNo?: string;
  userId?: number | null;
  storeId?: number | null;
  storeName?: string;
  bizType?: string;
  amountType?: string;
  changeType?: string;
  amount?: number | null;
  balanceAfter?: number | null;
  relatedOrderId?: number | null;
  relatedOrderNo?: string;
  remark?: string;
  createdAt?: string;
}

export interface AdminCardUsageCenterItem {
  id: number;
  usageNo?: string;
  userCardId?: number | null;
  cardNo?: string;
  userId?: number | null;
  storeId?: number | null;
  storeName?: string;
  orderId?: number | null;
  orderNo?: string;
  usedTimes?: number | null;
  usageTime?: string;
  remark?: string;
  createdAt?: string;
}

export interface AdminPaymentDetailPageResult {
  records: AdminPaymentDetailItem[];
  total: number;
  size: number;
  current: number;
}

export interface AdminWalletTransactionPageResult {
  records: AdminWalletTransactionCenterItem[];
  total: number;
  size: number;
  current: number;
}

export interface AdminCardUsagePageResult {
  records: AdminCardUsageCenterItem[];
  total: number;
  size: number;
  current: number;
}
