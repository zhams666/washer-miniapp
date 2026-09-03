import {
  API_TRANSPORT,
  CLOUDBASE_ENV_ID,
  CLOUDBASE_SERVICE_NAME,
  LOCAL_REQUEST_URL,
} from '../config/url';
import type { IObject, ResponseData } from '../typings/interface.d';
import { buildQueryPath } from './request-path';

export type ApiRequestMethod = 'GET' | 'POST';

const normalizeResponse = <T>(data: unknown): ResponseData<T> => {
  if (data && typeof data === 'object') {
    return data as ResponseData<T>;
  }
  return {
    code: -1,
    data: null as T,
    message: 'Invalid API response',
  };
};

const localRequest = <T>(
  method: ApiRequestMethod,
  path: string,
  data: IObject | undefined,
  header: Record<string, string>
): Promise<ResponseData<T>> => new Promise((resolve, reject) => {
  wx.request({
    url: LOCAL_REQUEST_URL + path,
    data,
    header,
    method,
    timeout: 8000,
    success({ data: response }) {
      resolve(normalizeResponse<T>(response));
    },
    fail: reject,
  });
});

export const apiRequest = <T>(
  method: ApiRequestMethod,
  path: string,
  data?: IObject,
  header: Record<string, string> = {}
): Promise<ResponseData<T>> => {
  const requestHeader = {
    'content-type': 'application/json',
    ...header,
  };
  if (API_TRANSPORT === 'local') {
    return localRequest<T>(method, path, data, requestHeader);
  }

  return wx.cloud.callContainer({
    // Keep the request bound to the environment that owns the Cloud Run service.
    config: {
      env: CLOUDBASE_ENV_ID,
    },
    service: CLOUDBASE_SERVICE_NAME,
    path: method === 'GET' ? buildQueryPath(path, data) : path,
    method,
    data: method === 'GET' ? undefined : data,
    header: {
      'X-WX-SERVICE': CLOUDBASE_SERVICE_NAME,
      ...requestHeader,
    },
    timeout: 8000,
  }).then(({ data: response }) => normalizeResponse<T>(response));
};

export const isCloudBaseTransport = (): boolean => API_TRANSPORT === 'cloudbase';
