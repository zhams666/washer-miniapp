import { getMiniAdminDashboard, getMiniAdminStores } from '../../apis/admin';
import { ensureAdminToken } from '../../utils/admin-auth';

const formatMoney = (value: any) => Number(value || 0).toFixed(2);

Page({
  data: {
    loading: false,
    stores: [] as any[],
    storePickerOptions: ['全部门店'] as string[],
    selectedStoreIndex: 0,
    selectedStoreId: '',
    financeCards: [] as any[],
    recentActivities: [] as any[],
  },

  onLoad() {
    this.loadInitial();
  },

  async loadInitial() {
    try {
      ensureAdminToken();
    } catch (error) {
      return;
    }
    const stores = await getMiniAdminStores().catch(() => []);
    this.setData({
      stores,
      storePickerOptions: ['全部门店'].concat(stores.map((store) => store.storeName || `门店${store.id}`)),
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
});
