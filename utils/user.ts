import type { IObject, LoginResponse } from 'typings/interface.d';
import { StorageEnum } from '../config/enums';
import {
  getOpenIdSilently,
  mobileCodeLogin,
  mobileLogin,
  phoneLogin,
  sendLoginCode,
  getUserProfileSilently,
  saveUserProfileSilently,
} from '../apis/costomer';

let ensureCurrentUserPromise: Promise<LoginResponse> | null = null;

type CurrentUserSnapshot = {
  userId: number | null;
  openId: string | null;
  profile: IObject | null;
};

type EnsureCurrentUserOptions = {
  forceRefresh?: boolean;
  silentCreate?: boolean;
};

const normalizeUserId = (value: unknown): number | null => {
  const parsed = Number(value);
  if (Number.isInteger(parsed) && parsed > 0) {
    return parsed;
  }
  return null;
};

const normalizeOpenId = (value: unknown): string | null => {
  if (value === null || value === undefined) {
    return null;
  }

  const text = String(value).trim();
  return text || null;
};

const isPlainObject = (value: unknown): value is IObject => {
  return Boolean(value) && typeof value === 'object' && !Array.isArray(value);
};

const hasProfileData = (profile: unknown): profile is IObject => {
  return isPlainObject(profile) && Object.keys(profile).length > 0;
};

const extractUserIdFromProfile = (profile: unknown): number | null => {
  if (!hasProfileData(profile)) {
    return null;
  }

  return normalizeUserId(profile.id || profile.userId || profile.costomerId);
};

const extractOpenIdFromProfile = (profile: unknown): string | null => {
  if (!hasProfileData(profile)) {
    return null;
  }

  return normalizeOpenId(profile.openid || profile.openId);
};

const extractMobileFromProfile = (profile: unknown): string | null => {
  if (!hasProfileData(profile)) {
    return null;
  }

  const mobile = String(profile.mobile || profile.phone || '').trim();
  return mobile || null;
};

const normalizeMobileText = (value: unknown): string => {
  let mobile = String(value || '').replace(/[\s-]/g, '').trim();
  if (mobile.startsWith('+86')) {
    mobile = mobile.slice(3);
  } else if (mobile.startsWith('86') && mobile.length === 13) {
    mobile = mobile.slice(2);
  }
  return mobile;
};

const MOCK_MOBILE_LOGIN_OPEN_IDS: Record<string, string> = {
  '19552500939': 'mock_openid_user_001',
  '19552500940': 'mock_openid_user_002',
  '19552500941': 'mock_openid_user_003',
  '19552500942': 'mock_openid_user_004',
  '19552500943': 'mock_openid_user_005',
};

const resolveMockMobileLoginOpenId = (mobile: string): string | null => {
  return MOCK_MOBILE_LOGIN_OPEN_IDS[mobile] || null;
};

const rememberLoginMobile = (value: unknown) => {
  const mobile = normalizeMobileText(value);
  if (!mobile) {
    return;
  }
  const stored = wx.getStorageSync(StorageEnum.LOGIN_MOBILES);
  const list = Array.isArray(stored) ? stored.map(normalizeMobileText).filter(Boolean) : [];
  const next = [mobile, ...list.filter((item) => item !== mobile)].slice(0, 5);
  wx.setStorageSync(StorageEnum.LOGIN_MOBILES, next);
};

const readStoredProfile = (): IObject | null => {
  const cached = wx.getStorageSync(StorageEnum.USER_PROFILE);
  if (isPlainObject(cached)) {
    return cached as IObject;
  }
  return null;
};

const readCurrentUserSnapshot = (): CurrentUserSnapshot => {
  const profile = readStoredProfile();
  return {
    userId:
      normalizeUserId(wx.getStorageSync(StorageEnum.COSTOMER_ID)) ||
      extractUserIdFromProfile(profile),
    openId:
      normalizeOpenId(wx.getStorageSync(StorageEnum.OPEN_ID)) ||
      extractOpenIdFromProfile(profile),
    profile: hasProfileData(profile) ? profile : null,
  };
};

const buildStoredProfile = (
  profile: IObject | null,
  userId: number | null,
  openId: string | null
): IObject | null => {
  const merged: IObject = {};
  const currentProfile = readStoredProfile();
  const nextUserId = userId || extractUserIdFromProfile(profile);
  const nextOpenId = openId || extractOpenIdFromProfile(profile);
  const currentUserId = extractUserIdFromProfile(currentProfile);
  const currentOpenId = extractOpenIdFromProfile(currentProfile);
  const canReuseCurrentProfile =
    hasProfileData(currentProfile) &&
    ((!nextUserId && !nextOpenId) ||
      (Boolean(nextUserId) && Boolean(currentUserId) && nextUserId === currentUserId) ||
      (Boolean(nextOpenId) &&
        Boolean(currentOpenId) &&
        nextOpenId === currentOpenId));

  if (canReuseCurrentProfile && hasProfileData(currentProfile)) {
    Object.assign(merged, currentProfile);
  }
  if (hasProfileData(profile)) {
    Object.assign(merged, profile);
  }

  if (userId && !extractUserIdFromProfile(merged)) {
    merged.id = userId;
  }

  if (openId) {
    if (!normalizeOpenId(merged.openid)) {
      merged.openid = openId;
    }
    if (!normalizeOpenId(merged.openId)) {
      merged.openId = openId;
    }
  }

  return Object.keys(merged).length > 0 ? merged : null;
};

const syncCurrentUserStorage = (
  snapshot: CurrentUserSnapshot
): CurrentUserSnapshot => {
  const profile = buildStoredProfile(
    snapshot.profile,
    snapshot.userId,
    snapshot.openId
  );
  const normalized: CurrentUserSnapshot = {
    userId: snapshot.userId || extractUserIdFromProfile(profile),
    openId: snapshot.openId || extractOpenIdFromProfile(profile),
    profile,
  };

  wx.setStorageSync(StorageEnum.OPEN_ID, normalized.openId);
  wx.setStorageSync(StorageEnum.USER_PROFILE, normalized.profile);
  wx.setStorageSync(StorageEnum.COSTOMER_ID, normalized.userId);
  wx.setStorageSync(StorageEnum.IS_LOGIN, Boolean(normalized.userId));

  return normalized;
};

const buildLoginResponse = (
  status: number,
  snapshot: CurrentUserSnapshot
): LoginResponse => {
  return {
    status,
    profile: snapshot.profile,
    costomerId: snapshot.userId,
  };
};

const hasBoundMobile = (snapshot: CurrentUserSnapshot): boolean => {
  return Boolean(snapshot.userId && extractMobileFromProfile(snapshot.profile));
};

const requestLoginCode = (): Promise<string> => {
  return new Promise((resolve, reject) => {
    wx.login({
      success: (res) => {
        if (res.code) {
          resolve(res.code);
          return;
        }
        reject(res);
      },
      fail: reject,
    });
  });
};

const buildSaveUserPayload = (
  openId: string,
  profile: IObject | null
): IObject => {
  const nickname = String(
    (profile && (profile.nickname || profile.nickName)) || ''
  ).trim();
  const avatarUrl = String((profile && profile.avatarUrl) || '').trim();
  const phone = String(
    (profile && (profile.mobile || profile.phone)) || ''
  ).trim();

  return {
    openid: openId,
    openId,
    nickname,
    avatarUrl,
    phone,
  };
};

const resolveCurrentUserById = async (
  userId: number
): Promise<CurrentUserSnapshot | null> => {
  const profile = await getUserProfileSilently({ id: userId });
  if (!profile) {
    return null;
  }

  const snapshot = syncCurrentUserStorage({
    userId: normalizeUserId(profile.id) || userId,
    openId: extractOpenIdFromProfile(profile),
    profile,
  });

  return snapshot.userId ? snapshot : null;
};

const resolveCurrentUserByOpenId = async (
  openId: string
): Promise<CurrentUserSnapshot | null> => {
  const profile = await getUserProfileSilently({ openId });
  if (!profile) {
    return null;
  }

  const snapshot = syncCurrentUserStorage({
    userId: extractUserIdFromProfile(profile),
    openId: extractOpenIdFromProfile(profile) || openId,
    profile,
  });

  return snapshot.userId ? snapshot : null;
};

const ensureOpenId = async (): Promise<string | null> => {
  const currentSnapshot = readCurrentUserSnapshot();

  try {
    const code = await requestLoginCode();
    const openId = normalizeOpenId(await getOpenIdSilently(code));
    if (!openId) {
      console.error('ensureOpenId failed: empty openid from getOpenId');
      return null;
    }

    syncCurrentUserStorage({
      ...currentSnapshot,
      openId,
    });
    return openId;
  } catch (error) {
    console.error('ensureOpenId failed:', error);
  }

  return null;
};

const createOrReuseCurrentUser = async (
  openId: string
): Promise<CurrentUserSnapshot | null> => {
  const cachedProfile = readCurrentUserSnapshot().profile;
  const costomerId = await saveUserProfileSilently(
    buildSaveUserPayload(openId, cachedProfile)
  );
  const userId = normalizeUserId(costomerId);

  const profile =
    (userId ? await getUserProfileSilently({ id: userId }) : null) ||
    (await getUserProfileSilently({ openId })) ||
    buildStoredProfile(cachedProfile, userId, openId);

  const snapshot = syncCurrentUserStorage({
    userId: userId || extractUserIdFromProfile(profile),
    openId,
    profile,
  });

  return snapshot.userId ? snapshot : null;
};

const resolveCurrentUser = async (
  options: EnsureCurrentUserOptions
): Promise<LoginResponse> => {
  const fallbackSnapshot = readCurrentUserSnapshot();

  try {
    if (fallbackSnapshot.userId) {
      const resolvedById = await resolveCurrentUserById(fallbackSnapshot.userId);
      if (resolvedById && hasBoundMobile(resolvedById)) {
        return buildLoginResponse(0, resolvedById);
      }
    }

    const latestSnapshot = readCurrentUserSnapshot();
    if (latestSnapshot.openId) {
      const resolvedByOpenId = await resolveCurrentUserByOpenId(
        latestSnapshot.openId
      );
      if (resolvedByOpenId && hasBoundMobile(resolvedByOpenId)) {
        return buildLoginResponse(0, resolvedByOpenId);
      }
    }

    if (options.silentCreate !== true) {
      const currentSnapshot = readCurrentUserSnapshot();
      return buildLoginResponse(hasBoundMobile(currentSnapshot) ? 0 : 404, currentSnapshot);
    }

    const openId = await ensureOpenId();
    if (!openId) {
      const currentSnapshot = readCurrentUserSnapshot();
      return buildLoginResponse(hasBoundMobile(currentSnapshot) ? 0 : 500, currentSnapshot);
    }

    const createdSnapshot = await createOrReuseCurrentUser(openId);
    if (createdSnapshot) {
      return buildLoginResponse(hasBoundMobile(createdSnapshot) ? 0 : 404, createdSnapshot);
    }
  } catch (error) {
    console.error('resolveCurrentUser error:', error);
  }

  const currentSnapshot = readCurrentUserSnapshot();
  return buildLoginResponse(hasBoundMobile(currentSnapshot) ? 0 : 500, currentSnapshot);
};

export const clearLoginStorage = () => {
  wx.setStorageSync(StorageEnum.OPEN_ID, null);
  wx.setStorageSync(StorageEnum.USER_PROFILE, null);
  wx.setStorageSync(StorageEnum.COSTOMER_ID, null);
  wx.setStorageSync(StorageEnum.IS_LOGIN, false);
  ensureCurrentUserPromise = null;
};

export const ensureLoginStorage = () => {
  syncCurrentUserStorage(readCurrentUserSnapshot());
};

export const isLoggedIn = (): boolean => {
  return hasBoundMobile(readCurrentUserSnapshot());
};

export const getCachedUserId = (): number | null => {
  return readCurrentUserSnapshot().userId;
};

export const getCachedOpenId = (): string | null => {
  return readCurrentUserSnapshot().openId;
};

export const getCachedUserProfile = (): IObject | null => {
  return readCurrentUserSnapshot().profile;
};

export const getLoginMobileHistory = (): string[] => {
  const stored = wx.getStorageSync(StorageEnum.LOGIN_MOBILES);
  if (!Array.isArray(stored)) {
    return [];
  }
  return stored.map(normalizeMobileText).filter(Boolean).slice(0, 5);
};

export const requestMobileLoginCode = async (mobile: string): Promise<IObject | null> => {
  const normalizedMobile = normalizeMobileText(mobile);
  if (!normalizedMobile) {
    return null;
  }
  return sendLoginCode({ mobile: normalizedMobile });
};

export const ensureCurrentUser = async (
  options: EnsureCurrentUserOptions = {}
): Promise<LoginResponse> => {
  ensureLoginStorage();
  const currentSnapshot = readCurrentUserSnapshot();

  if (
    !options.forceRefresh &&
    hasBoundMobile(currentSnapshot) &&
    currentSnapshot.profile
  ) {
    return buildLoginResponse(0, currentSnapshot);
  }

  if (!ensureCurrentUserPromise) {
    ensureCurrentUserPromise = resolveCurrentUser(options).finally(() => {
      ensureCurrentUserPromise = null;
    });
  }

  return ensureCurrentUserPromise;
};

export const prepareCurrentUserLogin = async (): Promise<LoginResponse> => {
  ensureLoginStorage();
  const currentSnapshot = readCurrentUserSnapshot();

  if (currentSnapshot.userId) {
    const resolvedById = await resolveCurrentUserById(currentSnapshot.userId);
    if (resolvedById && hasBoundMobile(resolvedById)) {
      return buildLoginResponse(0, resolvedById);
    }
  }

  const openId = currentSnapshot.openId || (await ensureOpenId());
  if (!openId) {
    return buildLoginResponse(500, readCurrentUserSnapshot());
  }

  const resolvedByOpenId = await resolveCurrentUserByOpenId(openId);
  if (resolvedByOpenId && hasBoundMobile(resolvedByOpenId)) {
    return buildLoginResponse(0, resolvedByOpenId);
  }

  const snapshot = syncCurrentUserStorage({
    ...readCurrentUserSnapshot(),
    openId,
  });
  return buildLoginResponse(404, snapshot);
};

export const registerCurrentUser = async (
  profile: IObject
): Promise<LoginResponse> => {
  ensureCurrentUserPromise = null;
  ensureLoginStorage();

  const currentSnapshot = readCurrentUserSnapshot();
  const openId = currentSnapshot.openId || (await ensureOpenId());
  if (!openId) {
    return buildLoginResponse(500, readCurrentUserSnapshot());
  }

  syncCurrentUserStorage({
    userId: currentSnapshot.userId,
    openId,
    profile: buildStoredProfile(profile, currentSnapshot.userId, openId),
  });

  const createdSnapshot = await createOrReuseCurrentUser(openId);
  if (createdSnapshot) {
    return buildLoginResponse(0, createdSnapshot);
  }

  return buildLoginResponse(500, readCurrentUserSnapshot());
};

export const requireCurrentUser = async (): Promise<LoginResponse> => {
  const result = await ensureCurrentUser({ silentCreate: false });
  const snapshot = readCurrentUserSnapshot();
  if (result.status === 0 && normalizeUserId(result.costomerId) && hasBoundMobile(snapshot)) {
    return result;
  }
  throw new Error('current user is required');
};

export const loginCurrentUserWithPhoneCode = async (
  phoneCode: string,
  profile: IObject | null = null
): Promise<LoginResponse> => {
  void profile;
  const normalizedPhoneCode = String(phoneCode || '').trim();
  if (!normalizedPhoneCode) {
    return buildLoginResponse(500, readCurrentUserSnapshot());
  }

  ensureCurrentUserPromise = null;
  ensureLoginStorage();
  const currentSnapshot = readCurrentUserSnapshot();
  const loginCode = await requestLoginCode();
  const result = await phoneLogin({
    loginCode,
    phoneCode: normalizedPhoneCode,
    openId: currentSnapshot.openId || undefined,
  });

  if (!result || !result.userId) {
    return buildLoginResponse(500, readCurrentUserSnapshot());
  }

  const snapshot = syncCurrentUserStorage({
    userId: normalizeUserId(result.userId),
    openId: normalizeOpenId(result.openId) || currentSnapshot.openId,
    profile: buildStoredProfile(
      result.profile || {
        id: result.userId,
        openId: result.openId,
        openid: result.openId,
        mobile: result.mobile,
      },
      normalizeUserId(result.userId),
      normalizeOpenId(result.openId) || currentSnapshot.openId
    ),
  });
  rememberLoginMobile(result.mobile);

  return buildLoginResponse(hasBoundMobile(snapshot) ? 0 : 500, snapshot);
};

export const loginCurrentUserWithMobileCode = async (
  mobile: string,
  verifyCode: string,
  profile: IObject | null = null
): Promise<LoginResponse> => {
  void profile;
  const normalizedMobile = normalizeMobileText(mobile);
  const normalizedVerifyCode = String(verifyCode || '').trim();
  if (!normalizedMobile || !normalizedVerifyCode) {
    return buildLoginResponse(500, readCurrentUserSnapshot());
  }

  ensureCurrentUserPromise = null;
  ensureLoginStorage();
  const currentSnapshot = readCurrentUserSnapshot();
  const loginCode = await requestLoginCode();
  const result = await mobileCodeLogin({
    loginCode,
    mobile: normalizedMobile,
    verifyCode: normalizedVerifyCode,
    openId: currentSnapshot.openId || undefined,
  });

  if (!result || !result.userId) {
    return buildLoginResponse(500, readCurrentUserSnapshot());
  }

  const snapshot = syncCurrentUserStorage({
    userId: normalizeUserId(result.userId),
    openId: normalizeOpenId(result.openId) || currentSnapshot.openId,
    profile: buildStoredProfile(
      result.profile || {
        id: result.userId,
        openId: result.openId,
        openid: result.openId,
        mobile: result.mobile || normalizedMobile,
      },
      normalizeUserId(result.userId),
      normalizeOpenId(result.openId) || currentSnapshot.openId
    ),
  });
  rememberLoginMobile(result.mobile || normalizedMobile);

  return buildLoginResponse(hasBoundMobile(snapshot) ? 0 : 500, snapshot);
};

export const loginCurrentUserWithMobile = async (
  mobile: string,
  profile: IObject | null = null
): Promise<LoginResponse> => {
  void profile;
  const normalizedMobile = normalizeMobileText(mobile);
  if (!normalizedMobile) {
    return buildLoginResponse(500, readCurrentUserSnapshot());
  }

  ensureCurrentUserPromise = null;
  ensureLoginStorage();
  const currentSnapshot = readCurrentUserSnapshot();
  const mockOpenId = resolveMockMobileLoginOpenId(normalizedMobile);
  const loginCode = mockOpenId ? '' : await requestLoginCode();
  const result = await mobileLogin({
    loginCode,
    mobile: normalizedMobile,
    openId: mockOpenId || currentSnapshot.openId || undefined,
  });

  if (!result || !result.userId) {
    return buildLoginResponse(500, readCurrentUserSnapshot());
  }

  const snapshot = syncCurrentUserStorage({
    userId: normalizeUserId(result.userId),
    openId: normalizeOpenId(result.openId) || mockOpenId || currentSnapshot.openId,
    profile: buildStoredProfile(
      result.profile || {
        id: result.userId,
        openId: result.openId,
        openid: result.openId,
        mobile: result.mobile || normalizedMobile,
      },
      normalizeUserId(result.userId),
      normalizeOpenId(result.openId) || mockOpenId || currentSnapshot.openId
    ),
  });
  rememberLoginMobile(result.mobile || normalizedMobile);

  return buildLoginResponse(hasBoundMobile(snapshot) ? 0 : 500, snapshot);
};

export const wxLogin = async (force = false): Promise<LoginResponse> => {
  return ensureCurrentUser({ forceRefresh: force });
};
