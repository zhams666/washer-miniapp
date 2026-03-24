import { GET, POST } from '../utils/request';
import type { IObject } from 'typings/interface.d';

export const getSimpleOrderList = async (
  _userId?: number,
  _size = 10
): Promise<IObject[]> => {
  const { code, data } = await GET('/api/orders/simple-list', {
    userId: _userId,
    size: _size,
  });

  if (code == 0 && Array.isArray(data)) {
    return data;
  }

  return [];
};

export const createSimpleOrder = async (_data: IObject): Promise<IObject> => {
  const { code, data } = await POST('/api/orders/simple-create', _data);
  if (code == 0) {
    return data;
  }
  return {};
};
