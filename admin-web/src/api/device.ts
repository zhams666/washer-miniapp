import http from './http';
import type { DeviceFormPayload, DeviceItem } from '@/types/device';

export const fetchDeviceList = (params?: { storeId?: number; keyword?: string }) =>
  http.get<DeviceItem[]>('/api/devices', { params });

export const fetchDeviceDetail = (id: number) =>
  http.get<DeviceItem>(`/api/devices/${id}`);

export const createDevice = (payload: DeviceFormPayload) =>
  http.post<DeviceItem>('/api/devices', payload);

export const updateDevice = (id: number, payload: DeviceFormPayload) =>
  http.put<DeviceItem>(`/api/devices/${id}`, payload);
