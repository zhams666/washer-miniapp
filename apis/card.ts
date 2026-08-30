import { GET, POST } from '../utils/request';
import type { IObject } from 'typings/interface.d';

export const getCardSummary = async (_userId: number): Promise<IObject> => {
  const { code, data } = await GET('/api/cards/summary', {
    userId: _userId,
  });
  if (code == 0) {
    return data;
  }
  return {};
};

export const getMyCards = async (_userId: number): Promise<IObject[]> => {
  const { code, data } = await GET('/api/cards/my', {
    userId: _userId,
  });
  if (code == 0 && Array.isArray(data)) {
    return data;
  }
  return [];
};

export const getCardProducts = async (
  _storeId: number,
  _userId?: number
): Promise<IObject[]> => {
  const { code, data } = await GET('/api/cards/products', {
    storeId: _storeId,
    ...(_userId ? { userId: _userId } : {}),
  });
  if (code == 0 && Array.isArray(data)) {
    return data;
  }
  return [];
};

export const purchaseCard = async (_data: IObject): Promise<IObject> => {
  const { code, data } = await POST('/api/cards/purchase', _data);
  if (code == 0) {
    return data;
  }
  return {};
};

export const redeemVoucher = async (_data: IObject): Promise<IObject> => {
  const { code, data } = await POST('/api/cards/voucher-redeem', _data);
  if (code == 0) {
    return data;
  }
  return {};
};
