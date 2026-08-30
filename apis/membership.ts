import { GET, POST } from '../utils/request';
import type { IObject } from 'typings/interface.d';

export type MembershipSetting = {
  memberDayEnabled?: number;
  memberDayWeekday?: number;
  memberDayStartTime?: string;
  memberDayEndTime?: string;
  memberDayFirstMinutes?: number;
  memberDayDiscountRate?: number | string;
  benefitText?: string;
};

export type MembershipPlan = {
  id: number;
  planCode?: string;
  planName?: string;
  planType?: string;
  durationMonths?: number;
  price?: number | string;
  benefitText?: string;
  status?: number;
};

export type MembershipOverview = {
  settings?: MembershipSetting;
  plans?: MembershipPlan[];
  user?: {
    userId?: number;
    isMember?: number;
    memberLevel?: string;
    memberSinceTime?: string;
    memberExpireTime?: string;
    points?: number;
  } | null;
};

export type MembershipOrderResult = {
  orderNo?: string;
  payAmount?: number | string;
  payStatus?: string;
  memberStartTime?: string;
  memberExpireTime?: string;
  payParams?: {
    timeStamp?: string;
    nonceStr?: string;
    package?: string;
    packageValue?: string;
    signType?: string;
    paySign?: string;
  } | null;
};

export const getMembershipOverview = async (userId?: number): Promise<MembershipOverview> => {
  const { code, data } = await GET<MembershipOverview>('/membership/overview', userId ? { userId } : {});
  return code === 0 && data && typeof data === 'object' ? data : {};
};

export const createMembershipOrder = async (payload: IObject): Promise<MembershipOrderResult> => {
  const { code, data } = await POST<MembershipOrderResult>('/membership/orders', payload);
  return code === 0 && data && typeof data === 'object' ? data : {};
};

export const syncMembershipOrder = async (orderNo: string): Promise<MembershipOrderResult> => {
  const { code, data } = await POST<MembershipOrderResult>(
    `/membership/orders/${encodeURIComponent(orderNo)}/sync`,
    {}
  );
  return code === 0 && data && typeof data === 'object' ? data : {};
};
