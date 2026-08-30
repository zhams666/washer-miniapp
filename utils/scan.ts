export type WashScanTarget = {
  storeId: number;
  bayId?: number;
  deviceId?: number;
  deviceCode?: string;
  raw: string;
};

const NUMBER_KEYS: Record<string, string[]> = {
  storeId: ['storeId', 'store_id', 'sid'],
  bayId: ['bayId', 'bay_id', 'bid'],
  deviceId: ['deviceId', 'device_id', 'did'],
};

const DEVICE_CODE_KEYS = ['deviceCode', 'device_code', 'code'];

const normalizeNumber = (value: unknown) => {
  const parsed = Number(value);
  return Number.isInteger(parsed) && parsed > 0 ? parsed : 0;
};

const normalizeText = (value: unknown) => String(value || '').trim();

const decodeValue = (value: string) => {
  try {
    return decodeURIComponent(value.replace(/\+/g, ' '));
  } catch {
    return value;
  }
};

const parseQuery = (query: string) => {
  return query
    .replace(/^\?/, '')
    .split('&')
    .reduce((acc: Record<string, string>, pair) => {
      if (!pair) return acc;
      const [rawKey, ...rest] = pair.split('=');
      const key = decodeValue(rawKey || '').trim();
      if (!key) return acc;
      const value = decodeValue(rest.join('=') || '').trim();
      acc[key] = value;
      acc[key.toLowerCase()] = value;
      return acc;
    }, {});
};

const extractQuery = (raw: string) => {
  const questionIndex = raw.indexOf('?');
  if (questionIndex >= 0) {
    return raw.slice(questionIndex + 1).split('#')[0];
  }
  return raw.split('#')[0];
};

const readNumberField = (data: Record<string, any>, field: keyof typeof NUMBER_KEYS) => {
  const keys = NUMBER_KEYS[field];
  for (let i = 0; i < keys.length; i += 1) {
    const key = keys[i];
    const rawValue =
      data[key] !== undefined && data[key] !== null ? data[key] : data[key.toLowerCase()];
    const value = normalizeNumber(rawValue);
    if (value) return value;
  }
  return 0;
};

const readDeviceCode = (data: Record<string, any>) => {
  for (let i = 0; i < DEVICE_CODE_KEYS.length; i += 1) {
    const key = DEVICE_CODE_KEYS[i];
    const rawValue =
      data[key] !== undefined && data[key] !== null ? data[key] : data[key.toLowerCase()];
    const value = normalizeText(rawValue);
    if (value) return value;
  }
  return '';
};

const buildTarget = (data: Record<string, any>, raw: string): WashScanTarget | null => {
  const storeId = readNumberField(data, 'storeId');
  const bayId = readNumberField(data, 'bayId');
  const deviceId = readNumberField(data, 'deviceId');
  const deviceCode = readDeviceCode(data);

  if (!storeId) {
    return null;
  }

  return {
    storeId,
    bayId: bayId || undefined,
    deviceId: deviceId || undefined,
    deviceCode: deviceCode || undefined,
    raw,
  };
};

export const parseWashScanResult = (result?: string): WashScanTarget | null => {
  const raw = normalizeText(result);
  if (!raw) return null;

  if (raw.startsWith('{') && raw.endsWith('}')) {
    try {
      const parsed = JSON.parse(raw);
      if (parsed && typeof parsed === 'object') {
        return buildTarget(parsed, raw);
      }
    } catch {
      return null;
    }
  }

  if (raw.includes('=') || raw.includes('?')) {
    return buildTarget(parseQuery(extractQuery(raw)), raw);
  }

  return null;
};

export const buildStoreDetailScanUrl = (target: WashScanTarget) => {
  const params = [`id=${target.storeId}`, 'from=scan'];
  if (target.bayId) params.push(`scannedBayId=${target.bayId}`);
  if (target.deviceId) params.push(`scannedDeviceId=${target.deviceId}`);
  if (target.deviceCode) params.push(`deviceCode=${encodeURIComponent(target.deviceCode)}`);
  return `/pages/store-detail/index?${params.join('&')}`;
};
