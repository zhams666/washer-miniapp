export interface AdminDashboardMetric {
  key: 'cardUsage' | 'walletConsume' | 'walletRecharge' | 'cardPurchase' | string;
  title: string;
  amount?: number | null;
  count?: number | null;
  times?: number | null;
  secondaryAmount?: number | null;
}

export interface AdminDashboardHourlyPoint {
  hour: number;
  label: string;
  cardUsageTimes?: number | null;
  walletConsumeAmount?: number | null;
  walletRechargeAmount?: number | null;
  cardPurchaseAmount?: number | null;
}

export interface AdminDashboardStoreMetric {
  storeId?: number | null;
  storeName?: string;
  cardUsageTimes?: number | null;
  walletConsumeAmount?: number | null;
  walletRechargeAmount?: number | null;
  cardPurchaseAmount?: number | null;
}

export interface AdminDashboardChannelMetric {
  channel?: string;
  orderCount?: number | null;
  cardCount?: number | null;
  payAmount?: number | null;
}

export interface AdminDashboardDeviceAlertItem {
  deviceId?: number | null;
  deviceCode?: string;
  deviceName?: string;
  storeId?: number | null;
  storeName?: string;
  status?: string;
  lastHeartbeatTime?: string;
  lastOnlineTime?: string;
  updatedAt?: string;
  remark?: string;
}

export interface AdminDashboardDeviceStatus {
  totalCount?: number | null;
  normalCount?: number | null;
  runningCount?: number | null;
  idleCount?: number | null;
  faultCount?: number | null;
  offlineCount?: number | null;
  disabledCount?: number | null;
  pausedCount?: number | null;
  abnormalCount?: number | null;
  alerts?: AdminDashboardDeviceAlertItem[];
}

export interface AdminDashboardActivityItem {
  type: 'cardUsage' | 'walletConsume' | 'walletRecharge' | 'cardPurchase' | string;
  title?: string;
  storeName?: string;
  referenceNo?: string;
  amount?: number | null;
  times?: number | null;
  occurredAt?: string;
}

export interface AdminDashboardKpiItem {
  key: string;
  title: string;
  amount?: number | null;
  count?: number | null;
  unit?: string | null;
  description?: string | null;
}

export interface AdminDashboardDailyTrendPoint {
  date: string;
  label: string;
  rechargeAmount?: number | null;
  consumeAmount?: number | null;
  washCount?: number | null;
  cardUsageTimes?: number | null;
  groupVerifyCount?: number | null;
}

export interface AdminDashboardTodayOverview {
  bizDate: string;
  startDate?: string;
  endDate?: string;
  generatedAt?: string;
  topMetrics?: AdminDashboardKpiItem[];
  rangeMetrics?: AdminDashboardKpiItem[];
  dailyTrend?: AdminDashboardDailyTrendPoint[];
  summaryCards: AdminDashboardMetric[];
  hourlyTrend: AdminDashboardHourlyPoint[];
  storeMetrics: AdminDashboardStoreMetric[];
  cardPurchaseChannels: AdminDashboardChannelMetric[];
  deviceStatus?: AdminDashboardDeviceStatus;
  recentActivities: AdminDashboardActivityItem[];
}
