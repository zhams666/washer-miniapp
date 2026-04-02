export interface AdminUserItem {
  id: number;
  userNo?: string;
  nickname?: string;
  realName?: string;
  mobile?: string;
  userStatus?: number | null;
  registerSource?: string;
  isMember?: number | null;
  memberLevel?: string;
  lastConsumeTime?: string;
  createdAt?: string;
}

export interface AdminUserPageResult {
  records: AdminUserItem[];
  total: number;
  size: number;
  current: number;
}

export interface AdminUserWalletAsset {
  id: number;
  storeId?: number | null;
  storeName?: string;
  principalBalance?: number | null;
  availablePrincipalBalance?: number | null;
  giftBalance?: number | null;
  availableGiftBalance?: number | null;
  status?: number | null;
  updatedAt?: string;
}

export interface AdminUserCardAsset {
  id: number;
  storeId?: number | null;
  storeName?: string;
  cardNo?: string;
  cardType?: string;
  sourceChannel?: string;
  totalTimes?: number | null;
  usedTimes?: number | null;
  remainingTimes?: number | null;
  status?: string;
  effectiveTime?: string;
  expireTime?: string;
  updatedAt?: string;
}

export interface AdminUserRecentOrder {
  id: number;
  orderNo?: string;
  storeId?: number | null;
  storeName?: string;
  deviceId?: number | null;
  deviceName?: string;
  payMode?: string;
  paymentStatus?: string;
  orderStatus?: string;
  finalAmount?: number | null;
  paidAmount?: number | null;
  createdAt?: string;
}

export interface AdminUserWalletTransactionItem {
  id: number;
  transactionNo?: string;
  storeId?: number | null;
  storeName?: string;
  bizType?: string;
  amountType?: string;
  changeType?: string;
  amount?: number | null;
  balanceAfter?: number | null;
  relatedOrderNo?: string;
  createdAt?: string;
}

export interface AdminUserCardUsageItem {
  id: number;
  usageNo?: string;
  userCardId?: number | null;
  storeId?: number | null;
  storeName?: string;
  orderId?: number | null;
  orderNo?: string;
  usedTimes?: number | null;
  usageTime?: string;
  createdAt?: string;
}

export interface AdminUserOverview {
  id: number;
  userNo?: string;
  nickname?: string;
  realName?: string;
  mobile?: string;
  userStatus?: number | null;
  registerSource?: string;
  isMember?: number | null;
  memberLevel?: string;
  memberSinceTime?: string;
  lastLoginTime?: string;
  lastConsumeTime?: string;
  remark?: string;
  createdAt?: string;
  todayFirstPeriodDiscountUsed?: number | null;
  todayFirstPeriodDiscountDate?: string;
  todayFirstPeriodDiscountStoreId?: number | null;
  todayFirstPeriodDiscountStoreName?: string;
  todayFirstPeriodDiscountOrderId?: number | null;
  todayFirstPeriodDiscountOrderNo?: string;
  todayFirstPeriodDiscountAmount?: number | null;
  wallets: AdminUserWalletAsset[];
  cards: AdminUserCardAsset[];
  recentOrders: AdminUserRecentOrder[];
  recentWalletTransactions: AdminUserWalletTransactionItem[];
  recentCardUsages: AdminUserCardUsageItem[];
}
