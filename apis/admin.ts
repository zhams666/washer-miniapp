import { BaseEnum } from '../config/enums';
import { REQUEST_URL } from '../config/url';
import type { IObject, ResponseData } from '../typings/interface.d';
import { getAdminToken } from '../utils/admin-auth';

type HttpMethod = 'GET' | 'POST';

export type MiniAdminLoginResult = {
  bound: boolean;
  token?: string;
  openId?: string;
  message?: string;
  profile?: IObject | null;
};

const cleanParams = (_data?: IObject): IObject => {
  return Object.keys(_data || {}).reduce((acc: IObject, key) => {
    const value = (_data || {})[key];
    if (value !== undefined && value !== null && value !== '') {
      acc[key] = value;
    }
    return acc;
  }, {});
};

const request = <T>(_method: HttpMethod, _url: string, _data?: IObject): Promise<T> => {
  return new Promise((resolve, reject) => {
    const token = getAdminToken();
    wx.request({
      url: REQUEST_URL + _url,
      method: _method,
      data: {
        ...cleanParams(_data),
        wxAppId: BaseEnum.APP_ID,
      },
      header: {
        'content-type': 'application/json',
        ...(token ? { 'X-Washer-Admin-Token': token } : {}),
      },
      success({ statusCode, data }) {
        const response = (data || {}) as ResponseData<T>;
        if (statusCode === 200 && response.code === 0) {
          resolve(response.data);
          return;
        }
        wx.showToast({
          title: response.message || response.msg || '请求失败',
          icon: 'none',
        });
        reject(response);
      },
      fail(error) {
        reject(error);
      },
    });
  });
};

const requestLoginCode = (): Promise<string> => {
  return new Promise((resolve, reject) => {
    wx.login({
      success(result) {
        if (result.code) {
          resolve(result.code);
          return;
        }
        reject(result);
      },
      fail: reject,
    });
  });
};

export const miniAdminLoginWithCode = (code: string): Promise<MiniAdminLoginResult> =>
  request<MiniAdminLoginResult>('POST', '/api/mini-admin/auth/login', { code });

export const miniAdminLogin = async (): Promise<MiniAdminLoginResult> => {
  const code = await requestLoginCode();
  return miniAdminLoginWithCode(code);
};

export const miniAdminLocalLogin = (role?: string): Promise<MiniAdminLoginResult> => {
  const roleCode = String(role || '').trim();
  if (roleCode === 'platform') {
    return miniAdminLoginWithCode('mock-platform');
  }
  if (roleCode === 'franchisee') {
    return miniAdminLoginWithCode('mock-franchisee');
  }
  if (roleCode === 'store') {
    return miniAdminLoginWithCode('mock-store');
  }
  if (roleCode === 'staff') {
    return miniAdminLoginWithCode('mock-staff');
  }
  return miniAdminLoginWithCode('local-test');
};

export const getMiniAdminCurrent = () =>
  request<IObject>('GET', '/api/mini-admin/auth/current');

export const getMiniAdminStores = () =>
  request<IObject[]>('GET', '/api/mini-admin/stores/options');

export const getMiniAdminDashboard = (_params?: IObject) =>
  request<IObject>('GET', '/api/mini-admin/dashboard', _params);

export const getMiniAdminOperationOverview = (_params?: IObject) =>
  request<IObject>('GET', '/api/mini-admin/operation/overview', _params);

export const getMiniAdminDevices = (_params?: IObject) =>
  request<IObject[]>('GET', '/api/mini-admin/devices', _params);

export const startMiniAdminDevice = (_id: number) =>
  request<IObject>('POST', `/api/mini-admin/devices/${_id}/start`);

export const stopMiniAdminDevice = (_id: number) =>
  request<IObject>('POST', `/api/mini-admin/devices/${_id}/stop`);

export const getMiniAdminOrders = (_params?: IObject) =>
  request<IObject>('GET', '/api/mini-admin/orders', _params);

export const searchMiniAdminUsers = (_params?: IObject) =>
  request<IObject[]>('GET', '/api/mini-admin/users/search', _params);

export const getMiniAdminUserAssets = (_userId: number, _params?: IObject) =>
  request<IObject>('GET', `/api/mini-admin/users/${_userId}/assets`, _params);

export const adjustMiniAdminWallet = (_data: IObject) =>
  request<IObject>('POST', '/api/mini-admin/asset/wallet-adjustments', _data);

export const createMiniAdminFine = (_data: IObject) =>
  request<IObject>('POST', '/api/mini-admin/asset/wallet-fines', _data);

export const adjustMiniAdminCard = (_data: IObject) =>
  request<IObject>('POST', '/api/mini-admin/asset/card-adjustments', _data);
