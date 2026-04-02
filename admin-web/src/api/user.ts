import http from './http';
import type { AdminUserOverview, AdminUserPageResult } from '@/types/user';

export const fetchUserPage = (params: { page: number; size: number; keyword?: string }) =>
  http.get<AdminUserPageResult>('/api/admin/users', { params });

export const fetchUserOverview = (id: number) =>
  http.get<AdminUserOverview>(`/api/admin/users/${id}`);
