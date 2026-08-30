import type { StoreOption } from './store';

export interface PermissionOption {
  value: string;
  label: string;
  description?: string;
}

export interface MiniAdminStoreOption {
  id: number;
  franchiseeId?: number;
  storeName: string;
}

export interface MiniAdminPermissionOptions {
  roles: PermissionOption[];
  dataScopes: PermissionOption[];
  permissions: PermissionOption[];
  stores: StoreOption[];
}

export interface MiniAdminPermissionItem {
  exists: boolean;
  userId: number;
  userNo: string;
  nickname?: string;
  mobile?: string;
  openId?: string;
  staffId?: number;
  staffNo?: string;
  staffName?: string;
  roleCode: string;
  roleName: string;
  dataScope: string;
  dataScopeName: string;
  status: number;
  remark?: string;
  storeIds: number[];
  stores: MiniAdminStoreOption[];
  permissions: string[];
  updatedAt?: string;
}

export interface MiniAdminPermissionPayload {
  userNo: string;
  staffName?: string;
  roleCode: string;
  dataScope: string;
  franchiseeId?: number;
  storeIds: number[];
  status: number;
  remark?: string;
}
