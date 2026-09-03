import { BaseEnum } from '../config/enums';
import type { IObject } from 'typings/interface.d';
import { apiRequest } from '../utils/container-request';

const silentGet = (_url: string, _data?: IObject): Promise<IObject> => {
  const requestData = Object.keys(_data || {}).reduce((acc: IObject, key) => {
    const value = (_data || {})[key];
    if (value !== undefined && value !== null) {
      acc[key] = value;
    }
    return acc;
  }, {});
  return apiRequest<IObject>('GET', _url, { ...requestData, wxAppId: BaseEnum.APP_ID })
    .then((response) => response.code === 0 ? (response.data || {}) : Promise.reject(response));
};

/**
 * 获取门店分页列表
 */
export const getStoreList = async (
  _page = 1,
  _size = 10,
  _keyword = ''
): Promise<IObject> => {
  return silentGet('/api/stores', {
    page: _page,
    size: _size,
    keyword: _keyword,
  });
};

export const getStoreDetail = async (_id: number): Promise<IObject> => {
  return silentGet(`/api/stores/${_id}`);
};

/**
 * 小程序门店列表
 */
export const getMiniStoreList = async (
  _page = 1,
  _size = 10,
  _userId?: number,
  _userLat?: number,
  _userLng?: number
): Promise<IObject> => {
  return silentGet('/api/stores/miniapp-list', {
    page: _page,
    size: _size,
    userId: _userId,
    userLat: _userLat,
    userLng: _userLng,
  });
};

/**
 * 小程序门店详情
 */
export const getMiniStoreDetail = async (
  _id: number,
  _userId?: number,
  _userLat?: number,
  _userLng?: number
): Promise<IObject> => {
  return silentGet(`/api/stores/${_id}/miniapp-detail`, {
    userId: _userId,
    userLat: _userLat,
    userLng: _userLng,
  });
};

/**
 * 小程序门店工位状态
 */
export const getStoreBayStatus = async (_storeId?: number | string): Promise<any> => {
  return silentGet('/api/stores/bay-status', {
    storeId: _storeId,
  });
};
