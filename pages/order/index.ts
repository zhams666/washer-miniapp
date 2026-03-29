import {
  completeOrder,
  createSimpleOrder,
  getSimpleOrderList,
  startOrder,
} from '../../apis/order';

type OrderCardItem = {
  id: number;
  storeName: string;
  createTime: string;
  amount: string;
  status: string;
  statusType: string;
  orderStatus: string;
  actionText: string;
  actionType: string;
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
    const statusMap: Record<string, { text: string; type: string; actionText: string; actionType: string }> = {
      pending: { text: 'Pending', type: 'pending', actionText: 'Start Order', actionType: 'start' },
      running: { text: 'Running', type: 'doing', actionText: 'Finish Order', actionType: 'complete' },
      completed: { text: 'Completed', type: 'done', actionText: 'View Detail', actionType: 'detail' },
    };

    const statusInfo = statusMap[item.orderStatus] || {
      text: item.orderStatus || 'Unknown',
      type: 'cancel',
      actionText: 'View Detail',
      actionType: 'detail',
    };

    return {
      id: Number(item.id || 0),
      storeName: item.storeName || 'Unknown Store',
      createTime: this.formatTime(item.createdAt),
      amount: Number(item.finalAmount || 0).toFixed(2),
      status: statusInfo.text,
      statusType: statusInfo.type,
      orderStatus: item.orderStatus || 'pending',
      actionText: statusInfo.actionText,
      actionType: statusInfo.actionType,
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

  async handleOrderAction(e: WechatMiniprogram.TouchEvent) {
    const { id, action } = e.currentTarget.dataset as { id: number; action: string };

    try {
      if (action === 'start') {
        await startOrder(Number(id));
        wx.showToast({
          title: 'Started',
          icon: 'success',
        });
        this.loadOrders();
        return;
      }

      if (action === 'complete') {
        await completeOrder(Number(id));
        wx.showToast({
          title: 'Completed',
          icon: 'success',
        });
        this.loadOrders();
        return;
      }

      this.showOrderDetail();
    } catch (error) {
      wx.showToast({
        title: 'Action failed',
        icon: 'none',
      });
      console.error('handleOrderAction error:', error);
    }
  },

  showOrderDetail() {
    wx.showToast({
      title: 'Detail pending',
      icon: 'none',
    });
  },
});
