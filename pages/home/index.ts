import { getMiniStoreList, getStoreBayStatus, getStoreList } from '../../apis/store';
import { submitFranchiseContact } from '../../apis/franchise';
import { buildStoreDetailScanUrl, parseWashScanResult } from '../../utils/scan';
import { getLocation } from '../../utils/location';
import { getCachedUserId, isLoggedIn } from '../../utils/user';

const TEXT_SCAN_INVALID = '二维码无效，请扫描门店或工位二维码';
const TEXT_SCAN_FAILED = '扫码失败，请重试';
const TEXT_OPEN_STORE_FAILED = '打开门店失败';
const DEFAULT_COVER_IMAGE = '/assets/images/car.png';

type LocationPoint = {
  latitude: number;
  longitude: number;
};

type NearbyStoreInfo = {
  id: number;
  name: string;
  statusText: string;
  statusType: string;
  showStatus: boolean;
  distanceText: string;
  priceText: string;
  addressText: string;
  totalBaysText: string;
  idleBaysText: string;
  usingBaysText: string;
  showStats: boolean;
  coverImage: string;
  latitude: number;
  longitude: number;
};

Page({
  data: {
    nearbyStore: null as NearbyStoreInfo | null,
    hasNearbyStore: false,
    showNearbyEmpty: true,
    franchiseModalVisible: false,
    franchiseSubmitting: false,
    franchiseForm: {
      contactName: '',
      contactPhone: '',
    },
    quickActions: [
      { key: 'storeRecharge', title: '门店充值', icon: '/assets/icons/home-store-recharge.png' },
      { key: 'wallet', title: '通用充值', icon: '/assets/icons/home-wallet.png' },
      { key: 'franchise', title: '加盟联系', icon: '/assets/icons/home-franchise.png' },
      { key: 'tutorial', title: '洗车教程', icon: '/assets/icons/home-tutorial.png' },
    ],
  },

  onLoad() {
    this.loadNearbyStore();
  },

  onShow() {
    const tabBar = (this as any).getTabBar && (this as any).getTabBar();
    if (tabBar && tabBar.setData) {
      tabBar.setData({ selectedPath: 'pages/home/index' });
    }
  },

  async loadNearbyStore() {
    try {
      const userLocation = await this.resolveUserLocation();
      const record = await this.fetchNearbyStoreRecord(userLocation);
      if (!record) {
        this.setData({ nearbyStore: null, hasNearbyStore: false, showNearbyEmpty: true });
        return;
      }
      const nearbyStore = this.mapNearbyStore(record, userLocation);
      this.setData({
        nearbyStore: nearbyStore.name ? nearbyStore : null,
        hasNearbyStore: Boolean(nearbyStore.name),
        showNearbyEmpty: !nearbyStore.name,
      });
    } catch (error) {
      this.setData({ nearbyStore: null, hasNearbyStore: false, showNearbyEmpty: true });
      console.warn('load nearby store failed:', error);
    }
  },

  async fetchNearbyStoreRecord(userLocation?: LocationPoint | null) {
    const userId = isLoggedIn() ? getCachedUserId() || undefined : undefined;
    try {
      const pageData = await getMiniStoreList(
        1,
        20,
        userId,
        userLocation ? userLocation.latitude : undefined,
        userLocation ? userLocation.longitude : undefined
      );
      const records = pageData && Array.isArray(pageData.records) ? pageData.records : [];
      if (records.length > 0) {
        return this.pickNearestStoreRecord(records as Record<string, any>[]);
      }
    } catch (miniError) {
      console.warn('mini store list failed, fallback to base store:', miniError);
    }

    const pageData = await getStoreList(1, 1);
    const records = pageData && Array.isArray(pageData.records) ? pageData.records : [];
    const firstStore = records.length > 0 ? this.pickNearestStoreRecord(records as Record<string, any>[]) : null;
    if (!firstStore) {
      return null;
    }

    const storeId = Number(firstStore.id || firstStore.storeId || 0);
    const bayStatus = storeId ? await getStoreBayStatus(storeId).catch(() => null) : null;
    const bayStatusRecord = Array.isArray(bayStatus) ? bayStatus[0] : bayStatus;
    return { ...firstStore, ...(bayStatusRecord || {}) };
  },

  pickNearestStoreRecord(records: Record<string, any>[]) {
    if (!records.length) {
      return null;
    }
    return records.slice().sort((a, b) => {
      const distanceA = Number(
        a.distanceKm !== undefined && a.distanceKm !== null ? a.distanceKm : Number.MAX_SAFE_INTEGER
      );
      const distanceB = Number(
        b.distanceKm !== undefined && b.distanceKm !== null ? b.distanceKm : Number.MAX_SAFE_INTEGER
      );
      return distanceA - distanceB;
    })[0];
  },

  mapNearbyStore(item: Record<string, any>, userLocation?: LocationPoint | null): NearbyStoreInfo {
    const bayStatusList = Array.isArray(item.bayStatusList || item.bays)
      ? ((item.bayStatusList || item.bays) as Record<string, any>[])
      : [];
    const inferredUsing = bayStatusList.filter((bay) => {
      const status = String(bay.status || bay.deviceStatus || '').toLowerCase();
      return status === 'using' || status === 'running';
    }).length;
    const inferredDisabled = bayStatusList.filter((bay) => {
      const status = String(bay.status || bay.deviceStatus || '').toLowerCase();
      return status === 'disabled' || status === 'offline' || status === 'stop';
    }).length;
    const totalBays = this.toNumber(item.totalBays, bayStatusList.length);
    const usingBays = this.toNumber(item.usingBays, inferredUsing);
    const disabledBays = this.toNumber(item.disabledBays, inferredDisabled);
    const idleBays = totalBays > 0 ? Math.max(totalBays - usingBays - disabledBays, 0) : 0;
    const statusType = totalBays > 0 ? (idleBays > 0 ? 'idle' : 'busy') : 'unknown';
    const name = String(item.name || item.storeName || '').trim();
    const addressText = String(item.address || item.storeAddress || item.subtitle || '').trim();
    const storeLocation = this.resolveStoreLocation(item, userLocation);

    return {
      id: Number(item.id || item.storeId || 0),
      name,
      statusText: statusType === 'idle' ? '有空位' : statusType === 'busy' ? '使用中' : '可洗车',
      statusType,
      showStatus: totalBays > 0,
      distanceText: this.resolveDistanceText(item, userLocation),
      priceText: this.resolvePricingRuleText(item.pricingRuleText),
      addressText,
      totalBaysText: totalBays > 0 ? String(totalBays) : '',
      idleBaysText: totalBays > 0 ? String(idleBays) : '',
      usingBaysText: totalBays > 0 ? String(usingBays) : '',
      showStats: totalBays > 0,
      coverImage: item.coverImage || item.image || DEFAULT_COVER_IMAGE,
      latitude: storeLocation.latitude,
      longitude: storeLocation.longitude,
    };
  },

  async resolveUserLocation(): Promise<LocationPoint | null> {
    try {
      const location = await getLocation();
      const latitude = Number((location && location.latitude) || 0);
      const longitude = Number((location && location.longitude) || 0);
      return latitude && longitude ? { latitude, longitude } : null;
    } catch (error) {
      console.warn('home get location failed:', error);
      return null;
    }
  },

  resolveStoreLocation(item: Record<string, any>, userLocation?: LocationPoint | null): LocationPoint {
    const latitude = this.toNumber(item.latitude || item.lat, 0);
    const longitude = this.toNumber(item.longitude || item.lng || item.lon, 0);
    if (latitude && longitude) {
      return { latitude, longitude };
    }
    if (userLocation && userLocation.latitude && userLocation.longitude) {
      return { latitude: userLocation.latitude, longitude: userLocation.longitude };
    }
    return { latitude: 0, longitude: 0 };
  },

  resolveDistanceText(item: Record<string, any>, userLocation?: LocationPoint | null) {
    const distance = item.distanceKm !== null && item.distanceKm !== undefined ? Number(item.distanceKm) : null;
    if (distance !== null && !Number.isNaN(distance)) {
      return `距离您约${distance.toFixed(1)}km`;
    }
    const storeLatitude = this.toNumber(item.latitude || item.lat, 0);
    const storeLongitude = this.toNumber(item.longitude || item.lng || item.lon, 0);
    if (userLocation && userLocation.latitude && userLocation.longitude && !storeLatitude && !storeLongitude) {
      return '距离您约0.0km';
    }
    return '';
  },

  resolvePricingRuleText(value: any) {
    const text = String(value || '').trim();
    return text || '6.00/10分钟';
  },

  toNumber(value: any, fallback = 0) {
    const num = Number(value);
    return Number.isNaN(num) ? fallback : num;
  },

  startWash() {
    const nearbyStore = this.data.nearbyStore as NearbyStoreInfo | null;
    if (nearbyStore && nearbyStore.id) {
      wx.navigateTo({ url: `/pages/store-detail/index?id=${nearbyStore.id}` });
      return;
    }
    this.goService();
  },

  handleNearbyNav() {
    const nearbyStore = this.data.nearbyStore as NearbyStoreInfo | null;
    if (!nearbyStore || !nearbyStore.latitude || !nearbyStore.longitude) {
      wx.showToast({ title: '暂无导航信息', icon: 'none' });
      return;
    }
    wx.openLocation({
      latitude: nearbyStore.latitude,
      longitude: nearbyStore.longitude,
      name: nearbyStore.name,
      address: nearbyStore.addressText,
      scale: 18,
    });
  },

  scan() {
    wx.scanCode({
      onlyFromCamera: false,
      success: (res) => {
        const target = parseWashScanResult(res.result);
        if (!target) {
          wx.showToast({ title: TEXT_SCAN_INVALID, icon: 'none' });
          return;
        }
        wx.navigateTo({
          url: buildStoreDetailScanUrl(target),
          fail: (error) => {
            console.error('open scanned store failed:', error);
            wx.showToast({ title: TEXT_OPEN_STORE_FAILED, icon: 'none' });
          },
        });
      },
      fail: (error) => {
        if (String((error && error.errMsg) || '').includes('cancel')) {
          return;
        }
        console.error('scanCode failed:', error);
        wx.showToast({ title: TEXT_SCAN_FAILED, icon: 'none' });
      },
    });
  },

  goService() {
    wx.switchTab({ url: '/pages/service/index' });
  },

  goVoucherRedeem() {
    wx.navigateTo({ url: '/pages/voucher-redeem/index' });
  },

  handleQuickAction(e: WechatMiniprogram.TouchEvent) {
    const { key } = e.currentTarget.dataset;

    if (key === 'storeRecharge') {
      const nearbyStore = this.data.nearbyStore as NearbyStoreInfo | null;
      const params = nearbyStore && nearbyStore.id ? `?storeId=${nearbyStore.id}` : '';
      wx.navigateTo({ url: `/pages/pay/index${params}` });
      return;
    }

    if (key === 'wallet') {
      wx.navigateTo({ url: '/pages/wallet/index' });
      return;
    }

    if (key === 'franchise') {
      this.openFranchiseModal();
      return;
    }

    if (key === 'tutorial') {
      wx.navigateTo({ url: '/pages/question/index' });
    }
  },

  openFranchiseModal() {
    this.setData({
      franchiseModalVisible: true,
    });
  },

  closeFranchiseModal() {
    if (this.data.franchiseSubmitting) {
      return;
    }
    this.setData({
      franchiseModalVisible: false,
    });
  },

  onFranchiseNameInput(e: WechatMiniprogram.Input) {
    const value = e && e.detail ? String(e.detail.value || '') : '';
    this.setData({
      'franchiseForm.contactName': value,
    });
  },

  onFranchisePhoneInput(e: WechatMiniprogram.Input) {
    const value = e && e.detail ? String(e.detail.value || '') : '';
    this.setData({
      'franchiseForm.contactPhone': value,
    });
  },

  normalizePhone(value: any) {
    let phone = String(value || '').replace(/[\s-]/g, '').trim();
    if (phone.indexOf('+86') === 0) {
      phone = phone.slice(3);
    }
    return phone;
  },

  noop() {},

  async submitFranchiseForm() {
    if (this.data.franchiseSubmitting) {
      return;
    }

    const contactName = String(this.data.franchiseForm.contactName || '').trim();
    const contactPhone = this.normalizePhone(this.data.franchiseForm.contactPhone);
    if (!contactName) {
      wx.showToast({ title: '请输入姓名', icon: 'none' });
      return;
    }
    if (!/^\d{5,20}$/.test(contactPhone)) {
      wx.showToast({ title: '请输入正确电话', icon: 'none' });
      return;
    }

    this.setData({ franchiseSubmitting: true });
    try {
      await submitFranchiseContact({
        contactName,
        contactPhone,
        source: 'home',
      });
      this.setData({
        franchiseModalVisible: false,
        franchiseForm: {
          contactName: '',
          contactPhone: '',
        },
      });
      wx.showToast({ title: '提交成功', icon: 'success' });
    } catch (error) {
      wx.showToast({ title: '提交失败，请稍后重试', icon: 'none' });
      console.error('submitFranchiseForm failed:', error);
    } finally {
      this.setData({ franchiseSubmitting: false });
    }
  },
});
