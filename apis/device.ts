import { GET } from '../utils/request';
import type { IObject } from 'typings/interface.d';

export const getDeviceList = async (
  _storeId: number,
  _keyword = ''
): Promise<IObject[]> => {
  const { code, data } = await GET('/api/devices', {
    storeId: _storeId,
    keyword: _keyword,
  });

  if (code == 0 && Array.isArray(data)) {
    return data;
  }

  return [];
};

export const getDeviceDetail = async (_id: number): Promise<IObject> => {
  const { code, data } = await GET(`/api/devices/${_id}`);
  if (code == 0) {
    return data;
  }
  return {};
};
