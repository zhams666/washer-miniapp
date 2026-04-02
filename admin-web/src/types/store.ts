export interface StoreOption {
  id: number;
  storeName: string;
}

export interface StoreItem {
  id: number;
  storeCode?: string;
  storeName: string;
  storeStatus?: number | null;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  longitude?: number | null;
  latitude?: number | null;
  contactName?: string;
  contactPhone?: string;
  businessHours?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface StorePageResult {
  records: StoreItem[];
  total: number;
  size: number;
  current: number;
}

export interface StoreFormPayload {
  storeCode?: string;
  storeName: string;
  storeStatus: number;
  province?: string;
  city?: string;
  district?: string;
  address?: string;
  contactName?: string;
  contactPhone?: string;
  businessHours?: string;
  remark?: string;
}
