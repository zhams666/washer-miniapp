import { joinWashQueue } from '../../apis/order';
import { getMiniStoreList, getStoreBayStatus, getStoreList } from '../../apis/store';
import { getLocation } from '../../utils/location';
import { getCachedUserId, isLoggedIn } from '../../utils/user';

type BayStatusItem = {
  status: string;
  usingMinutes: number;
};

type StoreCardItem = {
  id: number;
  name: string;
  coverImage: string;
  priceText: string;
  idleBaysText: string;
  usingBaysText: string;
  disabledBaysText: string;
  statusDots: string[];
  featureTags: string[];
  hasFeatureTags: boolean;
  distanceText: string;
  addressText: string;
  actionText: string;
  actionClass: string;
  canStart: boolean;
  queueActive: boolean;
  queueAheadCount: number;
  queuePosition: number;
  latitude: number;
  longitude: number;
};

type LocationPoint = {
  latitude: number;
  longitude: number;
};

Page({
  data: {
    stores: [] as StoreCardItem[],
    loading: false,
    showEmpty: false,
    currentLatitude: 0,
    currentLongitude: 0,
  },

  onShow() {
    const tabBar = (this as any).getTabBar && (this as any).getTabBar();
    if (tabBar && tabBar.setData) {
      tabBar.setData({ selectedPath: 'pages/service/index' });
    }
    this.loadStores();
  },

  async loadStores() {
    this.setData({ loading: true, showEmpty: false });
    const userId = isLoggedIn() ? getCachedUserId() || undefined : undefined;
    const userLocation = await this.resolveUserLocation();
    this.setData({
      currentLatitude: userLocation ? userLocation.latitude : 0,
      currentLongitude: userLocation ? userLocation.longitude : 0,
    });

    try {
      let records: Record<string, any>[] = [];
      try {
        records = await this.loadMiniStoreRecords(userId, userLocation);
      } catch (miniError) {
        console.warn('miniapp-list failed, fallback to base store + bay status:', miniError);
        records = await this.loadFallbackStoreRecords(userLocation);
      }

      const stores = records
        .map((item) => this.mapStoreItem(item))
        .filter((item) => item.id && item.name)
        .sort((a, b) => this.resolveSortDistance(a.distanceText) - this.resolveSortDistance(b.distanceText));

      this.setData({
        stores,
        loading: false,
        showEmpty: stores.length === 0,
      });
    } catch (error) {
      this.setData({
        loading: false,
        showEmpty: (this.data.stores as StoreCardItem[]).length === 0,
      });
      wx.showToast({
        title: '加载失败',
        icon: 'none',
      });
      console.error('loadStores error:', error);
    }
  },

  async loadMiniStoreRecords(userId?: number, userLocation?: LocationPoint | null) {
    const pageData = await getMiniStoreList(
      1,
      20,
      userId || undefined,
      userLocation ? userLocation.latitude : undefined,
      userLocation ? userLocation.longitude : undefined
    );
    return pageData && Array.isArray(pageData.records) ? pageData.records : [];
  },

  async loadFallbackStoreRecords(userLocation?: LocationPoint | null) {
    const [storePage, bayStatusData] = await Promise.all([
      getStoreList(1, 20),
      getStoreBayStatus().catch(() => []),
    ]);

    const stores = storePage && Array.isArray(storePage.records) ? storePage.records : [];
    const bayStatusMap = this.buildBayStatusMap(Array.isArray(bayStatusData) ? bayStatusData : []);

    return stores.map((store: Record<string, any>) => {
      const storeId = Number(store.id || store.storeId || 0);
      const status = bayStatusMap[storeId] || {};
      return {
        ...store,
        ...status,
        id: storeId,
        name: store.name || store.storeName,
        distanceKm: this.calculateDistanceKm(
          userLocation,
          this.toNumber(store.latitude || store.lat, 0),
          this.toNumber(store.longitude || store.lng || store.lon, 0)
        ),
      };
    });
  },

  buildBayStatusMap(records: Record<string, any>[]) {
    return records.reduce((acc: Record<number, Record<string, any>>, item) => {
      const storeId = Number(item.storeId || item.id || 0);
      if (!storeId) {
        return acc;
      }
      acc[storeId] = {
        totalBays: item.totalBays,
        usingBays: item.usingBays,
        bayStatusList: item.bayStatusList,
      };
      return acc;
    }, {});
  },

  mapStoreItem(item: Record<string, any>): StoreCardItem {
    const bayStatusList = this.normalizeBayStatusList(item.bayStatusList || item.bays);
    const usingCount = this.toNumber(
      item.usingBays,
      bayStatusList.filter((bay) => bay.status === 'using').length
    );
    const totalBays = this.toNumber(item.totalBays, bayStatusList.length);
    const disabledCount = this.toNumber(
      item.disabledBays,
      bayStatusList.filter((bay) =>
        ['disabled', 'offline', 'fault', 'paused', 'unavailable'].includes(bay.status)
      ).length
    );
    const idleCount = totalBays > 0 ? Math.max(totalBays - usingCount - disabledCount, 0) : 0;
    const storeLocation = this.resolveStoreLocation(item);
    const featureTags = this.normalizeFeatureTags(item, bayStatusList);
    const canStart = idleCount > 0;
    const queueInfo = item.queueInfo || {};
    const queueActive = this.toBoolean(item.queueActive !== undefined ? item.queueActive : queueInfo.active);
    const queueAheadCount = this.toNumber(
      item.queueAheadCount !== undefined ? item.queueAheadCount : queueInfo.aheadCount,
      0
    );

    return {
      id: Number(item.id || item.storeId || 0),
      name: String(item.name || item.storeName || '').trim(),
      coverImage: item.coverImage || item.image || '/assets/images/washing.png',
      priceText: this.resolvePricingRuleText(item.pricingRuleText),
      idleBaysText: String(idleCount),
      usingBaysText: String(usingCount),
      disabledBaysText: String(disabledCount),
      statusDots: this.buildStatusDots(idleCount, usingCount, disabledCount),
      featureTags,
      hasFeatureTags: featureTags.length > 0,
      distanceText: this.resolveDistanceText(item),
      addressText: String(item.address || item.storeAddress || item.subtitle || '').trim(),
      actionText: canStart ? '开始洗车' : queueActive ? `前方${queueAheadCount}人` : '排队洗车',
      actionClass: canStart ? 'start' : 'queue',
      canStart,
      queueActive,
      queueAheadCount,
      queuePosition: this.toNumber(item.queuePosition !== undefined ? item.queuePosition : queueInfo.position, 0),
      latitude: storeLocation.latitude,
      longitude: storeLocation.longitude,
    };
  },

  normalizeFeatureTags(item: Record<string, any>, bayStatusList: BayStatusItem[]) {
    const value = item.featureTags || item.tags || item.usageSummary;
    const tags: string[] = [];

    if (Array.isArray(value)) {
      value.forEach((text) => {
        const normalized = String(text || '').trim();
        if (normalized) {
          tags.push(normalized);
        }
      });
    } else if (typeof value === 'string') {
      value.split(/[,，;；\r\n]+/).forEach((text) => {
        const normalized = String(text || '').trim();
        if (normalized) {
          tags.push(normalized);
        }
      });
    }

    if (tags.length === 0) {
      tags.push('地下车库', '避开烈日', '风扇降温', '停车免费', '24小时');
    }

    const usingBays = bayStatusList.filter((bay) => bay.status === 'using');
    usingBays.slice(0, 2).forEach((bay) => {
      tags.push(`已洗${bay.usingMinutes}分钟`);
    });

    return tags.slice(0, 7);
  },

  buildStatusDots(idleCount: number, usingCount: number, disabledCount: number) {
    const dots: string[] = [];
    for (let i = 0; i < Math.min(idleCount, 3); i += 1) {
      dots.push('idle');
    }
    for (let i = 0; i < Math.min(usingCount, 2); i += 1) {
      dots.push('busy');
    }
    for (let i = 0; i < Math.min(disabledCount, 2); i += 1) {
      dots.push('disabled');
    }
    return dots.length > 0 ? dots : ['disabled'];
  },

  normalizeBayStatusList(value: any): BayStatusItem[] {
    if (!Array.isArray(value)) {
      return [];
    }
    return value.map((item: Record<string, any>) => {
      const usingMinutes = this.toNumber(
        item.usingMinutes !== undefined && item.usingMinutes !== null ? item.usingMinutes : item.usedMinutes,
        0
      );
      const rawStatus = String(item.status || '').trim().toLowerCase();
      const deviceStatus = String(item.deviceStatus || item.device_status || '').trim().toLowerCase();
      const status =
        rawStatus === 'using' || rawStatus === 'running' || deviceStatus === 'running'
          ? 'using'
          : ['offline', 'fault', 'disabled', 'paused', 'unavailable'].includes(rawStatus)
          ? rawStatus
          : ['offline', 'fault', 'disabled', 'paused', 'unavailable'].includes(deviceStatus)
          ? deviceStatus
          : 'idle';
      return {
        status,
        usingMinutes,
      };
    });
  },

  async resolveUserLocation(): Promise<LocationPoint | null> {
    try {
      const location = await getLocation();
      const latitude = Number((location && location.latitude) || 0);
      const longitude = Number((location && location.longitude) || 0);
      if (!latitude || !longitude) {
        return null;
      }
      return { latitude, longitude };
    } catch (error) {
      console.warn('service get location failed:', error);
      return null;
    }
  },

  resolveStoreLocation(item: Record<string, any>): LocationPoint {
    const latitude = this.toNumber(item.latitude || item.lat, 0);
    const longitude = this.toNumber(item.longitude || item.lng || item.lon, 0);
    if (latitude && longitude) {
      return { latitude, longitude };
    }
    const mockLatitude = Number(this.data.currentLatitude || 0);
    const mockLongitude = Number(this.data.currentLongitude || 0);
    if (mockLatitude && mockLongitude) {
      return {
        latitude: mockLatitude,
        longitude: mockLongitude,
      };
    }
    return {
      latitude: 0,
      longitude: 0,
    };
  },

  calculateDistanceKm(userLocation: LocationPoint | null | undefined, latitude: number, longitude: number) {
    if (!userLocation || !userLocation.latitude || !userLocation.longitude || !latitude || !longitude) {
      return null;
    }
    const earthRadiusKm = 6371;
    const dLat = this.toRadians(latitude - userLocation.latitude);
    const dLng = this.toRadians(longitude - userLocation.longitude);
    const lat1 = this.toRadians(userLocation.latitude);
    const lat2 = this.toRadians(latitude);
    const a =
      Math.sin(dLat / 2) * Math.sin(dLat / 2) +
      Math.cos(lat1) * Math.cos(lat2) * Math.sin(dLng / 2) * Math.sin(dLng / 2);
    const c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    return Math.round(earthRadiusKm * c * 100) / 100;
  },

  toRadians(value: number) {
    return (value * Math.PI) / 180;
  },

  resolveSortDistance(distanceText: string) {
    const match = String(distanceText || '').match(/(\d+(?:\.\d+)?)/);
    if (!match) {
      return Number.MAX_SAFE_INTEGER;
    }
    return Number(match[1]);
  },

  resolvePricingRuleText(value: any) {
    const text = String(value || '').trim();
    return text ? text.replace(/\s+/g, '') : '6.00/10分钟（超出后0.5/分钟）';
  },

  resolveDistanceText(item: Record<string, any>) {
    const distance = item.distanceKm !== null && item.distanceKm !== undefined ? Number(item.distanceKm) : null;
    if (distance !== null && !Number.isNaN(distance)) {
      return `距离您${distance.toFixed(2)}km`;
    }
    const storeLatitude = this.toNumber(item.latitude || item.lat, 0);
    const storeLongitude = this.toNumber(item.longitude || item.lng || item.lon, 0);
    if (Number(this.data.currentLatitude || 0) && Number(this.data.currentLongitude || 0) && !storeLatitude && !storeLongitude) {
      return '距离您0.00km';
    }
    return '';
  },

  toNumber(value: any, fallback = 0) {
    const num = Number(value);
    return Number.isNaN(num) ? fallback : num;
  },

  toBoolean(value: any) {
    return value === true || value === 'true' || value === 1 || value === '1';
  },

  goDetail(e: WechatMiniprogram.TouchEvent) {
    const { id } = e.currentTarget.dataset as { id: number };
    if (!id) {
      return;
    }
    wx.navigateTo({
      url: `/pages/store-detail/index?id=${id}`,
    });
  },

  async handleStoreAction(e: WechatMiniprogram.TouchEvent) {
    const { id } = e.currentTarget.dataset as { id: number };
    const store = (this.data.stores as StoreCardItem[]).find((item) => Number(item.id) === Number(id));
    if (!store) {
      return;
    }
    if (store.canStart) {
      wx.navigateTo({
        url: `/pages/store-detail/index?id=${store.id}`,
      });
      return;
    }
    await this.joinQueueForStore(store);
  },

  async joinQueueForStore(store: StoreCardItem) {
    if (!isLoggedIn()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none',
      });
      wx.switchTab({ url: '/pages/mine/index' });
      return;
    }
    if (!store.latitude || !store.longitude) {
      wx.showToast({
        title: '门店暂无定位，不能排队',
        icon: 'none',
      });
      return;
    }

    const userLocation = await this.resolveUserLocation();
    const distanceKm = this.calculateDistanceKm(userLocation, store.latitude, store.longitude);
    if (distanceKm === null) {
      wx.showToast({
        title: '无法获取当前位置',
        icon: 'none',
      });
      return;
    }
    if (distanceKm > 0.1) {
      wx.showToast({
        title: '距门店超过100米，不能排队',
        icon: 'none',
      });
      return;
    }

    try {
      wx.showLoading({ title: '排队中' });
      const queueInfo = await joinWashQueue({
        userId: getCachedUserId(),
        storeId: store.id,
        userLat: userLocation ? userLocation.latitude : undefined,
        userLng: userLocation ? userLocation.longitude : undefined,
      });
      wx.hideLoading();
      wx.showToast({
        title: `排队成功，前方${Number(queueInfo.aheadCount || 0)}人`,
        icon: 'none',
      });
      void this.loadStores();
    } catch (error) {
      wx.hideLoading();
      wx.showToast({
        title: '排队失败，请确认在100米内',
        icon: 'none',
      });
      console.error('join queue failed:', error);
    }
  },
});
