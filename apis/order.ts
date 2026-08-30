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

export const getOrderPage = async (
  _page = 1,
  _size = 10,
  _userId?: number
): Promise<IObject> => {
  const { code, data } = await GET('/api/orders', {
    page: _page,
    size: _size,
    userId: _userId,
  });
  if (code == 0) {
    return data;
  }
  return {};
};

export const startWashOrder = async (_data: IObject): Promise<IObject> => {
  const { code, data } = await POST('/api/orders/start-wash', _data);
  if (code == 0) {
    return data;
  }
  return {};
};

export const joinWashQueue = async (_data: IObject): Promise<IObject> => {
  const { code, data } = await POST('/api/queues/join', _data);
  if (code == 0) {
    return data;
  }
  return {};
};

export const checkWashQueueLocation = async (_data: IObject): Promise<IObject> => {
  const { code, data } = await POST('/api/queues/check-location', _data);
  if (code == 0) {
    return data;
  }
  return {};
};

export const startOrder = async (_id: number): Promise<IObject> => {
  const { code, data } = await POST(`/api/orders/${_id}/start`, {});
  if (code == 0) {
    return data;
  }
  return {};
};

export const completeOrder = async (_id: number): Promise<IObject> => {
  const { code, data } = await POST(`/api/orders/${_id}/complete`, {});
  if (code == 0) {
    return data;
  }
  return {};
};

export const checkOrderAutoStop = async (_id: number): Promise<IObject> => {
  const { code, data } = await POST(`/api/orders/${_id}/auto-stop-check`, {});
  if (code == 0) {
    return data;
  }
  return {};
};

export const getOrderDetail = async (_id: number): Promise<IObject> => {
  const { code, data } = await GET(`/api/orders/${_id}`);
  if (code == 0) {
    return data;
  }
  return {};
};

export const getOrderStatusLogs = async (_id: number): Promise<IObject[]> => {
  const { code, data } = await GET(`/api/orders/${_id}/status-logs`);
  if (code == 0 && Array.isArray(data)) {
    return data;
  }
  return [];
};

export const getOrderPaymentDetails = async (_id: number): Promise<IObject[]> => {
  const { code, data } = await GET(`/api/orders/${_id}/payment-details`);
  if (code == 0 && Array.isArray(data)) {
    return data;
  }
  return [];
};

export const getDurationRanking = async (
  _scope = 'day',
  _userId?: number,
  _limit = 10
): Promise<IObject> => {
  const { code, data } = await GET('/api/orders/duration-ranking', {
    scope: _scope,
    userId: _userId,
    limit: _limit,
  });
  if (code == 0 && data) {
    return data;
  }
  return {};
};
