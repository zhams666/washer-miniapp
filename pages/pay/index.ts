import type { IObject } from 'typings/interface.d';
import { createRechargeOrder, getRechargeProducts } from '../../apis/wallet';
import { getMiniStoreList, getStoreList } from '../../apis/store';
import { getLocation } from '../../utils/location';
import { requireCurrentUser } from '../../utils/user';
const TEXT_PAYMENT_START_FAILED = '支付参数异常';

const TEXT_LOGIN_REQUIRED = '请先登录';
const TEXT_SELECT_AMOUNT = '请选择充值金额';
const TEXT_SELECT_STORE = '请选择充值门店';
const TEXT_RECHARGE_FAILED = '创建充值订单失败';
const TEXT_RECHARGE_RESULT_MISSING = '未返回有效的充值订单号';
const TEXT_START_WASH_RECHARGE_HINT =
  '已为你预选当前门店和推荐充值档位，充值成功后可返回继续洗车。';

let pageEnterSequence = 0;

type RechargePriceOption = {
  rechargeProductId: number;
  price: number;
  gift: number;
  value: number;
  title?: string;
};

type StoreOption = {
  id: number;
  name: string;
  label: string;
  distanceKm: number | null;
  distanceText: string;
};

type LocationPoint = {
  latitude: number;
  longitude: number;
};

Page({
  data: {
    userId: 0,
    pageTitle: '用户充值',
    entrySource: '',
    preferredStoreId: 0,
    returnStoreId: 0,
    returnBayId: 0,
    returnDeviceId: 0,
    requiredAmount: 0,
    sourceHint: '',
    selectIndex: 0,
    loading: false,
    storeOptions: [] as StoreOption[],
    storePickerRange: [] as string[],
    selectedStoreIndex: 0,
    storeId: 0,
    storeConfirmed: false,
    storeConfirmText: '',
    locatingStore: false,
    priceOps: [] as RechargePriceOption[],
  },

  onLoad(options: Record<string, string>) {
    this.applyEntryOptions(options || {});
    void this.enterPage();
  },

  onShow() {
    void this.enterPage();
  },

  async enterPage() {
    const currentSequence = pageEnterSequence + 1;
    pageEnterSequence = currentSequence;

    try {
      const userId = await this.requirePageUser();
      if (currentSequence !== pageEnterSequence) {
        return false;
      }

      this.syncCurrentUserId(userId);
      await this.loadStores(userId);
      return true;
    } catch (error) {
      if (currentSequence !== pageEnterSequence) {
        return false;
      }

      this.handleRequireCurrentUserError(error);
      console.error('enterPage error:', error);
      return false;
    }
  },

  normalizeUserId(value: unknown) {
    const userId = Number(value || 0);
    if (Number.isInteger(userId) && userId > 0) {
      return userId;
    }
    return 0;
  },

  normalizeAmount(value: unknown) {
    const amount = Number(value || 0);
    if (Number.isNaN(amount) || amount <= 0) {
      return 0;
    }
    return amount;
  },

  applyEntryOptions(options: Record<string, string>) {
    const entrySource = String(options.source || '').trim();
    const storeId = this.normalizeUserId(options.storeId);
    const returnStoreId = this.normalizeUserId(options.returnStoreId) || storeId;
    const returnBayId = this.normalizeUserId(options.returnBayId);
    const returnDeviceId = this.normalizeUserId(options.returnDeviceId);
    const requiredAmount = this.normalizeAmount(options.requiredAmount);

    this.setData({
      pageTitle: entrySource === 'startWash' ? '余额不足，去充值' : '用户充值',
      entrySource,
      preferredStoreId: storeId,
      returnStoreId,
      returnBayId,
      returnDeviceId,
      requiredAmount,
      sourceHint: entrySource === 'startWash' ? TEXT_START_WASH_RECHARGE_HINT : '',
      selectIndex: this.resolveRecommendedPriceIndex(requiredAmount),
    });
  },

  getCurrentUserId() {
    return this.normalizeUserId(this.data.userId);
  },

  syncCurrentUserId(userId: number) {
    const safeUserId = this.normalizeUserId(userId);
    if (this.getCurrentUserId() !== safeUserId) {
      this.setData({ userId: safeUserId });
    }
    return safeUserId;
  },

  async requirePageUser() {
    const result = await requireCurrentUser();
    const userId = this.normalizeUserId((result && result.costomerId) || null);

    if (!userId) {
      throw new Error('current user is required');
    }

    return userId;
  },

  handleRequireCurrentUserError(error: unknown) {
    this.setData({
      userId: 0,
      loading: false,
    });

    wx.showToast({
      title: this.resolveRequireCurrentUserErrorMessage(error),
      icon: 'none',
    });
  },

  resolveRequireCurrentUserErrorMessage(error: unknown) {
    const message = this.extractErrorMessage(error);
    if (!message || message.includes('current user is required')) {
      return TEXT_LOGIN_REQUIRED;
    }
    return message;
  },

  extractErrorMessage(error: unknown) {
    const safeError = error as Record<string, any>;
    const candidates = [
      safeError && safeError.msg,
      safeError && safeError.message,
      safeError && safeError.errMsg,
    ];

    for (let i = 0; i < candidates.length; i += 1) {
      const message = candidates[i];
      if (typeof message === 'string' && message.trim()) {
        return message.trim();
      }
    }

    return '';
  },

  selectPrice(e: IObject) {
    this.setData({
      selectIndex: e.currentTarget.dataset.index,
    });
  },

  resolveRecommendedPriceIndex(requiredAmount: number) {
    const priceOps = this.data.priceOps as RechargePriceOption[];
    const amount = this.normalizeAmount(requiredAmount);
    if (amount <= 0) {
      return 0;
    }

    const matchedIndex = priceOps.findIndex((item) => Number(item.value || 0) >= amount);
    return matchedIndex >= 0 ? matchedIndex : Math.max(0, priceOps.length - 1);
  },

  async loadStores(userId?: number) {
    this.setData({ locatingStore: true });
    try {
      const userLocation = await this.resolveUserLocation();
      const records = await this.loadStoreRecords(userId || this.getCurrentUserId(), userLocation);
      const storeOptions = records
        .map((item: Record<string, any>) => this.mapStoreOption(item, userLocation))
        .filter((item: StoreOption) => item.id > 0)
        .sort((a: StoreOption, b: StoreOption) => this.resolveSortDistance(a) - this.resolveSortDistance(b));
      const preferredStoreId = Number(this.data.preferredStoreId || 0);
      const matchedStoreIndex = storeOptions.findIndex((item) => item.id === preferredStoreId);
      const selectedStoreIndex = matchedStoreIndex >= 0 ? matchedStoreIndex : 0;
      const selectedStore = storeOptions[selectedStoreIndex];
      this.setData({
        storeOptions,
        storePickerRange: storeOptions.map((item) => item.label),
        selectedStoreIndex,
        storeId: selectedStore ? selectedStore.id : 0,
        storeConfirmed: false,
        storeConfirmText: this.resolveStoreConfirmText(selectedStore, false),
      });
      if (selectedStore && selectedStore.id) {
        await this.loadRechargeProducts(selectedStore.id);
        this.promptStoreConfirmation(selectedStore, false);
      }
    } catch (error) {
      console.error('loadStores error:', error);
    } finally {
      this.setData({ locatingStore: false });
    }
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
      console.warn('pay get location failed:', error);
      return null;
    }
  },

  async loadStoreRecords(userId: number, userLocation: LocationPoint | null) {
    try {
      const pageData = await getMiniStoreList(
        1,
        100,
        userId || undefined,
        userLocation ? userLocation.latitude : undefined,
        userLocation ? userLocation.longitude : undefined
      );
      const records = pageData && Array.isArray(pageData.records) ? pageData.records : [];
      if (records.length) {
        return records;
      }
    } catch (error) {
      console.warn('pay miniapp store list failed, fallback to admin store list:', error);
    }

    const fallbackPage = await getStoreList(1, 100);
    return fallbackPage && Array.isArray(fallbackPage.records) ? fallbackPage.records : [];
  },

  mapStoreOption(item: Record<string, any>, userLocation: LocationPoint | null): StoreOption {
    const id = Number(item.id || item.storeId || 0);
    const name = String(item.name || item.storeName || `门店${id || ''}`).trim();
    const distanceKm = this.resolveStoreDistanceKm(item, userLocation);
    const distanceText = this.resolveDistanceText(distanceKm);
    return {
      id,
      name,
      distanceKm,
      distanceText,
      label: distanceText ? `${name} · ${distanceText}` : name,
    };
  },

  resolveStoreDistanceKm(item: Record<string, any>, userLocation: LocationPoint | null) {
    const backendDistance =
      item.distanceKm !== undefined && item.distanceKm !== null ? Number(item.distanceKm) : null;
    if (backendDistance !== null && !Number.isNaN(backendDistance)) {
      return backendDistance;
    }
    const latitude = Number(item.latitude || item.lat || 0);
    const longitude = Number(item.longitude || item.lng || item.lon || 0);
    return this.calculateDistanceKm(userLocation, latitude, longitude);
  },

  resolveDistanceText(distanceKm: number | null) {
    if (distanceKm === null || Number.isNaN(Number(distanceKm))) {
      return '';
    }
    return `距您约${Number(distanceKm).toFixed(2)}km`;
  },

  calculateDistanceKm(userLocation: LocationPoint | null, latitude: number, longitude: number) {
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

  resolveSortDistance(store: StoreOption) {
    return store.distanceKm !== null && !Number.isNaN(Number(store.distanceKm))
      ? Number(store.distanceKm)
      : Number.MAX_SAFE_INTEGER;
  },

  resolveStoreConfirmText(store: StoreOption | undefined, confirmed: boolean) {
    if (!store) {
      return '';
    }
    const prefix = confirmed
      ? '已确认充值门店'
      : store.distanceText
      ? '已根据当前位置推荐充值门店'
      : '待确认充值门店';
    return `${prefix}：${store.name}${store.distanceText ? `（${store.distanceText}）` : ''}`;
  },

  promptStoreConfirmation(store: StoreOption, alreadyConfirmed: boolean, force = false) {
    if (!store || !store.id || alreadyConfirmed) {
      return;
    }
    const page = this as Record<string, any>;
    const promptKey = `${store.id}:${store.distanceText}`;
    if (!force && page._lastStoreConfirmPromptKey === promptKey) {
      return;
    }
    page._lastStoreConfirmPromptKey = promptKey;

    wx.showModal({
      title: '确认充值门店',
      content: `当前选择充值门店：${store.name}${store.distanceText ? `（${store.distanceText}）` : ''}。充值赠送余额仅限所选门店使用，请确认是否选择该门店充值。`,
      confirmText: '确认',
      cancelText: '手动选择',
      success: ({ confirm }) => {
        if (confirm) {
          this.setData({
            storeConfirmed: true,
            storeConfirmText: this.resolveStoreConfirmText(store, true),
          });
          return;
        }
        this.setData({
          storeConfirmed: false,
          storeConfirmText: '请在上方切换或确认充值门店后再充值',
        });
      },
    });
  },

  confirmRechargeBeforeSubmit(
    store: StoreOption | undefined,
    amount: number,
    giftAmount: number
  ): Promise<boolean> {
    if (!store || !store.id) {
      return Promise.resolve(false);
    }

    const giftText = giftAmount > 0 ? `\n赠送金额：¥${giftAmount.toFixed(2)}` : '';
    const distanceText = store.distanceText ? `（${store.distanceText}）` : '';
    return new Promise((resolve) => {
      wx.showModal({
        title: '请确认充值门店',
        content: `充值门店：${store.name}${distanceText}\n充值金额：¥${amount.toFixed(2)}${giftText}\n\n赠送余额仅限所选门店使用，请确认门店无误后再充值。`,
        confirmText: '确认充值',
        cancelText: '再看看',
        success: ({ confirm }) => {
          if (confirm) {
            this.setData({
              storeConfirmed: true,
              storeConfirmText: this.resolveStoreConfirmText(store, true),
            });
            resolve(true);
            return;
          }
          resolve(false);
        },
        fail: () => {
          resolve(false);
        },
      });
    });
  },

  async loadRechargeProducts(storeId: number) {
    if (!storeId) {
      this.setData({ priceOps: [], selectIndex: 0 });
      return;
    }
    try {
      const records = await getRechargeProducts(storeId);
      const priceOps = records
        .map((item: Record<string, any>) => this.mapRechargeProduct(item))
        .filter((item: RechargePriceOption) => item.rechargeProductId > 0 && item.value > 0);
      this.setData({
        priceOps,
        selectIndex: this.resolveRecommendedPriceIndexWithOptions(
          this.data.requiredAmount,
          priceOps
        ),
      });
    } catch (error) {
      console.error('loadRechargeProducts error:', error);
      this.setData({ priceOps: [], selectIndex: 0 });
    }
  },

  mapRechargeProduct(item: Record<string, any>): RechargePriceOption {
    const payAmountSource =
      item.payAmount !== undefined && item.payAmount !== null
        ? item.payAmount
        : item.price !== undefined && item.price !== null
        ? item.price
        : item.value !== undefined && item.value !== null
        ? item.value
        : 0;
    const giftSource =
      item.giftAmount !== undefined && item.giftAmount !== null
        ? item.giftAmount
        : item.gift !== undefined && item.gift !== null
        ? item.gift
        : 0;
    const payAmount = Number(payAmountSource);
    return {
      rechargeProductId: Number(item.rechargeProductId || item.planId || item.id || 0),
      price: payAmount,
      gift: Number(giftSource),
      value: payAmount,
      title: String(item.title || item.productName || ''),
    };
  },

  resolveRecommendedPriceIndexWithOptions(
    requiredAmount: number,
    priceOps: RechargePriceOption[]
  ) {
    const amount = this.normalizeAmount(requiredAmount);
    if (!priceOps.length || amount <= 0) {
      return 0;
    }
    const matchedIndex = priceOps.findIndex((item) => Number(item.value || 0) >= amount);
    return matchedIndex >= 0 ? matchedIndex : Math.max(0, priceOps.length - 1);
  },

  onStoreChange(e: WechatMiniprogram.PickerChange) {
    const index = Number(e.detail.value || 0);
    const storeOptions = this.data.storeOptions as StoreOption[];
    const selectedStore = storeOptions[index];
    this.setData({
      selectedStoreIndex: index,
      storeId: selectedStore ? selectedStore.id : 0,
      storeConfirmed: Boolean(selectedStore && selectedStore.id),
      storeConfirmText: this.resolveStoreConfirmText(selectedStore, Boolean(selectedStore && selectedStore.id)),
    });
    if (selectedStore && selectedStore.id) {
      void this.loadRechargeProducts(selectedStore.id);
    }
  },

  async handleRecharge() {
    let userId = this.getCurrentUserId();
    if (!userId) {
      const ready = await this.enterPage();
      if (!ready) {
        return;
      }

      userId = this.getCurrentUserId();
      if (!userId) {
        return;
      }
    }

    const priceOps = this.data.priceOps as RechargePriceOption[];
    const selected = priceOps[this.data.selectIndex] || priceOps[0];
    const amount = Number((selected && selected.value) || 0);
    const giftAmount = Number((selected && selected.gift) || 0);
    const rechargeProductId = Number((selected && selected.rechargeProductId) || 0);

    if (!rechargeProductId || !amount || Number.isNaN(amount)) {
      wx.showToast({
        title: TEXT_SELECT_AMOUNT,
        icon: 'none',
      });
      return;
    }

    if (!this.data.storeId && this.data.storePickerRange.length > 0) {
      wx.showToast({
        title: TEXT_SELECT_STORE,
        icon: 'none',
      });
      return;
    }

    const storeOptions = this.data.storeOptions as StoreOption[];
    const selectedStore = storeOptions[this.data.selectedStoreIndex];
    const confirmed = await this.confirmRechargeBeforeSubmit(
      selectedStore,
      amount,
      giftAmount
    );
    if (!confirmed) {
      return;
    }

    this.setData({ loading: true });
    try {
      console.info('recharge submit started', {
        storeId: this.data.storeId,
        rechargeProductId,
        amount,
      });
      const result = await createRechargeOrder({
        storeId: this.data.storeId || undefined,
        rechargeProductId,
      });

      const rechargeOrderNo = String((result && result.rechargeOrderNo) || '').trim();
      if (!rechargeOrderNo) {
        throw new Error(TEXT_RECHARGE_RESULT_MISSING);
      }
      console.info('recharge order created', {
        rechargeOrderNo,
        payStatus: result.payStatus,
      });

      const paymentResult = await this.requestWechatPayment(result);
      const principalAmount =
        result && result.principalAmount !== undefined && result.principalAmount !== null
          ? result.principalAmount
          : amount;
      const resultGiftAmount =
        result && result.giftAmount !== undefined && result.giftAmount !== null
          ? result.giftAmount
          : giftAmount;
      const query = [
        `rechargeOrderNo=${encodeURIComponent(rechargeOrderNo)}`,
        `principalAmount=${encodeURIComponent(String(principalAmount))}`,
        `giftAmount=${encodeURIComponent(String(resultGiftAmount))}`,
        `payStatus=${encodeURIComponent(String((result && result.payStatus) || ''))}`,
        `paymentResult=${encodeURIComponent(paymentResult)}`,
        this.data.entrySource ? `source=${encodeURIComponent(String(this.data.entrySource))}` : '',
        this.data.returnStoreId ? `returnStoreId=${encodeURIComponent(String(this.data.returnStoreId))}` : '',
        this.data.returnBayId ? `returnBayId=${encodeURIComponent(String(this.data.returnBayId))}` : '',
        this.data.returnDeviceId ? `returnDeviceId=${encodeURIComponent(String(this.data.returnDeviceId))}` : '',
      ]
        .filter(Boolean)
        .join('&');

      wx.navigateTo({
        url: `/pages/pay-result/index?${query}`,
      });
    } catch (error) {
      wx.showToast({
        title: this.extractErrorMessage(error) || TEXT_RECHARGE_FAILED,
        icon: 'none',
      });
      console.error('recharge submit failed:', {
        storeId: this.data.storeId,
        rechargeProductId,
        message: this.extractErrorMessage(error),
        error,
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  requestWechatPayment(result: Record<string, any>): Promise<string> {
    const payParams = result && result.payParams;
    if (!payParams) {
      return Promise.resolve('server');
    }

    const packageValue = payParams.package || payParams.packageValue;
    if (
      !payParams.timeStamp ||
      !payParams.nonceStr ||
      !packageValue ||
      !payParams.signType ||
      !payParams.paySign
    ) {
      return Promise.reject(new Error(TEXT_PAYMENT_START_FAILED));
    }

    return new Promise((resolve) => {
      wx.requestPayment({
        timeStamp: String(payParams.timeStamp),
        nonceStr: String(payParams.nonceStr),
        package: String(packageValue),
        signType: String(payParams.signType) as WechatMiniprogram.RequestPaymentOption['signType'],
        paySign: String(payParams.paySign),
        success: () => {
          resolve('success');
        },
        fail: (error) => {
          const message = this.extractErrorMessage(error);
          if (message.includes('cancel')) {
            resolve('cancel');
            return;
          }
          console.error('requestWechatPayment error:', error);
          resolve('fail');
        },
      });
    });
  },
});
