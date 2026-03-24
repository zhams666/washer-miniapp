import { createSimpleOrder, getSimpleOrderList } from '../../apis/order';

type OrderCardItem = {
  id: number;
  storeName: string;
  createTime: string;
  amount: string;
  status: string;
  statusType: string;
};

Page({
  data: {
    list: [] as OrderCardItem[],
    loading: false,
  },

  onLoad() {
    this.loadOrders();
  },

  async loadOrders() {
    this.setData({ loading: true });

    try {
      const orders = await getSimpleOrderList(1, 10);
      this.setData({
        list: orders.map((item: Record<string, any>) => this.mapOrderItem(item)),
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
      wx.showToast({
        title: 'Load failed',
        icon: 'none',
      });
      console.error('loadOrders error:', error);
    }
  },

  mapOrderItem(item: Record<string, any>): OrderCardItem {
    const statusMap: Record<string, { text: string; type: string }> = {
      completed: { text: 'Done', type: 'done' },
      running: { text: 'Doing', type: 'doing' },
      pending: { text: 'Pending', type: 'doing' },
      cancelled: { text: 'Cancelled', type: 'cancel' },
    };

    const statusInfo = statusMap[item.orderStatus] || {
      text: item.orderStatus || 'Unknown',
      type: 'cancel',
    };

    return {
      id: Number(item.id || 0),
      storeName: item.storeName || 'Unknown Store',
      createTime: this.formatTime(item.createdAt),
      amount: Number(item.finalAmount || 0).toFixed(2),
      status: statusInfo.text,
      statusType: statusInfo.type,
    };
  },

  formatTime(value: string) {
    if (!value) {
      return 'N/A';
    }
    return value.replace('T', ' ').slice(0, 16);
  },

  async createTestOrder() {
    try {
      await createSimpleOrder({
        userId: 1,
        storeId: 1,
        finalAmount: 18,
        remark: 'miniapp test order',
      });

      wx.showToast({
        title: 'Created',
        icon: 'success',
      });

      this.loadOrders();
    } catch (error) {
      wx.showToast({
        title: 'Create failed',
        icon: 'none',
      });
      console.error('createTestOrder error:', error);
    }
  },

  showOrderDetail() {
    wx.showToast({
      title: 'Detail pending',
      icon: 'none',
    });
  },
});
