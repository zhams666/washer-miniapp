import { REQUEST_URL } from '../config/url';
import { BaseEnum } from '../config/enums';
import type { IObject } from 'typings/interface.d';

const silentGet = (_url: string, _data?: IObject): Promise<IObject> => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: REQUEST_URL + _url,
      data: { ..._data, wxAppId: BaseEnum.APP_ID },
      header: {
        'content-type': 'application/json',
      },
      method: 'GET',
      success({ statusCode, data }) {
        const response = (data || {}) as Record<string, any>;
        if (statusCode === 200 && response.code === 0) {
          resolve((response.data || {}) as IObject);
          return;
        }
        reject(response);
      },
      fail(err) {
        reject(err);
      },
    });
  });
};

const silentPost = (_url: string, _data?: IObject): Promise<IObject> => {
  return new Promise((resolve, reject) => {
    wx.request({
      url: REQUEST_URL + _url,
      data: { ..._data, wxAppId: BaseEnum.APP_ID },
      header: {
        'content-type': 'application/json',
      },
      method: 'POST',
      success({ statusCode, data }) {
        const response = (data || {}) as Record<string, any>;
        if (statusCode === 200 && response.code === 0) {
          resolve((response.data || {}) as IObject);
          return;
        }
        reject(response);
      },
      fail(err) {
        reject(err);
      },
    });
  });
};

export const getDeviceList = async (
  _storeId?: number,
  _keyword = ''
): Promise<IObject[]> => {
  const data = await silentGet('/api/devices', {
    storeId: _storeId,
    keyword: _keyword,
  });
  if (Array.isArray(data)) {
    return data;
  }

  return [];
};

export const getDeviceDetail = async (_id: number): Promise<IObject> => {
  return silentGet(`/api/devices/${_id}`);
};

export const mockStartDevice = async (_id: number): Promise<IObject> => {
  return silentPost(`/api/devices/${_id}/start`, {});
};

export const mockStopDevice = async (_id: number): Promise<IObject> => {
  return silentPost(`/api/devices/${_id}/stop`, {});
};
