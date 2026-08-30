import { getMiniAdminOrders, getMiniAdminStores } from '../../apis/admin';
import { ensureAdminToken } from '../../utils/admin-auth';

const orderStatusText: Record<string, string> = {
  pending: '待开始',
  running: '进行中',
  completed: '已完成',
  cancelled: '已取消',
};

const payStatusText: Record<string, string> = {
  unpaid: '未支付',
  paid: '已支付',
  refunded: '已退款',
};

const formatDateTime = (value?: string) => {
  return value ? String(value).replace('T', ' ').slice(0, 16) : '--';
};

Page({
  data: {
    loading: false,
    stores: [] as any[],
    storePickerOptions: ['全部门店'] as string[],
    statusOptions: ['全部状态', '待开始', '进行中', '已完成'],
    statusValues: ['', 'pending', 'running', 'completed'],
    selectedStoreIndex: 0,
    selectedStatusIndex: 0,
    selectedStoreId: '',
    orderStatus: '',
    keyword: '',
    page: 1,
    size: 10,
    hasMore: true,
    orders: [] as any[],
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
    this.reloadOrders();
  },

  async reloadOrders() {
    this.setData({
      page: 1,
      hasMore: true,
      orders: [],
    });
    await this.loadOrders();
  },

  async loadOrders() {
    if (this.data.loading || !this.data.hasMore) {
      return;
    }
    this.setData({ loading: true });
    try {
      const result = await getMiniAdminOrders({
        page: this.data.page,
        size: this.data.size,
        storeId: this.data.selectedStoreId || undefined,
        orderStatus: this.data.orderStatus || undefined,
        keyword: this.data.keyword || undefined,
      });
      const records = Array.isArray(result.records) ? result.records : [];
      const mapped = records.map((order) => ({
        ...order,
        displayTime: formatDateTime(order.createdAt),
        orderStatusText: orderStatusText[String(order.orderStatus || '')] || order.orderStatus || '--',
        paymentStatusText: payStatusText[String(order.paymentStatus || '')] || order.paymentStatus || '--',
        amountText: Number(order.finalAmount || 0).toFixed(2),
      }));
      const nextOrders = this.data.orders.concat(mapped);
      this.setData({
        orders: nextOrders,
        page: this.data.page + 1,
        hasMore: nextOrders.length < Number(result.total || 0),
      });
    } catch (error) {
      console.error('load mini admin orders failed:', error);
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
    this.reloadOrders();
  },

  handleStatusChange(e: WechatMiniprogram.PickerChange) {
    const selectedStatusIndex = Number(e.detail.value || 0);
    this.setData({
      selectedStatusIndex,
      orderStatus: this.data.statusValues[selectedStatusIndex] || '',
    });
    this.reloadOrders();
  },

  handleKeywordInput(e: WechatMiniprogram.Input) {
    this.setData({ keyword: e.detail.value });
  },

  handleSearch() {
    this.reloadOrders();
  },

  handleLoadMore() {
    this.loadOrders();
  },
});
