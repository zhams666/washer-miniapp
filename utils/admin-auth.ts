import { StorageEnum } from '../config/enums';
import type { IObject } from '../typings/interface.d';

let redirectingToAdminLogin = false;

export const getAdminToken = (): string => {
  return String(wx.getStorageSync(StorageEnum.ADMIN_TOKEN) || '').trim();
};

export const getCachedAdminProfile = (): IObject | null => {
  const profile = wx.getStorageSync(StorageEnum.ADMIN_PROFILE);
  return profile && typeof profile === 'object' ? (profile as IObject) : null;
};

export const setAdminSession = (token: string, profile: IObject | null) => {
  wx.setStorageSync(StorageEnum.ADMIN_TOKEN, token || '');
  wx.setStorageSync(StorageEnum.ADMIN_PROFILE, profile || null);
};

export const setAdminProfile = (profile: IObject | null) => {
  wx.setStorageSync(StorageEnum.ADMIN_PROFILE, profile || null);
};

export const clearAdminSession = () => {
  wx.setStorageSync(StorageEnum.ADMIN_TOKEN, '');
  wx.setStorageSync(StorageEnum.ADMIN_PROFILE, null);
};

export const redirectToAdminLogin = () => {
  clearAdminSession();
  if (redirectingToAdminLogin) {
    return;
  }
  redirectingToAdminLogin = true;
  wx.redirectTo({
    url: '/pages-admin/login/index',
    complete: () => {
      redirectingToAdminLogin = false;
    },
  });
};

export const ensureAdminToken = (): string => {
  const token = getAdminToken();
  if (!token) {
    redirectToAdminLogin();
    throw new Error('admin token is required');
  }
  return token;
};
