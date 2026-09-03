import type { IObject } from '../typings/interface.d';

export const buildQueryPath = (path: string, data?: IObject): string => {
  const query = Object.entries(data || {})
    .filter(([, value]) => value !== undefined && value !== null)
    .map(([key, value]) => `${encodeURIComponent(key)}=${encodeURIComponent(String(value))}`);
  return query.length > 0 ? `${path}?${query.join('&')}` : path;
};
