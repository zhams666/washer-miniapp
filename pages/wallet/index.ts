import { getWalletHistory, getWalletStoreBalances, getWalletSummary } from '../../apis/wallet';
import { requireCurrentUser } from '../../utils/user';

type WalletItem = {
  title: string;
  subtitle: string;
  createTime: string;
  amountPrefix: string;
  amountText: string;
  amountClass: string;
  payBadgeText: string;
  payBadgeClass: string;
  tags: HistoryTag[];
  detailLines: string[];
};

type HistoryTag = {
  text: string;
  style: string;
};

type GiftItem = {
  storeId: number;
  storeName: string;
  giftBalance: string;
};

type CurrentUser = {
  userId: number;
};

type LoadOptions = {
  showErrorToast?: boolean;
};

const TEXT_LOGIN_REQUIRED = '请先登录';
const TEXT_LOAD_FAILED = '加载失败';
const TEXT_BALANCE_DETAIL_FAILED = '余额明细加载失败';

let pageEnterSequence = 0;
let walletLoadSequence = 0;
let historyLoadSequence = 0;
let balanceDetailLoadSequence = 0;

Page({
  data: {
    userId: 0,
    walletNum: '0.00',
    principalBalance: '0.00',
    currentNav: 0,
    list: [] as WalletItem[],
    loading: false,
    showBalanceDetail: false,
    giftBalances: [] as GiftItem[],
    detailLoading: false,
  },

  onLoad() {
    void this.enterPage();
  },

  onShow() {
    void this.handlePageShow();
  },

  async handlePageShow() {
    const page = this as Record<string, any>;
    const shouldRefreshAfterRecharge = Boolean(page._shouldRefreshAfterRecharge);

    if (shouldRefreshAfterRecharge && this.data.currentNav !== 0) {
      this.setData({ currentNav: 0 });
    }

    const ready = await this.enterPage();
    if (!ready) {
      return;
    }

    if (!shouldRefreshAfterRecharge) {
      return;
    }

    page._shouldRefreshAfterRecharge = false;
    const userId = this.getCurrentUserId();
    if (userId) {
      await this.loadBalanceDetail(userId, {
        showErrorToast: false,
      });
    }
  },

  async enterPage() {
    const currentSequence = pageEnterSequence + 1;
    pageEnterSequence = currentSequence;

    try {
      const currentUser = await this.requirePageUser();
      if (currentSequence !== pageEnterSequence) {
        return false;
      }

      const previousUserId = this.getCurrentUserId();
      const nextUserId = this.syncCurrentUser(currentUser);

      if (previousUserId && previousUserId !== nextUserId) {
        this.resetWalletData();
      }

      await this.loadWallet(nextUserId, {
        showErrorToast: true,
      });
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

  getCurrentUserId() {
    return this.normalizeUserId(this.data.userId);
  },

  async requirePageUser(): Promise<CurrentUser> {
    const result = await requireCurrentUser();
    const userId = this.normalizeUserId((result && result.costomerId) || null);

    if (!userId) {
      throw new Error('current user is required');
    }

    return {
      userId,
    };
  },

  syncCurrentUser(currentUser: CurrentUser) {
    const userId = this.normalizeUserId(currentUser.userId);
    if (this.getCurrentUserId() !== userId) {
      this.setData({ userId });
    }
    return userId;
  },

  resetWalletData() {
    this.setData({
      walletNum: '0.00',
      principalBalance: '0.00',
      list: [],
      giftBalances: [],
    });
  },

  resetPageState() {
    this.setData({
      userId: 0,
      walletNum: '0.00',
      principalBalance: '0.00',
      list: [],
      loading: false,
      showBalanceDetail: false,
      giftBalances: [],
      detailLoading: false,
    });
  },

  handleRequireCurrentUserError(error: unknown) {
    this.resetPageState();
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

  async loadWallet(userId?: number, options: LoadOptions = {}) {
    const safeUserId = this.normalizeUserId(userId !== undefined && userId !== null ? userId : this.getCurrentUserId());
    if (!safeUserId) {
      return;
    }

    const currentSequence = walletLoadSequence + 1;
    walletLoadSequence = currentSequence;
    this.setData({ loading: true });

    try {
      const summary = await getWalletSummary(safeUserId);
      if (currentSequence !== walletLoadSequence) {
        return;
      }

      if (this.getCurrentUserId() !== safeUserId) {
        return;
      }

      const principal = Number((summary && summary.principalBalance) || 0);
      const gift = Number((summary && summary.giftBalance) || 0);
      const totalValue =
        summary && summary.totalBalance !== undefined && summary.totalBalance !== null
          ? summary.totalBalance
          : principal + gift;
      const total = Number(totalValue);
      const safePrincipal = Number.isNaN(principal) ? 0 : principal;
      const safeGift = Number.isNaN(gift) ? 0 : gift;
      const safeTotal = Number.isNaN(total) ? safePrincipal + safeGift : total;

      this.setData({
        walletNum: safeTotal.toFixed(2),
        principalBalance: safePrincipal.toFixed(2),
      });

      await this.loadHistory(safeUserId, {
        showErrorToast: false,
      });
      await this.loadBalanceDetail(safeUserId, {
        showErrorToast: false,
      });
    } catch (error) {
      if (currentSequence !== walletLoadSequence) {
        return;
      }

      if (this.getCurrentUserId() !== safeUserId) {
        return;
      }

      if (options.showErrorToast !== false) {
        wx.showToast({
          title: TEXT_LOAD_FAILED,
          icon: 'none',
        });
      }
      console.error('loadWallet error:', error);
    } finally {
      if (currentSequence === walletLoadSequence && this.getCurrentUserId() === safeUserId) {
        this.setData({ loading: false });
      }
    }
  },

  async loadHistory(userId?: number, options: LoadOptions = {}) {
    const safeUserId = this.normalizeUserId(userId !== undefined && userId !== null ? userId : this.getCurrentUserId());
    if (!safeUserId) {
      return;
    }

    const currentSequence = historyLoadSequence + 1;
    historyLoadSequence = currentSequence;

    try {
      const bizType = this.data.currentNav === 0 ? 'recharge' : 'consume';
      const data = await getWalletHistory(String(safeUserId), 20, 1, bizType);
      if (currentSequence !== historyLoadSequence) {
        return;
      }

      if (this.getCurrentUserId() !== safeUserId) {
        return;
      }

      const records = data && Array.isArray(data.records) ? data.records : [];
      const isConsume = this.data.currentNav === 1;
      this.setData({
        list: records.map((item: Record<string, any>) =>
          isConsume ? this.mapConsumeItem(item) : this.mapRechargeItem(item)
        ),
      });
    } catch (error) {
      if (currentSequence !== historyLoadSequence) {
        return;
      }

      if (this.getCurrentUserId() !== safeUserId) {
        return;
      }

      if (options.showErrorToast !== false) {
        wx.showToast({
          title: TEXT_LOAD_FAILED,
          icon: 'none',
        });
      }
      console.error('loadHistory error:', error);
    }
  },

  async loadBalanceDetail(userId?: number, options: LoadOptions = {}) {
    const safeUserId = this.normalizeUserId(userId !== undefined && userId !== null ? userId : this.getCurrentUserId());
    if (!safeUserId) {
      return;
    }

    const currentSequence = balanceDetailLoadSequence + 1;
    balanceDetailLoadSequence = currentSequence;
    this.setData({ detailLoading: true });

    try {
      const data = await getWalletStoreBalances(safeUserId);
      if (currentSequence !== balanceDetailLoadSequence) {
        return;
      }

      if (this.getCurrentUserId() !== safeUserId) {
        return;
      }

      const records = data && Array.isArray(data.records) ? data.records : [];
      const giftBalances = records.map((item: Record<string, any>) => ({
        storeId: Number(item.storeId || 0),
        storeName: item.storeName || `门店${item.storeId || ''}`,
        giftBalance: this.formatAmount(item.giftBalance),
      }));
      this.setData({ giftBalances });
    } catch (error) {
      if (currentSequence !== balanceDetailLoadSequence) {
        return;
      }

      if (this.getCurrentUserId() !== safeUserId) {
        return;
      }

      if (options.showErrorToast !== false) {
        wx.showToast({
          title: TEXT_BALANCE_DETAIL_FAILED,
          icon: 'none',
        });
      }
      console.error('loadBalanceDetail error:', error);
    } finally {
      if (
        currentSequence === balanceDetailLoadSequence &&
        this.getCurrentUserId() === safeUserId
      ) {
        this.setData({ detailLoading: false });
      }
    }
  },

  openBalanceDetail() {
    this.setData({ showBalanceDetail: true });
    void this.loadBalanceDetail();
  },

  closeBalanceDetail() {
    this.setData({ showBalanceDetail: false });
  },

  noop() {},

  mapRechargeItem(item: Record<string, any>): WalletItem {
    const isMembership = String(item.recordType || item.bizType || '').toLowerCase() === 'membership';
    if (isMembership) {
      return {
        title: '会员充值',
        subtitle: '会员权益开通',
        createTime: this.formatTime(item.createdAt),
        amountPrefix: '+',
        amountText: this.formatAmount(item.amount),
        amountClass: 'income',
        payBadgeText: '会员',
        payBadgeClass: 'recharge',
        tags: [],
        detailLines: [String(item.relatedOrderNo || item.remark || '').trim()].filter(Boolean),
      };
    }
    const storeName = String(item.storeName || '').trim();
    return {
      title: '充值',
      subtitle: storeName ? `${storeName} 钱包充值` : '钱包充值',
      createTime: this.formatTime(item.createdAt),
      amountPrefix: '+',
      amountText: this.formatAmount(item.amount),
      amountClass: 'income',
      payBadgeText: '充值',
      payBadgeClass: 'recharge',
      tags: [],
      detailLines: [],
    };
  },

  mapConsumeItem(item: Record<string, any>): WalletItem {
    if (this.isFineRecord(item)) {
      return this.mapFineItem(item);
    }

    const payMode = String(item.payMode || '').toLowerCase();
    const cardDeductTimes = this.toNumber(item.cardDeductTimes, 0);
    const isCardPay = payMode === 'card' || cardDeductTimes > 0;
    const hasDiscount = item.hasDiscount === true || item.hasDiscount === 'true';
    const storeName = String(item.storeName || '').trim();
    const orderNo = String(item.orderNo || item.relatedOrderNo || '').trim();
    const paymentParts = Array.isArray(item.paymentParts) ? item.paymentParts : [];
    const tags = this.buildConsumeTags(item, paymentParts, isCardPay, hasDiscount);
    const detailLines = this.buildConsumeDetailLines(item, paymentParts, isCardPay, cardDeductTimes);

    return {
      title: storeName ? `${storeName}洗车` : '洗车消费',
      subtitle: orderNo ? `订单号 ${orderNo}` : '洗车订单',
      createTime: this.formatTime(item.createdAt),
      amountPrefix: '-',
      amountText: isCardPay
        ? `${cardDeductTimes || 1}次`
        : this.formatAmount(item.paidAmount !== undefined && item.paidAmount !== null ? item.paidAmount : item.amount),
      amountClass: isCardPay ? 'card-pay' : 'expense',
      payBadgeText: String(item.payTypeText || (isCardPay ? '次卡支付' : '钱包支付')),
      payBadgeClass: isCardPay ? 'card' : 'wallet',
      tags,
      detailLines,
    };
  },

  isFineRecord(item: Record<string, any>) {
    const recordType = String(item.recordType || '').toLowerCase();
    const bizType = String(item.bizType || '').toLowerCase();
    const relatedAction = String(item.relatedAction || '').toLowerCase();
    return recordType === 'fine' || bizType === 'fine' || relatedAction === 'fine';
  },

  mapFineItem(item: Record<string, any>): WalletItem {
    const storeName = String(item.storeName || '').trim();
    const fineNo = String(item.bizActionNo || item.orderNo || item.relatedOrderNo || '').trim();
    const paymentParts = Array.isArray(item.paymentParts) ? item.paymentParts : [];
    const amountValue =
      item.amount !== undefined && item.amount !== null
        ? item.amount
        : item.paidAmount;

    return {
      title: storeName ? `${storeName}违规罚款` : '违规罚款',
      subtitle: fineNo ? `罚款单号 ${fineNo}` : '后台余额扣罚',
      createTime: this.formatTime(item.createdAt),
      amountPrefix: '-',
      amountText: this.formatAmount(amountValue),
      amountClass: 'fine',
      payBadgeText: '罚款扣款',
      payBadgeClass: 'fine',
      tags: this.buildFineTags(paymentParts),
      detailLines: this.buildFineDetailLines(item, paymentParts),
    };
  },

  buildFineTags(paymentParts: Record<string, any>[]): HistoryTag[] {
    const tags: HistoryTag[] = [
      {
        text: '罚款扣款',
        style: 'fine',
      },
    ];

    paymentParts.forEach((part) => {
      const label = String(part.label || '').trim();
      if (!label) {
        return;
      }
      tags.push({
        text: label,
        style: 'fine-soft',
      });
    });

    return tags;
  },

  buildFineDetailLines(item: Record<string, any>, paymentParts: Record<string, any>[]) {
    const lines: string[] = [];
    if (paymentParts.length > 0) {
      paymentParts.forEach((part) => {
        const label = String(part.label || '').trim();
        if (label) {
          lines.push(label);
        }
      });
    } else {
      lines.push(`余额扣罚 ${this.formatAmount(item.amount)} 元`);
    }

    const remark = String(item.remark || '').trim();
    if (remark) {
      lines.push(`备注：${remark}`);
    }
    return lines;
  },

  buildConsumeTags(
    item: Record<string, any>,
    paymentParts: Record<string, any>[],
    isCardPay: boolean,
    hasDiscount: boolean
  ): HistoryTag[] {
    const tags: HistoryTag[] = [];
    const payText = String(item.payTypeText || (isCardPay ? '次卡支付' : '钱包支付'));
    tags.push({
      text: payText,
      style: isCardPay ? 'card' : 'wallet',
    });

    paymentParts.forEach((part) => {
      const label = String(part.label || '').trim();
      if (!label) {
        return;
      }
      tags.push({
        text: label,
        style: String(part.sourceType || '') === 'card' ? 'card-soft' : 'wallet-soft',
      });
    });

    if (hasDiscount) {
      const discountText =
        String(item.discountText || '').trim() ||
        `优惠券 -${this.formatAmount(item.discountAmount)} 元`;
      tags.push({
        text: discountText,
        style: 'coupon',
      });
    }

    return tags;
  },

  buildConsumeDetailLines(
    item: Record<string, any>,
    paymentParts: Record<string, any>[],
    isCardPay: boolean,
    cardDeductTimes: number
  ) {
    const lines: string[] = [];
    const finalAmount = this.toAmount(item.finalAmount);
    const paidAmount = this.toAmount(item.paidAmount);
    const discountAmount = this.toAmount(item.discountAmount);

    if (isCardPay) {
      lines.push(`核销次卡 ${cardDeductTimes || 1} 次`);
      return lines;
    }

    if (paymentParts.length > 0) {
      paymentParts.forEach((part) => {
        const label = String(part.label || '').trim();
        if (label) {
          lines.push(label);
        }
      });
    } else {
      lines.push(`钱包实付 ${this.formatAmount(paidAmount)} 元`);
    }

    if (discountAmount > 0) {
      lines.push(`优惠券抵扣 ${this.formatAmount(discountAmount)} 元`);
    }
    if (finalAmount > 0 && Math.abs(finalAmount - paidAmount) > 0.000001) {
      lines.push(`优惠后应付 ${this.formatAmount(finalAmount)} 元`);
    }
    return lines;
  },

  formatAmount(value: any) {
    const num = Number(value || 0);
    if (Number.isNaN(num)) {
      return '0.00';
    }
    return num.toFixed(2);
  },

  toNumber(value: any, fallback = 0) {
    const num = Number(value);
    return Number.isNaN(num) ? fallback : num;
  },

  toAmount(value: any) {
    return this.toNumber(value, 0);
  },

  formatTime(value: any) {
    if (!value) {
      return '';
    }
    const text = String(value);
    return text.replace('T', ' ').slice(0, 19);
  },

  changeType() {
    const nextNav = this.data.currentNav == 0 ? 1 : 0;
    this.setData({
      currentNav: nextNav,
    });

    const userId = this.getCurrentUserId();
    if (userId) {
      void this.loadHistory(userId);
    }
  },

  async goPay() {
    const userId = this.getCurrentUserId();
    if (!userId) {
      const ready = await this.enterPage();
      if (!ready) {
        return;
      }
    }

    const page = this as Record<string, any>;
    page._shouldRefreshAfterRecharge = true;

    wx.navigateTo({
      url: '/pages/pay/index',
      fail: () => {
        page._shouldRefreshAfterRecharge = false;
      },
    });
  },
});
