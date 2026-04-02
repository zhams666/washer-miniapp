export interface StoreOption {
  id: number;
  storeName: string;
}

export interface OrderQueryParams {
  page: number;
  size: number;
  storeId?: number;
  orderStatus?: string;
  paymentStatus?: string;
  keyword?: string;
}

export interface AdminOrderItem {
  id: number;
  orderNo: string;
  userId: number;
  storeId: number;
  storeName: string;
  deviceId?: number | null;
  deviceName?: string;
  payMode: string;
  paymentStatus: string;
  orderStatus: string;
  finalAmount?: number | null;
  paidAmount?: number | null;
  remark?: string;
  createdAt?: string;
  startTime?: string;
  endTime?: string;
}

export interface AdminOrderPageResult {
  records: AdminOrderItem[];
  total: number;
  size: number;
  current: number;
}

export interface OrderStatusLog {
  id: number;
  fromStatus?: string;
  toStatus?: string;
  actionType?: string;
  operatorType?: string;
  operatorId?: number;
  remark?: string;
  createdAt?: string;
}

export interface OrderPaymentDetail {
  id: number;
  sourceType?: string;
  amountType?: string;
  userCardId?: number | null;
  amount?: number | null;
  deductTimes?: number | null;
  paymentSeq?: number | null;
  settleStage?: string;
  allocationStrategy?: string;
  bizActionNo?: string;
  refundedAmount?: number | null;
  createdAt?: string;
}

export interface AdminOrderDetail {
  id: number;
  orderNo: string;
  userId: number;
  storeId: number;
  storeName: string;
  deviceId?: number | null;
  deviceCode?: string;
  deviceName?: string;
  deviceStatus?: string;
  orderSource?: string;
  payMode?: string;
  paymentStatus?: string;
  paymentStatusDesc?: string;
  orderStatus?: string;
  estimatedAmount?: number | null;
  finalAmount?: number | null;
  paidAmount?: number | null;
  refundAmount?: number | null;
  isFirstPeriodDiscountUsed?: number | null;
  firstPeriodDiscountAmount?: number | null;
  firstPeriodDiscountDesc?: string;
  cardUsageId?: number | null;
  cardDeductTimes?: number | null;
  remark?: string;
  abnormalReason?: string;
  createdAt?: string;
  startTime?: string;
  endTime?: string;
  settleTime?: string;
  statusLogs: OrderStatusLog[];
  paymentDetails: OrderPaymentDetail[];
}
