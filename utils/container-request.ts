import {
  API_TRANSPORT,
  CLOUDBASE_ENV_ID,
  CLOUDBASE_SERVICE_NAME,
  LOCAL_REQUEST_URL,
} from '../config/url';
import type { IObject, ResponseData } from '../typings/interface.d';
import { buildQueryPath } from './request-path';

export type ApiRequestMethod = 'GET' | 'POST';

type TraceableError = Error & {
  traceId?: string;
};

const createTraceId = (): string => {
  return `WASHER_${Date.now()}_${Math.random().toString(36).slice(2, 8)}`;
};

const summarizeError = (error: unknown): string => {
  if (error && typeof error === 'object') {
    const record = error as Record<string, unknown>;
    const message = record.message || record.errMsg || record.msg;
    if (message) {
      return String(message).replace(/[\r\n]+/g, ' ').slice(0, 180);
    }
  }
  return String(error || 'unknown error').replace(/[\r\n]+/g, ' ').slice(0, 180);
};

const attachTraceId = (error: unknown, traceId: string): TraceableError => {
  if (error && typeof error === 'object') {
    const traceable = error as TraceableError;
    traceable.traceId = traceId;
    return traceable;
  }
  const wrapped = new Error(summarizeError(error)) as TraceableError;
  wrapped.traceId = traceId;
  return wrapped;
};

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
  const traceId = createTraceId();
  const requestHeader = {
    'content-type': 'application/json',
    'X-Washer-Trace-Id': traceId,
    ...header,
  };
  const request = API_TRANSPORT === 'local'
    ? localRequest<T>(method, path, data, requestHeader)
    : wx.cloud.callContainer({
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

  return request
    .then((response) => ({ ...response, traceId }))
    .catch((error: unknown) => {
      console.error('miniapp_api_transport_failed', {
        traceId,
        method,
        path,
        message: summarizeError(error),
      });
      return Promise.reject(attachTraceId(error, traceId));
    });
};

export const isCloudBaseTransport = (): boolean => API_TRANSPORT === 'cloudbase';
