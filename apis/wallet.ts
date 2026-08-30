import { GET, POST } from '../utils/request';
import type { IObject } from 'typings/interface.d';

export type RechargeOrderResult = {
  rechargeOrderNo: string;
  paymentNo?: string;
  walletId?: number;
  userId?: number;
  storeId?: number;
  rechargeProductId?: number;
  payAmount?: number | string;
  principalAmount?: number | string;
  giftAmount?: number | string;
  payStatus?: string;
  payParams?: {
    timeStamp: string;
    nonceStr: string;
    package: string;
    packageValue?: string;
    signType: 'RSA' | 'MD5' | string;
    paySign: string;
  };
  expireTime?: string;
  failReason?: string;
};

export const getRechargeProducts = async (_storeId: number): Promise<IObject[]> => {
  const { code, data } = await GET('/wallet/recharge-products', {
    storeId: _storeId,
  });
  if (code == 0 && Array.isArray(data)) {
    return data;
  }
  return [];
};

export const createRechargeOrder = async (
  _data: IObject
): Promise<RechargeOrderResult> => {
  const { code, data } = await POST('/pay/recharge', _data);
  if (code == 0 && data && typeof data === 'object') {
    return data as RechargeOrderResult;
  }
  return {} as RechargeOrderResult;
};

export const getRechargeOrderResult = async (
  _rechargeOrderNo: string
): Promise<RechargeOrderResult> => {
  const { code, data } = await GET(`/wallet/recharges/${encodeURIComponent(_rechargeOrderNo)}`);
  if (code == 0 && data && typeof data === 'object') {
    return data as RechargeOrderResult;
  }
  return {} as RechargeOrderResult;
};

export const syncRechargeOrder = async (
  _rechargeOrderNo: string
): Promise<RechargeOrderResult> => {
  const { code, data } = await POST(
    `/wallet/recharges/${encodeURIComponent(_rechargeOrderNo)}/sync`,
    {}
  );
  if (code == 0 && data && typeof data === 'object') {
    return data as RechargeOrderResult;
  }
  return {} as RechargeOrderResult;
};

export const payHandle = async (_data: IObject): Promise<IObject> => {
  const { code, data } = await POST('/pay/payCallBack', _data);
  if (code == 0) {
    return data;
  }
  return {};
};

export const getWalletHistory = async (
  _costomerId: string,
  _limit: number,
  _page: number,
  _bizType?: string
): Promise<IObject> => {
  const { code, data } = await GET('/costomerflow/page', {
    costomerId: _costomerId,
    limit: _limit,
    page: _page,
    bizType: _bizType,
  });
  if (code == 0) {
    return data;
  }
  return {};
};

export const getWalletSummary = async (_userId: number): Promise<IObject> => {
  const { code, data } = await GET('/wallet/summary', {
    userId: _userId,
  });
  if (code == 0) {
    return data;
  }
  return {};
};

export const getWalletStoreBalances = async (_userId: number): Promise<IObject> => {
  const { code, data } = await GET('/wallet/store-balances', {
    userId: _userId,
  });
  if (code == 0) {
    return data;
  }
  return {};
};
