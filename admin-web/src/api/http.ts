import axios from 'axios';

type ApiEnvelope<T> = {
  code: number;
  message: string;
  data: T;
};

const instance = axios.create({
  baseURL: import.meta.env.VITE_API_BASE_URL || '',
  timeout: 15000,
});

const unwrap = <T>(payload: ApiEnvelope<T>) => {
  if (payload.code !== 0) {
    throw new Error(payload.message || '请求失败');
  }

  return payload.data;
};

const http = {
  async get<T>(url: string, config?: object) {
    const response = await instance.get<ApiEnvelope<T>>(url, config);
    return unwrap(response.data);
  },
  async post<T>(url: string, data?: object, config?: object) {
    const response = await instance.post<ApiEnvelope<T>>(url, data, config);
    return unwrap(response.data);
  },
  async put<T>(url: string, data?: object, config?: object) {
    const response = await instance.put<ApiEnvelope<T>>(url, data, config);
    return unwrap(response.data);
  },
};

export default http;
