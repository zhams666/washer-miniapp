export type PointMallProductType = 'wash_service' | 'coupon' | 'physical';

export interface PointMallProduct {
  id?: number;
  title: string;
  description?: string;
  coverImage?: string;
  productType: PointMallProductType;
  pointsPrice: number;
  stockTotal: number;
  limitPerUser: number;
  effectiveTime?: string;
  expireTime?: string;
  status: 0 | 1;
  sortOrder: number;
  createdAt?: string;
  updatedAt?: string;
}

export interface PointMallProductQuery {
  page: number;
  size: number;
  keyword?: string;
  status?: number;
  productType?: PointMallProductType;
}

export interface PointMallProductPage {
  records: PointMallProduct[];
  total: number;
  current?: number;
  size?: number;
}
