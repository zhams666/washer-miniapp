import { BaseEnum } from '../config/enums';
import { LOCAL_REQUEST_URL } from '../config/url';
import { GET, POST } from '../utils/request';
import { apiRequest, isCloudBaseTransport } from '../utils/container-request';
import type { IObject, ResponseData } from 'typings/interface.d';

type UserProfileQuery =
  | string
  | number
  | {
      id?: string | number | null;
      openid?: string | null;
      openId?: string | null;
    };

const buildUserProfileParams = (query: UserProfileQuery): IObject => {
  if (typeof query === 'string' || typeof query === 'number') {
    return { id: query };
  }

  const params: IObject = {};
  if (query.id !== null && query.id !== undefined && query.id !== '') {
    params.id = query.id;
  }

  const openId = String(query.openid || query.openId || '').trim();
  if (openId) {
    params.openId = openId;
  }

  return params;
};

const getSilentErrorMessage = (error: unknown): string => {
  if (!error) {
    return 'unknown error';
  }
  if (typeof error === 'string') {
    return error;
  }
  if (typeof error === 'object') {
    const data = error as Record<string, any>;
    return String(data.msg || data.message || data.errMsg || 'request failed');
  }
  return String(error);
};

const logSilentRequestError = (action: string, error: unknown) => {
  console.error(`[costomer] ${action} failed: ${getSilentErrorMessage(error)}`, error);
};

const requestSilently = <T>(
  method: 'GET' | 'POST',
  url: string,
  data?: IObject
): Promise<ResponseData<T>> => {
  return apiRequest<T>(
    method,
    url,
    data ? { ...data, wxAppId: BaseEnum.APP_ID } : { wxAppId: BaseEnum.APP_ID }
  ).then((response) => response.code === 0 ? response : Promise.reject(response));
};

export const getOpenId = async (_code?: string): Promise<string> => {
  const codeText = String(_code || '').trim();
  const params = codeText ? { code: codeText } : {};
  const { code, data } = await GET<string>('/costomer/getOpenId', params);
  if (code == 0) {
    return String(data || '');
  }
  return '';
};

export const getOpenIdSilently = async (_code?: string): Promise<string> => {
  const codeText = String(_code || '').trim();
  if (!codeText) {
    console.error('[costomer] getOpenId failed: code is required');
    return '';
  }

  try {
    const { data } = await requestSilently<string>(
      'GET',
      '/costomer/getOpenId',
      { code: codeText }
    );
    return String(data || '');
  } catch (error) {
    logSilentRequestError('getOpenId', error);
    return '';
  }
};

export const getPhone = async (
  _code: string,
  _openId: string
): Promise<string> => {
  const { code, data } = await GET<string>('/costomer/getPhone', {
    code: _code,
    openId: _openId,
  });
  if (code == 0) {
    return String(data || '');
  }
  return '';
};

export type PhoneLoginResult = {
  userId: number | string;
  openId?: string;
  mobile?: string;
  mergedUserId?: number | string | null;
  profile?: IObject;
};

export const phoneLogin = async (_data: IObject): Promise<PhoneLoginResult | null> => {
  const { code, data } = await POST<PhoneLoginResult>('/costomer/phoneLogin', _data);
  if (code == 0 && data && typeof data === 'object') {
    return data as PhoneLoginResult;
  }
  return null;
};

export const sendLoginCode = async (_data: IObject): Promise<IObject | null> => {
  const { code, data } = await POST<IObject>('/costomer/sendLoginCode', _data);
  if (code == 0 && data && typeof data === 'object') {
    return data;
  }
  return null;
};

export const mobileCodeLogin = async (_data: IObject): Promise<PhoneLoginResult | null> => {
  const { code, data } = await POST<PhoneLoginResult>('/costomer/mobileCodeLogin', _data);
  if (code == 0 && data && typeof data === 'object') {
    return data as PhoneLoginResult;
  }
  return null;
};

export const mobileLogin = async (_data: IObject): Promise<PhoneLoginResult | null> => {
  const { code, data } = await POST<PhoneLoginResult>('/costomer/mobileLogin', _data);
  if (code == 0 && data && typeof data === 'object') {
    return data as PhoneLoginResult;
  }
  return null;
};

export const getUserProfile = async (
  query: UserProfileQuery
): Promise<IObject> => {
  const { code, data } = await GET<IObject>(
    '/costomer/getUserInfo',
    buildUserProfileParams(query)
  );
  if (code == 0) {
    return data;
  }
  return {};
};

export const getUserProfileSilently = async (
  query: UserProfileQuery
): Promise<IObject | null> => {
  try {
    const { data } = await requestSilently<IObject>(
      'GET',
      '/costomer/getUserInfo',
      buildUserProfileParams(query)
    );
    if (data && typeof data === 'object' && !Array.isArray(data)) {
      return data as IObject;
    }
  } catch (error) {
    logSilentRequestError('getUserProfile', error);
    return null;
  }

  return null;
};

export const saveUserProfile = async (_data: IObject): Promise<string> => {
  const { code, data } = await POST<string>('/costomer/saveUserInfo', _data);
  if (code == 0) {
    return String(data || '');
  }
  return '';
};

export const saveUserProfileSilently = async (
  _data: IObject
): Promise<string> => {
  try {
    const { data } = await requestSilently<string>(
      'POST',
      '/costomer/saveUserInfo',
      _data
    );
    return String(data || '');
  } catch (error) {
    logSilentRequestError('saveUserProfile', error);
    return '';
  }
};

export const uploadAvatar = (filePath: string): Promise<string> => {
  return new Promise((resolve, reject) => {
    const path = String(filePath || '').trim();
    if (!path) {
      reject(new Error('avatar file path is required'));
      return;
    }
    if (isCloudBaseTransport()) {
      reject(new Error('Avatar upload is unavailable in the CloudBase test transport'));
      return;
    }

    wx.uploadFile({
      url: LOCAL_REQUEST_URL + '/costomer/uploadAvatar',
      filePath: path,
      name: 'file',
      timeout: 10000,
      formData: {
        wxAppId: BaseEnum.APP_ID,
      },
      success({ statusCode, data }) {
        let response: ResponseData<any> | null = null;
        try {
          response =
            typeof data === 'string'
              ? (JSON.parse(data) as ResponseData<any>)
              : (data as ResponseData<any>);
        } catch (error) {
          reject(error);
          return;
        }

        if (statusCode === 200 && response && response.code === 0) {
          const payload = response.data;
          if (typeof payload === 'string') {
            resolve(payload);
            return;
          }
          if (payload && typeof payload === 'object') {
            const avatarUrl = String(payload.avatarUrl || payload.url || '').trim();
            if (avatarUrl) {
              resolve(avatarUrl);
              return;
            }
          }
        }

        reject(response || new Error('upload avatar failed'));
      },
      fail: reject,
    });
  });
};

export const getUserPlate = async (_id: string): Promise<IObject> => {
  const { code, data } = await GET<IObject>('/car/getList', {
    id: _id,
  });
  if (code == 0) {
    return data;
  }
  return {};
};
