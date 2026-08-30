export interface MembershipSetting {
  id?: number;
  settingKey?: string;
  memberDayEnabled?: number;
  memberDayWeekday?: number;
  memberDayStartTime?: string;
  memberDayEndTime?: string;
  memberDayFirstMinutes?: number;
  memberDayDiscountRate?: number | string;
  benefitText?: string;
}

export interface MembershipPlan {
  id?: number;
  planCode?: string;
  planName?: string;
  planType?: string;
  durationMonths?: number;
  price?: number | string;
  benefitText?: string;
  status?: number;
  sortOrder?: number;
}

export interface MembershipOrder {
  id?: number;
  orderNo?: string;
  userId?: number;
  planId?: number;
  payAmount?: number | string;
  payChannel?: string;
  payStatus?: string;
  paymentNo?: string;
  payTime?: string;
  memberStartTime?: string;
  memberExpireTime?: string;
  createdAt?: string;
}

export interface MembershipOrderPage {
  records: MembershipOrder[];
  total: number;
  current?: number;
  size?: number;
}
