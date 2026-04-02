import http from './http';
import type { AdminOrderDetail, AdminOrderPageResult, OrderQueryParams, StoreOption } from '@/types/order';

export const fetchStoreOptions = () => http.get<StoreOption[]>('/api/admin/stores/options');

export const fetchOrderPage = (params: OrderQueryParams) =>
  http.get<AdminOrderPageResult>('/api/admin/orders', { params });

export const fetchOrderDetail = (id: number) =>
  http.get<AdminOrderDetail>(`/api/admin/orders/${id}`);
