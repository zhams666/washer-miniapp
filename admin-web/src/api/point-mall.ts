import http from './http';
import type { PointMallProduct, PointMallProductPage, PointMallProductQuery } from '@/types/point-mall';

export const fetchPointMallProducts = (params: PointMallProductQuery) =>
  http.get<PointMallProductPage>('/api/admin/point-mall/products', { params });

export const createPointMallProduct = (payload: PointMallProduct) =>
  http.post<PointMallProduct>('/api/admin/point-mall/products', payload);

export const updatePointMallProduct = (id: number, payload: PointMallProduct) =>
  http.put<PointMallProduct>(`/api/admin/point-mall/products/${id}`, payload);

export const updatePointMallProductStatus = (id: number, status: 0 | 1) =>
  http.patch<PointMallProduct>(`/api/admin/point-mall/products/${id}/status`, { status });
