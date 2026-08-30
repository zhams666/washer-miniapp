import { checkWashQueueLocation, joinWashQueue, startWashOrder } from '../../apis/order';
import { getMiniStoreDetail, getStoreBayStatus, getStoreDetail } from '../../apis/store';
import { getWalletStoreBalances } from '../../apis/wallet';
import { getLocation } from '../../utils/location';
import { ensureCurrentUser, requireCurrentUser } from '../../utils/user';

const WASH_PRICING_RULE_TEXT =
  '16.8\u5143 / 20\u5206\u949f\uff0c\u8d85\u51fa\u540e 0.78\u5143 / \u5206\u949f';
const TEXT_HINT = '\u63d0\u793a';
const TEXT_LOADING_FAILED = '\u52a0\u8f7d\u5931\u8d25';
const TEXT_EMPTY = '';
const TEXT_BAY_UNAVAILABLE = '\u5f53\u524d\u5de5\u4f4d\u4e0d\u53ef\u7528';
const TEXT_SELECT_BAY_FIRST =
  '\u8bf7\u5148\u9009\u62e9\u4e00\u4e2a\u7a7a\u95f2\u5de5\u4f4d\uff0c\u518d\u5f00\u59cb\u6d17\u8f66\u3002';
const TEXT_SELECT_IDLE_BAY_AGAIN =
  '\u5f53\u524d\u5de5\u4f4d\u4e0d\u53ef\u7528\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9\u7a7a\u95f2\u5de5\u4f4d\u3002';
const TEXT_BAY_INFO_INVALID =
  '\u5de5\u4f4d\u4fe1\u606f\u5df2\u5931\u6548\uff0c\u8bf7\u91cd\u65b0\u9009\u62e9\u5de5\u4f4d\u3002';
const TEXT_LOGIN_REQUIRED = '\u8bf7\u5148\u767b\u5f55';
const TEXT_LOGIN_REQUIRED_CONTENT =
  '\u5f00\u59cb\u6d17\u8f66\u524d\u9700\u8981\u5148\u767b\u5f55\u8d26\u53f7\u3002';
const TEXT_GO_LOGIN = '\u53bb\u767b\u5f55';
const TEXT_CANCEL = '\u53d6\u6d88';
const TEXT_BALANCE_NOT_ENOUGH = '\u4f59\u989d\u4e0d\u8db3';
const TEXT_GO_RECHARGE = '\u53bb\u5145\u503c';
const TEXT_GO_BUY_CARD = '\u53bb\u8d2d\u4e70';
const TEXT_STARTING = '\u542f\u52a8\u4e2d...';
const TEXT_START_FAILED =
  '\u5f00\u59cb\u6d17\u8f66\u5931\u8d25\uff0c\u8bf7\u7a0d\u540e\u91cd\u8bd5\u3002';
const TEXT_START_FAILED_NO_ORDER_ID =
  '\u8ba2\u5355\u521b\u5efa\u6210\u529f\uff0c\u4f46\u672a\u8fd4\u56de\u6709\u6548\u7684 orderId';
const TEXT_LOGIN_THEN_START =
  '\u8bf7\u5148\u767b\u5f55\u540e\u518d\u5f00\u59cb\u6d17\u8f66\u3002';
const TEXT_SELECT_ONE_IDLE_BAY =
  '\u8bf7\u5148\u9009\u62e9\u4e00\u4e2a\u7a7a\u95f2\u5de5\u4f4d\u3002';
const TEXT_CARD_NOT_AVAILABLE =
  '\u6682\u65e0\u672c\u5e97\u53ef\u7528\u6b21\u5361';
const TEXT_CARD_NOT_AVAILABLE_CONTENT =
  '\u9009\u62e9\u6b21\u5361\u652f\u4ed8\u524d\u9700\u8981\u5148\u8d2d\u4e70\u672c\u5e97\u6b21\u5361\u3002';
const TEXT_SCAN_TARGET_NOT_FOUND =
  '\u4e8c\u7ef4\u7801\u5bf9\u5e94\u7684\u5de5\u4f4d\u4e0d\u5b58\u5728\u6216\u5df2\u53d8\u66f4\uff0c\u8bf7\u9009\u62e9\u5176\u4ed6\u7a7a\u95f2\u5de5\u4f4d\u3002';

type PayMode = 'wallet' | 'card';

type BayItem = {
  id: number;
  bayId: number;
  deviceId: number;
  deviceCode: string;
  status: string;
  deviceStatus: string;
  name: string;
  tagText: string;
  stateText: string;
  stateClass: string;
  serviceText: string;
  timeText: string;
  canStart: boolean;
  unavailableReason: string;
  selected: boolean;
  actionText: string;
  actionClass: string;
};

type WalletLoadResult = {
  loaded: boolean;
  data: Record<string, any>;
};

type LocationPoint = {
  latitude: number;
  longitude: number;
};

Page({
  data: {
    storeId: 0,
    currentUserId: 0,
    loading: false,
    submitting: false,
    coverImage: '/assets/images/washing.png',
    name: '',
    address: '',
    hasAddress: false,
    phoneText: TEXT_EMPTY,
    hasPhone: false,
    distanceText: TEXT_EMPTY,
    hasDistance: false,
    latitude: 0,
    longitude: 0,
    currentLatitude: 0,
    currentLongitude: 0,
    pricingRuleText: WASH_PRICING_RULE_TEXT,
    pricingBadgeText: '',
    hasVipMonthlyCard: false,
    isMember: false,
    isMemberDay: false,
    memberDayDiscountApplied: false,
    principalBalance: '0.00',
    giftBalance: '0.00',
    totalBays: 0,
    usingBays: 0,
    selectedBayId: 0,
    selectedDeviceId: 0,
    selectedBayName: '',
    scannedBayId: 0,
    scannedDeviceId: 0,
    scannedDeviceCode: '',
    scanSelectionHandled: true,
    minimumStartAmount: 0,
    startAvailableBalance: 0,
    walletBalanceLoaded: false,
    hasAvailableCard: false,
    availableCardRemainingTimes: 0,
    availableCardNo: '',
    idleBays: 0,
    queueActive: false,
    queueAheadCount: 0,
    queuePosition: 0,
    queueNo: '',
    bays: [] as BayItem[],
  },

  onLoad(options: Record<string, string>) {
    const storeId = Number((options && options.id) || 0);
    if (!storeId) {
      wx.showModal({
        title: TEXT_HINT,
        content:
          '\u95e8\u5e97\u4e0d\u5b58\u5728\u6216\u53c2\u6570\u7f3a\u5931\u3002',
        showCancel: false,
      });
      return;
    }

    const scannedBayId = this.toNumber((options && (options.scannedBayId || options.bayId)) || 0);
    const scannedDeviceId = this.toNumber(
      (options && (options.scannedDeviceId || options.deviceId)) || 0
    );
    const scannedDeviceCode = String((options && options.deviceCode) || '').trim();
    const fromScan = options && options.from === 'scan';

    this.setData({
      storeId,
      selectedBayId: scannedBayId,
      selectedDeviceId: scannedDeviceId,
      scannedBayId,
      scannedDeviceId,
      scannedDeviceCode,
      scanSelectionHandled: !fromScan,
    });
    this.loadDetail();
  },

  onShow() {
    const page = this as Record<string, any>;
    if (this.data.queueActive) {
      void this.checkCurrentQueueLocation();
      this.startQueueLocationMonitor();
    }
    if (!page._shouldRefreshAfterRecharge && !page._shouldRefreshAfterCardPurchase) {
      return;
    }

    page._shouldRefreshAfterRecharge = false;
    page._shouldRefreshAfterCardPurchase = false;
    void this.loadDetail();
  },

  onHide() {
    this.stopQueueLocationMonitor();
  },

  onUnload() {
    this.stopQueueLocationMonitor();
  },

  normalizeUserId(value: unknown) {
    const parsed = Number(value);
    if (Number.isInteger(parsed) && parsed > 0) {
      return parsed;
    }
    return 0;
  },

  syncCurrentUserId(value: unknown) {
    const userId = this.normalizeUserId(value);
    if (Number(this.data.currentUserId || 0) !== userId) {
      this.setData({ currentUserId: userId });
    }
    return userId;
  },

  async ensurePageUser() {
    try {
      const result = await ensureCurrentUser();
      return this.syncCurrentUserId((result && result.costomerId) || null) || undefined;
    } catch (error) {
      this.syncCurrentUserId(0);
      console.error('ensurePageUser error:', error);
      return undefined;
    }
  },

  async requirePageUser() {
    const result = await requireCurrentUser();
    const userId = this.syncCurrentUserId((result && result.costomerId) || null);
    if (!userId) {
      throw new Error('current user is required');
    }
    return userId;
  },

  async loadDetail() {
    this.setData({ loading: true });
    const userId = await this.ensurePageUser();
    const userLocation = await this.resolveUserLocation();
    this.setData({
      currentLatitude: userLocation ? userLocation.latitude : 0,
      currentLongitude: userLocation ? userLocation.longitude : 0,
    });
    const walletPromise = this.loadWalletData(userId);

    try {
      let detail: Record<string, any>;
      try {
        detail = await getMiniStoreDetail(
          this.data.storeId,
          userId,
          userLocation ? userLocation.latitude : undefined,
          userLocation ? userLocation.longitude : undefined
        );
      } catch (miniError) {
        console.warn('miniapp-detail failed, fallback to base store + bay status:', miniError);
        detail = await this.loadFallbackDetail(userLocation);
      }

      const walletResult = await walletPromise;
      const mergedDetail = this.mergeDetailWithWallet(detail || {}, walletResult);
      this.applyDetail(mergedDetail, walletResult.loaded);
    } catch (error) {
      wx.showToast({
        title: TEXT_LOADING_FAILED,
        icon: 'none',
      });
      console.error('loadDetail error:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadWalletData(userId?: number): Promise<WalletLoadResult> {
    if (!userId) {
      return {
        loaded: false,
        data: {},
      };
    }

    try {
      const data = await getWalletStoreBalances(userId);
      return {
        loaded: true,
        data: data || {},
      };
    } catch (error) {
      console.warn('load wallet store balances failed:', error);
      return {
        loaded: false,
        data: {},
      };
    }
  },

  async loadFallbackDetail(userLocation?: LocationPoint | null) {
    const [storeDetail, bayStatusData] = await Promise.all([
      getStoreDetail(this.data.storeId),
      getStoreBayStatus(this.data.storeId).catch(() => []),
    ]);

    const statusList = Array.isArray(bayStatusData) ? bayStatusData : [];
    const status = statusList.length > 0 ? statusList[0] : {};

    return {
      ...storeDetail,
      ...status,
      distanceKm: this.calculateDistanceKm(
        userLocation,
        this.toNumber(storeDetail.latitude || storeDetail.lat, 0),
        this.toNumber(storeDetail.longitude || storeDetail.lng || storeDetail.lon, 0)
      ),
      id: this.data.storeId,
      name: storeDetail.name || storeDetail.storeName,
    };
  },

  mergeDetailWithWallet(detail: Record<string, any>, walletResult: WalletLoadResult) {
    if (!walletResult.loaded) {
      return {
        ...detail,
        totalPrincipalBalance: Number(detail.principalBalance || 0),
      };
    }

    const wallet = this.findWalletBalance(walletResult.data, this.data.storeId);
    const records = walletResult.data && Array.isArray(walletResult.data.records) ? walletResult.data.records : [];
    const totalPrincipalBalance = records.reduce((sum: number, item: Record<string, any>) => {
      return sum + this.toAmount(item.principalBalance);
    }, 0);
    const principalBalance =
      wallet && wallet.principalBalance !== undefined && wallet.principalBalance !== null
        ? wallet.principalBalance
        : detail.principalBalance;
    const giftBalance =
      wallet && wallet.giftBalance !== undefined && wallet.giftBalance !== null
        ? wallet.giftBalance
        : detail.giftBalance;

    return {
      ...detail,
      principalBalance,
      giftBalance,
      totalPrincipalBalance,
    };
  },

  applyDetail(detail: Record<string, any>, walletBalanceLoaded: boolean) {
    const baseBays = this.normalizeBays(detail.bayStatusList || detail.bays);
    const scanTarget = this.getScanTarget();
    const scanSelectionPending =
      !this.data.scanSelectionHandled &&
      Boolean(scanTarget.bayId || scanTarget.deviceId || scanTarget.deviceCode);
    const selectedBay = this.findBayByTarget(
      baseBays,
      Number(this.data.selectedBayId || scanTarget.bayId || 0),
      Number(this.data.selectedDeviceId || scanTarget.deviceId || 0),
      scanTarget.deviceCode
    );
    const activeSelectedBay = selectedBay && selectedBay.canStart ? selectedBay : null;
    const pricingRuleText = this.resolvePricingRuleText(detail.pricingRuleText);
    const principalBalanceSource =
      detail.principalBalance !== undefined && detail.principalBalance !== null
        ? detail.principalBalance
        : detail.principal_balance;
    const giftBalanceSource =
      detail.giftBalance !== undefined && detail.giftBalance !== null
        ? detail.giftBalance
        : detail.bonusBalance !== undefined && detail.bonusBalance !== null
        ? detail.bonusBalance
        : detail.gift_balance !== undefined && detail.gift_balance !== null
        ? detail.gift_balance
        : detail.bonus_balance;
    const principalBalance = this.formatAmount(principalBalanceSource);
    const giftBalance = this.formatAmount(giftBalanceSource);
    const totalPrincipalBalance = walletBalanceLoaded ? this.toAmount(detail.totalPrincipalBalance) : 0;
    const startAvailableBalance = walletBalanceLoaded
      ? totalPrincipalBalance + this.toAmount(giftBalance)
      : 0;
    const minimumStartAmount = this.resolveMinimumStartAmount(pricingRuleText);
    const hasAvailableCardSource =
      detail.hasAvailableCard !== undefined && detail.hasAvailableCard !== null
        ? detail.hasAvailableCard
        : detail.has_available_card;
    const hasAvailableCard = this.toBoolean(hasAvailableCardSource);
    const availableCardRemainingTimes = this.toNumber(
      detail.availableCardRemainingTimes !== undefined && detail.availableCardRemainingTimes !== null
        ? detail.availableCardRemainingTimes
        : detail.available_card_remaining_times,
      0
    );
    const bays = this.decorateBays(
      baseBays,
      activeSelectedBay ? activeSelectedBay.bayId : 0,
      activeSelectedBay ? activeSelectedBay.deviceId : 0
    );
    const usingCount = bays.filter((bay) => bay.status === 'using').length;
    const hasVipMonthlyCard = this.toBoolean(detail.hasVipMonthlyCard);
    const isMember = this.toBoolean(detail.isMember);
    const isMemberDay = this.toBoolean(detail.isMemberDay);
    const memberDayDiscountApplied = this.toBoolean(detail.memberDayDiscountApplied);
    const storeLocation = this.resolveStoreLocation(detail);
    const idleBays = this.toNumber(
      detail.idleBays !== undefined && detail.idleBays !== null ? detail.idleBays : bays.filter((bay) => bay.canStart).length,
      0
    );
    const queueInfo = detail.queueInfo || {};
    const queueActive = this.toBoolean(detail.queueActive !== undefined ? detail.queueActive : queueInfo.active);
    const queueAheadCount = this.toNumber(
      detail.queueAheadCount !== undefined ? detail.queueAheadCount : queueInfo.aheadCount,
      0
    );
    const queuePosition = this.toNumber(
      detail.queuePosition !== undefined ? detail.queuePosition : queueInfo.position,
      0
    );
    const pricingBadgeText = hasVipMonthlyCard
      ? 'VIP月卡价'
      : memberDayDiscountApplied
      ? '会员日价'
      : '';

    this.setData({
      coverImage: detail.coverImage || detail.image || '/assets/images/washing.png',
      name: detail.name || detail.storeName || '',
      address: String(detail.address || detail.storeAddress || '').trim(),
      hasAddress: Boolean(String(detail.address || detail.storeAddress || '').trim()),
      phoneText: String(detail.phone || detail.contactPhone || '').trim(),
      hasPhone: Boolean(String(detail.phone || detail.contactPhone || '').trim()),
      distanceText: this.resolveDistanceText(detail),
      hasDistance: Boolean(this.resolveDistanceText(detail)),
      latitude: storeLocation.latitude,
      longitude: storeLocation.longitude,
      pricingRuleText,
      pricingBadgeText,
      hasVipMonthlyCard,
      isMember,
      isMemberDay,
      memberDayDiscountApplied,
      principalBalance,
      giftBalance,
      totalBays: this.toNumber(detail.totalBays, bays.length),
      usingBays: this.toNumber(detail.usingBays, usingCount),
      idleBays,
      selectedBayId: activeSelectedBay ? activeSelectedBay.bayId : 0,
      selectedDeviceId: activeSelectedBay ? activeSelectedBay.deviceId : 0,
      selectedBayName: activeSelectedBay ? activeSelectedBay.name : '',
      minimumStartAmount,
      startAvailableBalance,
      walletBalanceLoaded,
      hasAvailableCard,
      availableCardRemainingTimes,
      availableCardNo: String(detail.availableCardNo || detail.available_card_no || ''),
      queueActive,
      queueAheadCount,
      queuePosition,
      queueNo: String(detail.queueNo || queueInfo.queueNo || ''),
      bays,
      scanSelectionHandled: scanSelectionPending ? true : this.data.scanSelectionHandled,
    });

    if (scanSelectionPending) {
      this.notifyScannedBaySelection(selectedBay, activeSelectedBay);
    }
    if (queueActive) {
      this.startQueueLocationMonitor();
      void this.checkCurrentQueueLocation();
    } else {
      this.stopQueueLocationMonitor();
    }
  },

  normalizeBays(value: any): BayItem[] {
    if (!Array.isArray(value)) {
      return [];
    }

    return value.map((item: Record<string, any>, index: number) => {
      const deviceStatus = this.normalizeStatusText(
        item.deviceStatus !== undefined && item.deviceStatus !== null ? item.deviceStatus : item.device_status
      );
      const rawCanStart =
        item.canStart !== undefined && item.canStart !== null ? item.canStart : item.can_start;
      const status = this.resolveBayStatus(
        this.normalizeStatusText(item.status),
        deviceStatus,
        rawCanStart
      );
      const canStart =
        rawCanStart === undefined || rawCanStart === null
          ? status === 'idle'
          : rawCanStart === true || rawCanStart === 'true';
      const usedMinutes = this.toNumber(
        item.usingMinutes !== undefined && item.usingMinutes !== null ? item.usingMinutes : item.usedMinutes,
        0
      );
      const rawName = item.bayName || item.name || `\u5de5\u4f4d${item.bayId || item.id || index + 1}`;
      const simpleName = this.resolveSimpleBayName(rawName, index);
      const isUsing = status === 'using';
      const unavailableReason =
        String(item.unavailableReason || item.unavailable_reason || '').trim() ||
        (canStart ? '' : this.resolveBayUnavailableReason(status));

      return {
        id: Number(item.bayId || item.id || item.deviceId || index + 1),
        bayId: Number(item.bayId || item.id || 0),
        deviceId: Number(item.deviceId || 0),
        deviceCode: String(item.deviceCode || item.device_code || '').trim(),
        status,
        deviceStatus,
        name: simpleName,
        tagText: '\u81ea\u52a9',
        stateText: this.resolveBayStateText(status),
        stateClass: this.resolveBayStateClass(status, canStart),
        serviceText: canStart ? '\u53ef\u5f00\u59cb\u6d17\u8f66' : unavailableReason,
        timeText: isUsing
          ? `\u6d17\u8f66\u65f6\u95f4\uff1a\u5df2\u6d17 ${usedMinutes} \u5206\u949f`
          : '',
        canStart,
        unavailableReason,
        selected: false,
        actionText: canStart ? '\u70b9\u51fb\u5f00\u95e8' : this.resolveBayActionText(status),
        actionClass: canStart ? '' : 'disabled',
      };
    });
  },

  decorateBays(bays: BayItem[], selectedBayId: number, selectedDeviceId: number) {
    return bays.map((bay) => {
      if (!bay.canStart) {
        return {
          ...bay,
          selected: false,
          actionText: this.resolveBayActionText(bay.status),
          actionClass: 'disabled',
          serviceText: bay.unavailableReason || this.resolveBayUnavailableReason(bay.status),
        };
      }

      const selected = this.isSameSelectedBay(bay, selectedBayId, selectedDeviceId);
      return {
        ...bay,
        selected,
        actionText: selected ? '\u5df2\u9009\u62e9' : '\u70b9\u51fb\u5f00\u95e8',
        actionClass: '',
        serviceText: selected
          ? `\u5df2\u9009\u62e9 ${bay.name}\uff0c\u70b9\u51fb\u4e0b\u65b9\u5f00\u59cb\u6d17\u8f66`
          : '\u53ef\u5f00\u59cb\u6d17\u8f66',
      };
    });
  },

  getScanTarget() {
    return {
      bayId: Number(this.data.scannedBayId || 0),
      deviceId: Number(this.data.scannedDeviceId || 0),
      deviceCode: String(this.data.scannedDeviceCode || '').trim(),
    };
  },

  findBayByTarget(bays: BayItem[], bayId: number, deviceId: number, deviceCode = '') {
    if (bayId && deviceId) {
      return bays.find((bay) => bay.bayId === bayId && bay.deviceId === deviceId) || null;
    }

    const matchedById = bays.find((bay) => this.isSameSelectedBay(bay, bayId, deviceId));
    if (matchedById) {
      return matchedById;
    }

    const normalizedDeviceCode = String(deviceCode || '').trim();
    if (!normalizedDeviceCode) {
      return null;
    }

    return bays.find((bay) => bay.deviceCode === normalizedDeviceCode) || null;
  },

  notifyScannedBaySelection(selectedBay: BayItem | null, activeSelectedBay: BayItem | null) {
    if (!selectedBay) {
      this.showPrompt(TEXT_SCAN_TARGET_NOT_FOUND);
      return;
    }

    if (!activeSelectedBay) {
      this.showPrompt(this.resolveBayUnavailablePrompt(selectedBay));
      return;
    }

    wx.showToast({
      title: `\u5df2\u5b9a\u4f4d${activeSelectedBay.name}`,
      icon: 'none',
    });
  },

  normalizeStatusText(value: any) {
    return String(value || '').trim().toLowerCase();
  },

  resolveBayStatus(status: string, deviceStatus: string, canStart: unknown) {
    if (status === 'using' || status === 'running') {
      return 'using';
    }

    if (['offline', 'fault', 'disabled', 'paused'].includes(status)) {
      return status;
    }

    if (['offline', 'fault', 'disabled', 'paused'].includes(deviceStatus)) {
      return deviceStatus;
    }

    if (deviceStatus === 'running') {
      return 'using';
    }

    if (canStart === false || canStart === 'false') {
      return 'unavailable';
    }

    return 'idle';
  },

  resolveBayStateText(status: string) {
    const statusMap: Record<string, string> = {
      using: '\u8fd0\u884c\u4e2d',
      offline: '\u79bb\u7ebf',
      fault: '\u6545\u969c',
      disabled: '\u505c\u7528',
      paused: '\u6682\u505c',
      unavailable: '\u4e0d\u53ef\u7528',
      idle: '\u7a7a\u95f2\u4e2d',
    };
    return statusMap[status] || statusMap.idle;
  },

  resolveBayStateClass(status: string, canStart: boolean) {
    if (canStart || status === 'idle') {
      return 'idle';
    }
    if (status === 'using') {
      return 'using';
    }
    return 'disabled';
  },

  resolveBayActionText(status: string) {
    const actionMap: Record<string, string> = {
      using: '\u8fd0\u884c\u4e2d',
      offline: '\u8bbe\u5907\u79bb\u7ebf',
      fault: '\u8bbe\u5907\u6545\u969c',
      disabled: '\u5df2\u505c\u7528',
      paused: '\u8bbe\u5907\u6682\u505c',
      unavailable: '\u4e0d\u53ef\u7528',
    };
    return actionMap[status] || TEXT_BAY_UNAVAILABLE;
  },

  resolveBayUnavailableReason(status: string) {
    const reasonMap: Record<string, string> = {
      using: '\u5de5\u4f4d\u6d17\u8f66\u4e2d',
      offline: '\u8bbe\u5907\u5df2\u79bb\u7ebf\uff0c\u6682\u65f6\u65e0\u6cd5\u5f00\u59cb\u6d17\u8f66',
      fault: '\u8bbe\u5907\u6545\u969c\uff0c\u8bf7\u9009\u62e9\u5176\u4ed6\u5de5\u4f4d',
      disabled: '\u8bbe\u5907\u5df2\u505c\u7528\uff0c\u8bf7\u9009\u62e9\u5176\u4ed6\u5de5\u4f4d',
      paused: '\u8bbe\u5907\u6682\u505c\u670d\u52a1\uff0c\u8bf7\u9009\u62e9\u5176\u4ed6\u5de5\u4f4d',
      unavailable: TEXT_BAY_UNAVAILABLE,
    };
    return reasonMap[status] || TEXT_BAY_UNAVAILABLE;
  },

  resolveBayUnavailablePrompt(bay: BayItem) {
    const reason = bay.unavailableReason || this.resolveBayUnavailableReason(bay.status);
    return `${bay.name}${reason ? `\uff1a${reason}` : ''}\u3002`;
  },

  isSameSelectedBay(bay: BayItem, selectedBayId: number, selectedDeviceId: number) {
    if (selectedBayId && bay.bayId === selectedBayId) {
      return true;
    }

    if (selectedDeviceId && bay.deviceId === selectedDeviceId) {
      return true;
    }

    return false;
  },

  resolveSimpleBayName(name: string, index: number) {
    const match = String(name || '').match(/(\d+)/);
    if (match) {
      return `${match[1]}\u53f7\u4f4d`;
    }
    return `${index + 1}\u53f7\u4f4d`;
  },

  resolvePricingRuleText(value: any) {
    const text = String(value || '').trim();
    if (
      !text ||
      text === '1\u5143/10\u5206\u949f\uff0c\u8d85\u65f60.5\u5143/\u5206\u949f' ||
      text === '1\u5143/10\u5206\u949f\uff0c\u8d85\u51fa\u540e0.5\u5143/\u5206\u949f'
    ) {
      return WASH_PRICING_RULE_TEXT;
    }
    return text;
  },

  resolveDistanceText(detail: Record<string, any>) {
    const distance =
      detail.distanceKm !== null && detail.distanceKm !== undefined ? Number(detail.distanceKm) : null;
    if (distance !== null && !Number.isNaN(distance)) {
      return `${distance.toFixed(2)}km`;
    }
    const storeLatitude = this.toNumber(detail.latitude || detail.lat, 0);
    const storeLongitude = this.toNumber(detail.longitude || detail.lng || detail.lon, 0);
    if (Number(this.data.currentLatitude || 0) && Number(this.data.currentLongitude || 0) && !storeLatitude && !storeLongitude) {
      return '0.00km';
    }
    return TEXT_EMPTY;
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
      console.warn('store detail get location failed:', error);
      return null;
    }
  },

  resolveStoreLocation(detail: Record<string, any>): LocationPoint {
    const latitude = this.toNumber(detail.latitude || detail.lat, 0);
    const longitude = this.toNumber(detail.longitude || detail.lng || detail.lon, 0);
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

  resolveMinimumStartAmount(pricingRuleText: string) {
    const match = String(pricingRuleText || '').match(/(\d+(?:\.\d+)?)/);
    if (!match) {
      return 0;
    }

    const amount = Number(match[1]);
    return Number.isNaN(amount) ? 0 : amount;
  },

  formatAmount(value: any) {
    const num = Number(value || 0);
    if (Number.isNaN(num)) {
      return '0.00';
    }
    return num.toFixed(2);
  },

  toAmount(value: any) {
    const num = Number(value || 0);
    return Number.isNaN(num) ? 0 : num;
  },

  findWalletBalance(walletData: Record<string, any>, storeId: number) {
    const records = walletData && Array.isArray(walletData.records) ? walletData.records : [];
    return records.find((item: Record<string, any>) => Number(item.storeId || 0) === storeId) || null;
  },

  toNumber(value: any, fallback = 0) {
    const num = Number(value);
    return Number.isNaN(num) ? fallback : num;
  },

  toBoolean(value: any) {
    return value === true || value === 'true' || value === 1 || value === '1';
  },

  handleRecharge() {
    this.goRechargePage(false);
  },

  goRechargePage(fromStartWash = false) {
    const page = this as Record<string, any>;
    page._shouldRefreshAfterRecharge = true;
    const minimumStartAmount = Number(this.data.minimumStartAmount || 0);
    const startAvailableBalance = Number(this.data.startAvailableBalance || 0);
    const requiredAmount = fromStartWash
      ? Math.max(0, minimumStartAmount - startAvailableBalance)
      : 0;
    const params = [
      `source=${fromStartWash ? 'startWash' : 'storeDetail'}`,
      this.data.storeId ? `storeId=${this.data.storeId}` : '',
      this.data.storeId ? `returnStoreId=${this.data.storeId}` : '',
      this.data.selectedBayId ? `returnBayId=${this.data.selectedBayId}` : '',
      this.data.selectedDeviceId ? `returnDeviceId=${this.data.selectedDeviceId}` : '',
      requiredAmount > 0 ? `requiredAmount=${requiredAmount.toFixed(2)}` : '',
      minimumStartAmount > 0 ? `minimumStartAmount=${minimumStartAmount.toFixed(2)}` : '',
    ]
      .filter(Boolean)
      .join('&');

    wx.navigateTo({
      url: `/pages/pay/index${params ? `?${params}` : ''}`,
      fail: () => {
        page._shouldRefreshAfterRecharge = false;
      },
    });
  },

  handleNav() {
    const latitude = Number(this.data.latitude || 0);
    const longitude = Number(this.data.longitude || 0);

    if (!latitude || !longitude) {
      wx.showToast({
        title: '暂无导航信息',
        icon: 'none',
      });
      return;
    }

    wx.openLocation({
      latitude,
      longitude,
      name: this.data.name,
      address: this.data.address,
      scale: 18,
    });
  },

  handlePhoneCall() {
    const phoneNumber = String(this.data.phoneText || '').trim();
    if (!phoneNumber) {
      wx.showToast({
        title: '暂无联系电话',
        icon: 'none',
      });
      return;
    }

    wx.makePhoneCall({
      phoneNumber,
    });
  },

  handleCard() {
    this.navigateToCardPurchase();
  },

  async handleStartWash() {
    if (this.data.submitting) {
      return;
    }

    let userId = 0;
    try {
      userId = await this.requirePageUser();
    } catch (error) {
      console.error('requirePageUser before start wash error:', error);
      this.showLoginPrompt();
      return;
    }

    const selectedBay = this.getSelectedBay();
    if (!selectedBay) {
      if (Number(this.data.idleBays || 0) <= 0) {
        await this.joinCurrentStoreQueue(userId);
        return;
      }
      this.showPrompt(TEXT_SELECT_BAY_FIRST);
      return;
    }

    if (!selectedBay.canStart) {
      this.clearSelectedBay();
      this.showPrompt(this.resolveBayUnavailablePrompt(selectedBay));
      return;
    }

    if (!selectedBay.bayId && !selectedBay.deviceId) {
      this.clearSelectedBay();
      this.showPrompt(TEXT_BAY_INFO_INVALID);
      return;
    }

    this.showPayModeSelector(userId, selectedBay);
  },

  async joinCurrentStoreQueue(userId: number) {
    if (!this.data.latitude || !this.data.longitude) {
      this.showPrompt('门店暂无定位，不能排队。');
      return;
    }
    const userLocation = await this.resolveUserLocation();
    const distanceKm = this.calculateDistanceKm(userLocation, this.data.latitude, this.data.longitude);
    if (distanceKm === null) {
      this.showPrompt('无法获取当前位置，不能排队。');
      return;
    }
    if (distanceKm > 0.1) {
      this.showPrompt('距门店超过100米，不能排队。');
      return;
    }

    try {
      this.setData({ submitting: true });
      wx.showLoading({ title: '排队中' });
      const queueInfo = await joinWashQueue({
        userId,
        storeId: this.data.storeId,
        userLat: userLocation ? userLocation.latitude : undefined,
        userLng: userLocation ? userLocation.longitude : undefined,
      });
      wx.hideLoading();
      this.applyQueueInfo(queueInfo);
      wx.showToast({
        title: `排队成功，前方${Number(queueInfo.aheadCount || 0)}人`,
        icon: 'none',
      });
    } catch (error) {
      wx.hideLoading();
      this.showPrompt('排队失败，请确认在门店100米内。');
      console.error('joinCurrentStoreQueue error:', error);
    } finally {
      this.setData({ submitting: false });
    }
  },

  async checkCurrentQueueLocation() {
    const userId = this.normalizeUserId(this.data.currentUserId);
    if (!userId || !this.data.queueActive) {
      return;
    }
    const userLocation = await this.resolveUserLocation();
    if (!userLocation) {
      return;
    }
    try {
      const queueInfo = await checkWashQueueLocation({
        userId,
        storeId: this.data.storeId,
        userLat: userLocation.latitude,
        userLng: userLocation.longitude,
      });
      this.applyQueueInfo(queueInfo);
      if (this.toBoolean(queueInfo.cancelled)) {
        wx.showToast({
          title: '已超过100米，排队已取消',
          icon: 'none',
        });
      }
    } catch (error) {
      console.warn('checkCurrentQueueLocation failed:', error);
    }
  },

  startQueueLocationMonitor() {
    const page = this as Record<string, any>;
    if (page._queueLocationTimer) {
      return;
    }
    page._queueLocationTimer = setInterval(() => {
      void this.checkCurrentQueueLocation();
    }, 30000);
  },

  stopQueueLocationMonitor() {
    const page = this as Record<string, any>;
    if (!page._queueLocationTimer) {
      return;
    }
    clearInterval(page._queueLocationTimer);
    page._queueLocationTimer = null;
  },

  applyQueueInfo(queueInfo: Record<string, any>) {
    const queueActive = this.toBoolean(queueInfo.active);
    if (!queueActive) {
      this.stopQueueLocationMonitor();
    } else {
      this.startQueueLocationMonitor();
    }
    this.setData({
      queueActive,
      queueAheadCount: queueActive ? this.toNumber(queueInfo.aheadCount, 0) : 0,
      queuePosition: queueActive ? this.toNumber(queueInfo.position, 0) : 0,
      queueNo: queueActive ? String(queueInfo.queueNo || '') : '',
    });
  },

  showPayModeSelector(userId: number, selectedBay: BayItem) {
    wx.showActionSheet({
      itemList: ['钱包支付', '次卡支付'],
      success: ({ tapIndex }) => {
        const payMode: PayMode = tapIndex === 1 ? 'card' : 'wallet';
        this.startWashWithPayMode(payMode, userId, selectedBay);
      },
      fail: (error) => {
        if (!String((error && error.errMsg) || '').includes('cancel')) {
          console.error('showPayModeSelector error:', error);
        }
      },
    });
  },

  startWashWithPayMode(payMode: PayMode, userId: number, selectedBay: BayItem) {
    if (payMode === 'card') {
      if (!this.hasAvailableCardForStart()) {
        this.showCardUnavailablePrompt();
        return;
      }
    } else if (!this.hasEnoughWalletBalanceForStart()) {
      this.showBalanceInsufficientPrompt();
      return;
    }

    this.createStartWashOrder(payMode, userId, selectedBay);
  },

  async createStartWashOrder(payMode: PayMode, userId: number, selectedBay: BayItem) {
    try {
      this.setData({ submitting: true });
      wx.showLoading({ title: TEXT_STARTING });

      const order = await startWashOrder({
        userId,
        storeId: this.data.storeId,
        bayId: selectedBay.bayId || undefined,
        deviceId: selectedBay.deviceId || undefined,
        payMode,
      });

      const orderId = this.resolveStartWashOrderId(order);
      if (!orderId) {
        throw new Error(TEXT_START_FAILED_NO_ORDER_ID);
      }

      wx.hideLoading();
      this.goToWashing(orderId, selectedBay);
    } catch (error) {
      wx.hideLoading();
      this.handleStartWashError(error, payMode);
      console.error('handleStartWash error:', error);
    } finally {
      this.setData({ submitting: false });
    }
  },

  handleBayStart(e: WechatMiniprogram.TouchEvent) {
    const { bayId, deviceId, canStart } = e.currentTarget.dataset as {
      bayId: number;
      deviceId: number;
      canStart: boolean | string;
    };

    const safeBayId = Number(bayId || 0);
    const safeDeviceId = Number(deviceId || 0);
    const targetBay = (this.data.bays as BayItem[]).find((bay) =>
      this.isSameSelectedBay(bay, safeBayId, safeDeviceId)
    );

    if (!targetBay || (!targetBay.bayId && !targetBay.deviceId)) {
      wx.showToast({
        title: TEXT_BAY_INFO_INVALID,
        icon: 'none',
      });
      return;
    }

    const canStartValue = canStart === true || canStart === 'true';
    if (!canStartValue || !targetBay.canStart) {
      wx.showToast({
        title: targetBay.unavailableReason || TEXT_BAY_UNAVAILABLE,
        icon: 'none',
      });
      return;
    }

    this.setSelectedBay(targetBay);
    wx.showToast({
      title: `\u5df2\u9009\u62e9${targetBay.name}`,
      icon: 'none',
    });
  },

  setSelectedBay(bay: BayItem) {
    this.setData({
      selectedBayId: bay.bayId,
      selectedDeviceId: bay.deviceId,
      selectedBayName: bay.name,
      bays: this.decorateBays(this.data.bays as BayItem[], bay.bayId, bay.deviceId),
    });
  },

  clearSelectedBay() {
    this.setData({
      selectedBayId: 0,
      selectedDeviceId: 0,
      selectedBayName: '',
      bays: this.decorateBays(this.data.bays as BayItem[], 0, 0),
    });
  },

  getSelectedBay() {
    const selectedBayId = Number(this.data.selectedBayId || 0);
    const selectedDeviceId = Number(this.data.selectedDeviceId || 0);
    const bays = this.data.bays as BayItem[];

    return bays.find((bay) => this.isSameSelectedBay(bay, selectedBayId, selectedDeviceId)) || null;
  },

  hasEnoughWalletBalanceForStart() {
    if (!this.data.walletBalanceLoaded) {
      return true;
    }

    const minimumStartAmount = Number(this.data.minimumStartAmount || 0);
    if (minimumStartAmount <= 0) {
      return true;
    }

    const startAvailableBalance = Number(this.data.startAvailableBalance || 0);
    return startAvailableBalance + 0.000001 >= minimumStartAmount;
  },

  hasAvailableCardForStart() {
    return this.data.hasAvailableCard && Number(this.data.availableCardRemainingTimes || 0) > 0;
  },

  showLoginPrompt() {
    wx.showModal({
      title: TEXT_LOGIN_REQUIRED,
      content: TEXT_LOGIN_REQUIRED_CONTENT,
      confirmText: TEXT_GO_LOGIN,
      cancelText: TEXT_CANCEL,
      success: ({ confirm }) => {
        if (!confirm) {
          return;
        }

        wx.switchTab({
          url: '/pages/mine/index',
        });
      },
    });
  },

  showBalanceInsufficientPrompt(message?: string) {
    const minimumStartAmount = Number(this.data.minimumStartAmount || 0);
    const startAvailableBalance = Number(this.data.startAvailableBalance || 0);
    const content =
      message ||
      (minimumStartAmount > 0
        ? `\u5f53\u524d\u53ef\u7528\u4f59\u989d ${startAvailableBalance.toFixed(
            2
          )} \u5143\uff0c\u81f3\u5c11\u9700\u8981 ${minimumStartAmount.toFixed(
            2
          )} \u5143\u624d\u53ef\u5f00\u59cb\u6d17\u8f66\u3002`
        : '\u5f53\u524d\u4f59\u989d\u4e0d\u8db3\uff0c\u65e0\u6cd5\u5f00\u59cb\u6d17\u8f66\u3002');

    wx.showModal({
      title: TEXT_BALANCE_NOT_ENOUGH,
      content,
      confirmText: TEXT_GO_RECHARGE,
      cancelText: TEXT_CANCEL,
      success: ({ confirm }) => {
        if (!confirm) {
          return;
        }

        this.goRechargePage(true);
      },
    });
  },

  showCardUnavailablePrompt() {
    wx.showModal({
      title: TEXT_CARD_NOT_AVAILABLE,
      content: TEXT_CARD_NOT_AVAILABLE_CONTENT,
      confirmText: TEXT_GO_BUY_CARD,
      cancelText: TEXT_CANCEL,
      success: ({ confirm }) => {
        if (!confirm) {
          return;
        }
        this.navigateToCardPurchase();
      },
    });
  },

  navigateToCardPurchase() {
    const page = this as Record<string, any>;
    page._shouldRefreshAfterCardPurchase = true;
    const params = [
      this.data.storeId ? `storeId=${this.data.storeId}` : '',
      this.data.name ? `storeName=${encodeURIComponent(this.data.name)}` : '',
    ]
      .filter(Boolean)
      .join('&');

    wx.navigateTo({
      url: `/pages/card-purchase/index${params ? `?${params}` : ''}`,
      fail: () => {
        page._shouldRefreshAfterCardPurchase = false;
      },
    });
  },

  showPrompt(content: string, title = TEXT_HINT) {
    wx.showModal({
      title,
      content,
      showCancel: false,
    });
  },

  handleStartWashError(error: any, payMode?: PayMode) {
    const rawMessage = this.extractErrorMessage(error);
    const message = this.resolveStartWashErrorMessage(rawMessage);

    if (this.isCardUnavailableErrorMessage(rawMessage)) {
      this.setData({
        hasAvailableCard: false,
        availableCardRemainingTimes: 0,
        availableCardNo: '',
      });
      void this.loadDetail();
      if (payMode === 'card') {
        this.showCardUnavailablePrompt();
        return;
      }
    }

    if (this.isBalanceErrorMessage(rawMessage)) {
      this.showBalanceInsufficientPrompt(message);
      return;
    }

    if (this.isBayUnavailableErrorMessage(rawMessage)) {
      this.clearSelectedBay();
      this.loadDetail();
    }

    this.showPrompt(message);
  },

  extractErrorMessage(error: any) {
    const candidates = [error && error.msg, error && error.message, error && error.errMsg];
    for (let i = 0; i < candidates.length; i += 1) {
      const item = candidates[i];
      if (typeof item === 'string' && item.trim()) {
        return item.trim();
      }
    }
    return '';
  },

  resolveStartWashOrderId(order: Record<string, any>) {
    const orderData = order && order.data;
    const candidates = [
      order && order.id,
      order && order.orderId,
      orderData && orderData.id,
      orderData && orderData.orderId,
    ];

    for (let i = 0; i < candidates.length; i += 1) {
      const orderId = Number(candidates[i] || 0);
      if (orderId) {
        return orderId;
      }
    }

    return 0;
  },

  resolveStartWashErrorMessage(rawMessage: string) {
    if (!rawMessage) {
      return TEXT_START_FAILED;
    }

    if (rawMessage.includes('userId is required') || rawMessage.includes(TEXT_LOGIN_REQUIRED)) {
      return TEXT_LOGIN_THEN_START;
    }

    if (
      rawMessage.includes('deviceId or bayId is required') ||
      rawMessage.includes('\u9009\u62e9\u5de5\u4f4d')
    ) {
      return TEXT_SELECT_ONE_IDLE_BAY;
    }

    if (
      rawMessage.includes('bay is using') ||
      rawMessage.includes('bay unavailable') ||
      rawMessage.includes('device is offline') ||
      rawMessage.includes('device is fault') ||
      rawMessage.includes('device is disabled') ||
      rawMessage.includes('device is running') ||
      rawMessage.includes('device is paused') ||
      rawMessage.includes('device is unavailable') ||
      rawMessage.includes('\u5de5\u4f4d\u4e0d\u53ef\u7528')
    ) {
      return TEXT_SELECT_IDLE_BAY_AGAIN;
    }

    if (
      rawMessage.includes('device not found') ||
      rawMessage.includes('device does not belong to store') ||
      rawMessage.includes('deviceId and bayId do not match')
    ) {
      return TEXT_BAY_INFO_INVALID;
    }

    if (
      rawMessage.includes('\u94b1\u5305\u4f59\u989d\u4e0d\u8db3') ||
      rawMessage.includes('\u4f59\u989d\u4e0d\u8db3') ||
      rawMessage.includes('wallet balance is not enough')
    ) {
      return rawMessage.includes('wallet balance is not enough')
        ? '\u4f59\u989d\u4e0d\u8db3\uff0c\u8bf7\u5148\u5145\u503c\u540e\u518d\u5f00\u59cb\u6d17\u8f66\u3002'
        : rawMessage;
    }

    if (this.isCardUnavailableErrorMessage(rawMessage)) {
      return '\u6b21\u5361\u6682\u4e0d\u53ef\u7528\uff0c\u8bf7\u5237\u65b0\u95e8\u5e97\u540e\u91cd\u8bd5\uff0c\u6216\u6539\u7528\u4f59\u989d\u652f\u4ed8\u3002';
    }

    return rawMessage;
  },

  isCardUnavailableErrorMessage(rawMessage: string) {
    return (
      rawMessage.includes('available user card not found') ||
      rawMessage.includes('user card remaining times is not enough')
    );
  },

  isBalanceErrorMessage(rawMessage: string) {
    return (
      rawMessage.includes('\u94b1\u5305\u4f59\u989d\u4e0d\u8db3') ||
      rawMessage.includes('\u4f59\u989d\u4e0d\u8db3') ||
      rawMessage.includes('wallet balance is not enough')
    );
  },

  isBayUnavailableErrorMessage(rawMessage: string) {
    return (
      rawMessage.includes('bay is using') ||
      rawMessage.includes('bay unavailable') ||
      rawMessage.includes('device is offline') ||
      rawMessage.includes('device is fault') ||
      rawMessage.includes('device is disabled') ||
      rawMessage.includes('device is running') ||
      rawMessage.includes('device is paused') ||
      rawMessage.includes('device is unavailable') ||
      rawMessage.includes('\u5de5\u4f4d\u4e0d\u53ef\u7528') ||
      rawMessage.includes('device not found') ||
      rawMessage.includes('device does not belong to store') ||
      rawMessage.includes('deviceId and bayId do not match')
    );
  },

  goToWashing(orderId: number, selectedBay: BayItem) {
    const params = [
      `orderId=${orderId}`,
      this.data.storeId ? `storeId=${this.data.storeId}` : '',
      selectedBay.deviceId ? `deviceId=${selectedBay.deviceId}` : '',
      selectedBay.bayId ? `bayId=${selectedBay.bayId}` : '',
    ]
      .filter(Boolean)
      .join('&');

    const url = `/pages/washing/index?${params}`;

    wx.redirectTo({
      url,
      fail: () => {
        wx.navigateTo({
          url,
          fail: () => {
            wx.reLaunch({ url });
          },
        });
      },
    });
  },
});
