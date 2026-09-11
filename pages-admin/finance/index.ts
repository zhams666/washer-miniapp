import { getMiniAdminDashboard, getMiniAdminFinanceDetails, getMiniAdminStores } from '../../apis/admin';
import { ensureAdminToken } from '../../utils/admin-auth';

const formatMoney = (value: any) => Number(value || 0).toFixed(2);
const formatTime = (value: any) => String(value || '').replace('T', ' ').slice(0, 16) || '--';

const detailTitleMap: Record<string, string> = {
  washCount: '今日洗车明细',
  consumeAmount: '今日消费明细',
  rechargeAmount: '今日充值明细',
  cardUsageTimes: '次卡核销明细',
};

Page({
  data: {
    loading: false,
    detailLoading: false,
    stores: [] as any[],
    storePickerOptions: ['全部门店'] as string[],
    selectedStoreIndex: 0,
    selectedStoreId: '',
    detailMetricKey: '',
    detailTitle: '今日明细',
    financeCards: [] as any[],
    detailItems: [] as any[],
    recentActivities: [] as any[],
  },

  onLoad(options?: Record<string, string | undefined>) {
    const metricKey = String((options && options.metricKey) || '').trim();
    const storeId = String((options && options.storeId) || '').trim();
    this.setData({
      detailMetricKey: metricKey,
      detailTitle: detailTitleMap[metricKey] || '今日明细',
      selectedStoreId: storeId,
    });
    this.loadInitial();
  },

  async loadInitial() {
    try {
      ensureAdminToken();
    } catch (error) {
      return;
    }
    const stores = await getMiniAdminStores().catch(() => []);
    const preselectedStoreIndex = this.data.selectedStoreId
      ? stores.findIndex((store) => String(store.id) === this.data.selectedStoreId) + 1
      : 0;
    this.setData({
      stores,
      storePickerOptions: ['全部门店'].concat(stores.map((store) => store.storeName || `门店${store.id}`)),
      selectedStoreIndex: Math.max(0, preselectedStoreIndex),
      selectedStoreId: preselectedStoreIndex > 0 ? this.data.selectedStoreId : '',
    });
    this.loadFinance();
  },

  async loadFinance() {
    this.setData({ loading: true });
    try {
      const dashboard = await getMiniAdminDashboard({
        storeId: this.data.selectedStoreId || undefined,
      });
      const metrics = Array.isArray(dashboard.metrics) ? dashboard.metrics : [];
      const financeCards = metrics
        .filter((metric) => ['consumeAmount', 'rechargeAmount', 'cardUsageTimes'].includes(metric.key))
        .map((metric) => ({
          key: metric.key,
          title: metric.title,
          value: metric.unit === '元' ? formatMoney(metric.amount) : String(metric.count || 0),
          unit: metric.unit || '',
          desc: metric.description || '',
        }));
      const recentActivities = Array.isArray(dashboard.recentActivities)
        ? dashboard.recentActivities.map((item) => ({
            ...item,
            amountText: formatMoney(item.amount),
          }))
        : [];
      this.setData({
        financeCards,
        recentActivities,
      });
      if (this.data.detailMetricKey) {
        await this.loadMetricDetails(this.data.detailMetricKey);
      }
    } catch (error) {
      console.error('load mini admin finance failed:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  handleStoreChange(e: WechatMiniprogram.PickerChange) {
    const selectedStoreIndex = Number(e.detail.value || 0);
    const store = selectedStoreIndex > 0 ? this.data.stores[selectedStoreIndex - 1] : null;
    this.setData({
      selectedStoreIndex,
      selectedStoreId: store && store.id ? String(store.id) : '',
    });
    this.loadFinance();
  },

  handleFinanceCardTap(e: WechatMiniprogram.TouchEvent) {
    const key = String(e.currentTarget.dataset.key || '');
    if (!key) {
      return;
    }
    this.setData({
      detailMetricKey: key,
      detailTitle: detailTitleMap[key] || '今日明细',
      detailItems: [],
    });
    this.loadMetricDetails(key);
  },

  async loadMetricDetails(metricKey: string) {
    this.setData({ detailLoading: true });
    try {
      const records = await getMiniAdminFinanceDetails({
        metricKey,
        storeId: this.data.selectedStoreId || undefined,
      });
      this.setData({
        detailItems: (Array.isArray(records) ? records : []).map((item) => ({
          ...item,
          amountText: formatMoney(item.amount),
          countText: String(item.count || 0),
          occurredText: formatTime(item.occurredAt),
          userText: item.userNickname || item.userMobile || (item.userId ? `用户${item.userId}` : '未知用户'),
        })),
      });
    } catch (error) {
      console.error('load mini admin finance detail failed:', error);
    } finally {
      this.setData({ detailLoading: false });
    }
  },
});
