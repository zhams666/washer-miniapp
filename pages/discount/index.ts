import { getMyCards } from '../../apis/card';
import { requireCurrentUser } from '../../utils/user';

type CardItem = {
  id: number;
  cardNo: string;
  storeId: number;
  storeName: string;
  sourceText: string;
  statusText: string;
  statusClass: string;
  totalTimes: number;
  usedTimes: number;
  remainingTimes: number;
  timesText: string;
  timesLabel: string;
  expireText: string;
  externalOrderNo: string;
  available: boolean;
  isMonthlyCard: boolean;
};

Page({
  data: {
    loading: false,
    list: [] as CardItem[],
    availableCount: 0,
    showEmpty: false,
  },

  onShow() {
    void this.loadCards();
  },

  async loadCards() {
    this.setData({ loading: true, showEmpty: false });
    try {
      const user = await requireCurrentUser();
      const userId = Number((user && user.costomerId) || 0);
      if (!userId) {
        throw new Error('userId is required');
      }

      const records = await getMyCards(userId);
      const list = records.map((item) => this.mapCardItem(item));
      const availableCount = list.filter((item) => item.available && !item.isMonthlyCard).length;
      this.setData({
        list,
        availableCount,
        showEmpty: list.length === 0,
      });
    } catch (error) {
      console.error('loadCards error:', error);
      this.setData({ showEmpty: (this.data.list as CardItem[]).length === 0 });
      wx.showToast({
        title: '卡券加载失败',
        icon: 'none',
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  mapCardItem(item: Record<string, any>): CardItem {
    const totalTimes = this.toNumber(item.totalTimes, 0);
    const usedTimes = this.toNumber(item.usedTimes, Math.max(0, totalTimes - this.toNumber(item.remainingTimes, 0)));
    const remainingTimes = this.toNumber(item.remainingTimes, 0);
    const available = item.available === true || item.available === 'true';
    const status = String(item.status || '').toLowerCase();
    const cardType = String(item.cardType || '').toLowerCase();
    const isMonthlyCard = item.isMonthlyCard === true || item.isMonthlyCard === 'true' || cardType === 'monthly';

    return {
      id: this.toNumber(item.id, 0),
      cardNo: String(item.cardNo || ''),
      storeId: this.toNumber(item.storeId, 0),
      storeName: String(item.storeName || `门店${item.storeId || ''}`),
      sourceText: this.resolveSourceText(item.sourceChannel || item.cardType, isMonthlyCard),
      statusText: this.resolveStatusText(status, available),
      statusClass: available ? 'available' : 'disabled',
      totalTimes,
      usedTimes,
      remainingTimes,
      timesText: isMonthlyCard ? this.resolveMonthlyText(status, available) : this.resolveTimesText(status, available, usedTimes),
      timesLabel: isMonthlyCard ? 'VIP月卡权益' : '单次权益',
      expireText: this.resolveExpireText(item.expireTime),
      externalOrderNo: String(item.externalOrderNo || ''),
      available,
      isMonthlyCard,
    };
  },

  resolveSourceText(value: any, isMonthlyCard = false) {
    if (isMonthlyCard) {
      return 'VIP月卡';
    }
    const text = String(value || '').toLowerCase();
    if (text === 'meituan') {
      return '美团券';
    }
    if (text === 'dazhong') {
      return '大众点评券';
    }
    if (text === 'douyin') {
      return '抖音券';
    }
    return '门店次卡';
  },

  resolveStatusText(status: string, available: boolean) {
    if (available) {
      return '可使用';
    }
    if (status === 'used_up') {
      return '已用完';
    }
    if (status === 'expired') {
      return '已过期';
    }
    if (status === 'cancelled') {
      return '已取消';
    }
    return '不可用';
  },

  resolveTimesText(status: string, available: boolean, usedTimes: number) {
    if (available) {
      return '1 次';
    }
    if (status === 'used_up' || usedTimes > 0) {
      return '已使用';
    }
    return '不可用';
  },

  resolveMonthlyText(status: string, available: boolean) {
    if (available) {
      return '生效中';
    }
    if (status === 'expired') {
      return '已过期';
    }
    return '已失效';
  },

  resolveExpireText(value: any) {
    const text = String(value || '').trim();
    if (!text) {
      return '长期有效';
    }
    return `有效期至 ${text.replace('T', ' ').slice(0, 16)}`;
  },

  toNumber(value: any, fallback = 0) {
    const num = Number(value);
    return Number.isNaN(num) ? fallback : num;
  },

  handleUseCard(e: WechatMiniprogram.TouchEvent) {
    const { storeId, available } = e.currentTarget.dataset as {
      storeId: number;
      available: boolean | string;
    };
    const canUse = available === true || available === 'true';
    if (!canUse) {
      wx.showToast({
        title: '该卡券暂不可用',
        icon: 'none',
      });
      return;
    }

    const safeStoreId = Number(storeId || 0);
    if (safeStoreId) {
      wx.navigateTo({
        url: `/pages/store-detail/index?id=${safeStoreId}`,
      });
      return;
    }

    wx.switchTab({
      url: '/pages/service/index',
    });
  },

  goRedeem() {
    wx.navigateTo({
      url: '/pages/voucher-redeem/index',
    });
  },

  goStores() {
    wx.switchTab({
      url: '/pages/service/index',
    });
  },
});
