import http from './http';
import type {
  MiniAdminPermissionItem,
  MiniAdminPermissionOptions,
  MiniAdminPermissionPayload,
} from '@/types/mini-admin-permission';

export const fetchMiniAdminPermissionOptions = () =>
  http.get<MiniAdminPermissionOptions>('/api/admin/mini-admin-permissions/options');

export const fetchMiniAdminPermission = (userNo: string) =>
  http.get<MiniAdminPermissionItem>('/api/admin/mini-admin-permissions', {
    params: { userNo },
  });

export const saveMiniAdminPermission = (payload: MiniAdminPermissionPayload) =>
  http.post<MiniAdminPermissionItem>('/api/admin/mini-admin-permissions', payload);

export const deleteMiniAdminPermission = (userNo: string) =>
  http.delete<MiniAdminPermissionItem>('/api/admin/mini-admin-permissions', {
    params: { userNo },
  });
