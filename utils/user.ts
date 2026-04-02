import type { IObject, LoginResponse } from 'typings/interface.d';
import { StorageEnum } from '../config/enums';
import {
  getOpenId,
  saveUserProfile,
  getUserProfile,
} from '../apis/costomer';

export const ensureLoginStorage = () => {
  if (wx.getStorageSync(StorageEnum.IS_LOGIN) === '') {
    wx.setStorageSync(StorageEnum.OPEN_ID, null);
    wx.setStorageSync(StorageEnum.USER_PROFILE, null);
    wx.setStorageSync(StorageEnum.COSTOMER_ID, null);
    wx.setStorageSync(StorageEnum.IS_LOGIN, false);
  }
};

/**
 * 微信登录
 * 这个方法应在用户点击授权按钮时调用，不要在 onLaunch 中直接触发。
 */
export const wxLogin = (): Promise<LoginResponse> => {
  return new Promise((resolve) => {
    ensureLoginStorage();

    try {
      wx.getUserProfile({
        desc: '获取您的基本信息',
        success: async ({ userInfo }) => {
          try {
            wx.login({
              success: async (res) => {
                const openId: string = await getOpenId(res.code);
                const phone = '';
                const costomerId: string = await saveUserProfile({
                  ...userInfo,
                  openid: openId,
                  phone,
                });
                const profile: IObject = await getUserProfile(costomerId);
                wx.setStorageSync(StorageEnum.OPEN_ID, openId);
                wx.setStorageSync(StorageEnum.USER_PROFILE, profile);
                wx.setStorageSync(StorageEnum.COSTOMER_ID, costomerId);
                wx.setStorageSync(StorageEnum.IS_LOGIN, true);
                resolve({ status: 0, profile, costomerId } as LoginResponse);
              },
              fail: (error) => {
                console.error('wx.login failed:', error);
                wx.setStorageSync(StorageEnum.OPEN_ID, null);
                wx.setStorageSync(StorageEnum.USER_PROFILE, null);
                wx.setStorageSync(StorageEnum.COSTOMER_ID, null);
                wx.setStorageSync(StorageEnum.IS_LOGIN, false);
                resolve({
                  status: 500,
                  profile: null,
                  costomerId: null,
                } as LoginResponse);
              },
            });
          } catch (error) {
            console.error('wxLogin inner error:', error);
            wx.setStorageSync(StorageEnum.OPEN_ID, null);
            wx.setStorageSync(StorageEnum.USER_PROFILE, null);
            wx.setStorageSync(StorageEnum.COSTOMER_ID, null);
            wx.setStorageSync(StorageEnum.IS_LOGIN, false);
            resolve({
              status: 500,
              profile: null,
              costomerId: null,
            } as LoginResponse);
          }
        },
        fail: () => {
          wx.setStorageSync(StorageEnum.OPEN_ID, null);
          wx.setStorageSync(StorageEnum.USER_PROFILE, null);
          wx.setStorageSync(StorageEnum.COSTOMER_ID, null);
          wx.setStorageSync(StorageEnum.IS_LOGIN, false);
          resolve({
            status: 500,
            profile: null,
            costomerId: null,
          } as LoginResponse);
        },
      });
    } catch (error) {
      console.error('wx.getUserProfile failed before callback:', error);
      wx.setStorageSync(StorageEnum.OPEN_ID, null);
      wx.setStorageSync(StorageEnum.USER_PROFILE, null);
      wx.setStorageSync(StorageEnum.COSTOMER_ID, null);
      wx.setStorageSync(StorageEnum.IS_LOGIN, false);
      resolve({
        status: 500,
        profile: null,
        costomerId: null,
      } as LoginResponse);
    }
  });
};
