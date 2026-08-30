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

export interface SettlementDetailQueryParams {
  page: number;
  size: number;
  fromStoreId?: number;
  toStoreId?: number;
  orderNo?: string;
  bizDate?: string;
  billId?: number;
  billNo?: string;
}

export interface SettlementBillQueryParams {
  page: number;
  size: number;
  fromStoreId?: number;
  toStoreId?: number;
  billNo?: string;
  startDate?: string;
  endDate?: string;
  settlementStatus?: string;
}

export interface SettlementBillGeneratePayload {
  settlementPeriodType?: string;
  startDate: string;
  endDate: string;
  remark?: string;
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

export interface AdminSettlementDetailItem {
  id: number;
  orderId?: number | null;
  orderNo?: string;
  fromStoreId?: number | null;
  toStoreId?: number | null;
  principalAmount?: number | null;
  bizDate?: string;
  detailStatus?: string;
}

export interface AdminSettlementBillItem {
  id: number;
  billNo?: string;
  fromStoreId?: number | null;
  toStoreId?: number | null;
  settlementPeriodType?: string;
  startDate?: string;
  endDate?: string;
  totalOrderCount?: number | null;
  totalAmount?: number | null;
  totalRefundAmount?: number | null;
  netAmount?: number | null;
  settlementStatus?: string;
  lockStatus?: string;
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

export interface AdminSettlementDetailPageResult {
  records: AdminSettlementDetailItem[];
  total: number;
  size: number;
  current: number;
}

export interface AdminSettlementBillPageResult {
  records: AdminSettlementBillItem[];
  total: number;
  size: number;
  current: number;
}

export interface AdminSettlementBillGenerateResult {
  generatedCount: number;
  updatedDetailCount: number;
  settlementPeriodType?: string;
  startDate?: string;
  endDate?: string;
}
