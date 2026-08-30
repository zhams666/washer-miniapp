export interface IObject<T = any> {
  [key: string]: T;
}

export interface ResponseData<T = any> {
  code: number;
  data: T;
  msg?: string;
  message?: string;
}

export interface LoginCandidate {
  id: number | string;
  nickname?: string;
  avatarUrl?: string;
  mobile?: string;
  openid?: string;
  userNo?: string;
  lastLoginTime?: string;
}

export interface LoginResponse {
  status: number;
  profile: IObject | null;
  costomerId: number | string | null;
}
