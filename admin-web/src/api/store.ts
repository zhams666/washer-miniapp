import http from './http';
import type { StoreFormPayload, StoreItem, StoreOption, StorePageResult } from '@/types/store';

export const fetchStorePage = (params: { page: number; size: number; keyword?: string }) =>
  http.get<StorePageResult>('/api/stores', { params });

export const fetchStoreDetail = (id: number) =>
  http.get<StoreItem>(`/api/stores/${id}`);

export const createStore = (payload: StoreFormPayload) =>
  http.post<StoreItem>('/api/stores', payload);

export const updateStore = (id: number, payload: StoreFormPayload) =>
  http.put<StoreItem>(`/api/stores/${id}`, payload);

export const fetchAdminStoreOptions = () =>
  http.get<StoreOption[]>('/api/admin/stores/options');
