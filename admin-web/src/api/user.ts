import http from './http';
import type {
  AdminUserOverview,
  AdminUserPageResult,
  AdminUserFormPayload,
  AdminManualFinePayload,
  AdminManualRechargePayload,
  AdminManualRefundPayload,
  AdminUserCardAdjustResult,
  AdminUserCardDetail,
  AdminUserCardManualAddPayload,
  AdminUserCardManualReducePayload,
  AdminUserCardPageResult,
  AdminUserCardQueryParams,
} from '@/types/user';

export const fetchUserPage = (params: { page: number; size: number; keyword?: string }) =>
  http.get<AdminUserPageResult>('/api/admin/users', { params });

export const fetchUserOverview = (id: number) =>
  http.get<AdminUserOverview>(`/api/admin/users/${id}`);

export const createUser = (payload: AdminUserFormPayload) =>
  http.post('/api/admin/users', payload);

export const updateUser = (id: number, payload: AdminUserFormPayload) =>
  http.put(`/api/admin/users/${id}`, payload);

export const manualRecharge = (payload: AdminManualRechargePayload) =>
  http.post('/api/admin/wallet-recharges', payload);

export const manualRefund = (payload: AdminManualRefundPayload) =>
  http.post('/api/admin/wallet-refunds', payload);

export const manualFine = (payload: AdminManualFinePayload) =>
  http.post('/api/admin/wallet-fines', payload);

export const fetchUserCards = (userId: number, params: AdminUserCardQueryParams) =>
  http.get<AdminUserCardPageResult>(`/api/admin/users/${userId}/cards`, { params });

export const fetchUserCardDetail = (userId: number, cardId: number) =>
  http.get<AdminUserCardDetail>(`/api/admin/users/${userId}/cards/${cardId}`);

export const manualAddUserCards = (userId: number, payload: AdminUserCardManualAddPayload) =>
  http.post<AdminUserCardAdjustResult>(`/api/admin/users/${userId}/cards/manual-add`, payload);

export const manualReduceUserCards = (userId: number, payload: AdminUserCardManualReducePayload) =>
  http.post<AdminUserCardAdjustResult>(`/api/admin/users/${userId}/cards/manual-reduce`, payload);
