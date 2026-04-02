export interface DeviceItem {
  id: number;
  deviceCode?: string;
  deviceName?: string;
  storeId: number;
  storeName?: string;
  deviceType?: string;
  deviceRole?: string;
  status?: string;
  deviceStatus?: string;
  protocolType?: string;
  firmwareVersion?: string;
  remark?: string;
  createdAt?: string;
  updatedAt?: string;
}

export interface DeviceFormPayload {
  storeId: number;
  deviceCode?: string;
  deviceName?: string;
  deviceType?: string;
  deviceRole?: string;
  deviceStatus?: string;
  protocolType?: string;
  firmwareVersion?: string;
  remark?: string;
}
