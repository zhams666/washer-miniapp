import type { IObject, ResponseData } from '../typings/interface.d';
import { LOCAL_REQUEST_URL, TENCENT_MAP_URL } from '../config/url';
import { BaseEnum, StorageEnum } from '../config/enums';
import { apiRequest, isCloudBaseTransport } from './container-request';

const getResponseMessage = (response: ResponseData<any>) => {
  return response.msg || response.message || 'Request failed';
};

const buildJsonHeaders = () => {
  const openId = String(wx.getStorageSync(StorageEnum.OPEN_ID) || '').trim();
  return {
    'content-type': 'application/json',
    ...(openId ? { 'X-Washer-Openid': openId } : {}),
  };
};

export const GET = <T>(
  _url: string,
  _data?: IObject
): Promise<ResponseData<T> | undefined | any> => {
  return apiRequest<T>('GET', _url, { ..._data, wxAppId: BaseEnum.APP_ID }, buildJsonHeaders())
    .then((responseData) => {
      if (responseData.code === 0) {
        return responseData;
      }
      wx.showToast({ title: getResponseMessage(responseData), icon: 'error' });
      return Promise.reject(responseData);
    });
};

export const POST = <T>(
  _url: string,
  _data: IObject
): Promise<ResponseData<T> | undefined | any> => {
  return apiRequest<T>('POST', _url, { ..._data, wxAppId: BaseEnum.APP_ID }, buildJsonHeaders())
    .then((responseData) => {
      if (responseData.code === 0) {
        return responseData;
      }
      wx.showToast({ title: getResponseMessage(responseData), icon: 'error' });
      return Promise.reject(responseData);
    });
};

export const UPLOAD = <T>(
  ops: IObject
): Promise<ResponseData<T> | undefined | any> => {
  if (isCloudBaseTransport()) {
    return Promise.reject(new Error('Avatar upload is unavailable in the CloudBase test transport'));
  }
  return new Promise(function (resolve, reject) {
    wx.uploadFile({
      url: LOCAL_REQUEST_URL + ops.url,
      filePath: ops.filesPath,
      name: 'file',
      formData: {
        ...ops.data,
        wxAppId: BaseEnum.APP_ID,
      },
      success({ statusCode, data }) {
        const reponseData: ResponseData<T> = (data as unknown) as ResponseData;
        if (statusCode == 200) {
          resolve(reponseData);
        } else {
          reject(reponseData);
        }
      },
      fail(err) {
        reject(err);
      },
    });
  });
};

export const TENCENT_MAP_GET = <T>(
  _url: string,
  _data: IObject
): Promise<ResponseData<T> | undefined | any> => {
  return new Promise(function (resolve, reject) {
    wx.request({
      url: TENCENT_MAP_URL + _url,
      data: { ..._data, wxAppId: BaseEnum.APP_ID },
      header: {
        ...buildJsonHeaders(),
      },
      method: 'GET',
      success({ statusCode, data }) {
        const reponseData: ResponseData<T> = (data as unknown) as ResponseData;
        if (statusCode == 200) {
          resolve(reponseData);
        } else {
          wx.showToast({
            title: getResponseMessage(reponseData),
            icon: 'error',
          });
          reject(reponseData);
        }
      },
      fail(err) {
        reject(err);
      },
    });
  });
};
