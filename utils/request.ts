import type { IObject, ResponseData } from '../typings/interface.d';
import { REQUEST_URL, TENCENT_MAP_URL } from '../config/url';
import { BaseEnum } from '../config/enums';

const getResponseMessage = (response: ResponseData<any>) => {
  return response.msg || response.message || 'Request failed';
};

export const GET = <T>(
  _url: string,
  _data?: IObject
): Promise<ResponseData<T> | undefined | any> => {
  return new Promise(function (resolve, reject) {
    wx.request({
      url: REQUEST_URL + _url,
      data: { ..._data, wxAppId: BaseEnum.APP_ID },
      header: {
        'content-type': 'application/json',
      },
      method: 'GET',
      success({ statusCode, data }) {
        const reponseData: ResponseData<T> = (data as unknown) as ResponseData;
        if (statusCode == 200 && reponseData.code == 0) {
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

export const POST = <T>(
  _url: string,
  _data: IObject
): Promise<ResponseData<T> | undefined | any> => {
  return new Promise(function (resolve, reject) {
    wx.request({
      url: REQUEST_URL + _url,
      data: { ..._data, wxAppId: BaseEnum.APP_ID },
      header: {
        'content-type': 'application/json',
      },
      method: 'POST',
      success({ statusCode, data }) {
        const reponseData: ResponseData<T> = (data as unknown) as ResponseData;
        if (statusCode == 200 && reponseData.code == 0) {
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

export const UPLOAD = <T>(
  ops: IObject
): Promise<ResponseData<T> | undefined | any> => {
  return new Promise(function (resolve, reject) {
    wx.uploadFile({
      url: REQUEST_URL + ops.url,
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
        'content-type': 'application/json',
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
