import { BaseEnum } from '../config/enums';
import type { IObject } from 'typings/interface.d';
import { apiRequest } from '../utils/container-request';

const silentGet = (_url: string, _data?: IObject): Promise<IObject> => {
  return apiRequest<IObject>('GET', _url, { ..._data, wxAppId: BaseEnum.APP_ID })
    .then((response) => response.code === 0 ? (response.data || {}) : Promise.reject(response));
};

const silentPost = (_url: string, _data?: IObject): Promise<IObject> => {
  return apiRequest<IObject>('POST', _url, { ..._data, wxAppId: BaseEnum.APP_ID })
    .then((response) => response.code === 0 ? (response.data || {}) : Promise.reject(response));
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
