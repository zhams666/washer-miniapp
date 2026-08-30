import { GET, POST } from '../utils/request';

export type PointMallProduct = {
  id: number;
  title: string;
  description?: string;
  coverImage?: string;
  productType: 'wash_service' | 'coupon' | 'physical' | string;
  pointsPrice: number;
  limitPerUser?: number;
};

export const getPointMallProducts = async (): Promise<PointMallProduct[]> => {
  const { code, data } = await GET<PointMallProduct[]>('/point-mall/products');
  return code === 0 && Array.isArray(data) ? data : [];
};

export const redeemPointMallProduct = async (data: {
  userId: number;
  productId: number;
  requestNo: string;
}): Promise<Record<string, any>> => {
  const { code, data: result } = await POST('/point-mall/redemptions', data);
  return code === 0 && result ? result : {};
};
