import { GET } from '../utils/request';
import type { IObject } from 'typings/interface.d';

/**
 * 获取门店分页列表
 */
export const getStoreList = async (
  _page = 1,
  _size = 10,
  _keyword = ''
): Promise<IObject> => {
  const { code, data } = await GET('/api/stores', {
    page: _page,
    size: _size,
    keyword: _keyword,
  });

  if (code == 0) {
    return data;
  }

  return {};
};
