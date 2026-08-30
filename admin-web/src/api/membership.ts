import http from './http';
import type {
  MembershipOrderPage,
  MembershipPlan,
  MembershipSetting,
} from '@/types/membership';

export const fetchMembershipSettings = () =>
  http.get<MembershipSetting>('/api/admin/membership/settings');

export const saveMembershipSettings = (payload: MembershipSetting) =>
  http.put<MembershipSetting>('/api/admin/membership/settings', payload);

export const fetchMembershipPlans = () =>
  http.get<MembershipPlan[]>('/api/admin/membership/plans');

export const createMembershipPlan = (payload: MembershipPlan) =>
  http.post<MembershipPlan>('/api/admin/membership/plans', payload);

export const updateMembershipPlan = (id: number, payload: MembershipPlan) =>
  http.put<MembershipPlan>(`/api/admin/membership/plans/${id}`, payload);

export const disableMembershipPlan = (id: number) =>
  http.delete(`/api/admin/membership/plans/${id}`);

export const fetchMembershipOrders = (params: {
  page: number;
  size: number;
  userId?: number;
  status?: string;
}) => http.get<MembershipOrderPage>('/api/admin/membership/orders', { params });
