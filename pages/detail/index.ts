import { getOrderDetail, getOrderStatusLogs } from '../../apis/order';

Page({
  data: {
    orderId: 0,
    loading: false,
    detailFields: [],
    statusLogs: [],
  },

  onLoad(options: Record<string, string>) {
    const orderId = Number((options && options.id) || 0);
    if (!orderId) {
      wx.showToast({
        title: 'Invalid order id',
        icon: 'none',
      });
      return;
    }

    this.setData({ orderId: orderId });
    this.loadOrderDetail(orderId);
  },

  async loadOrderDetail(orderId: number) {
    this.setData({ loading: true });

    try {
      const result = await Promise.all([
        getOrderDetail(orderId),
        getOrderStatusLogs(orderId),
      ]);

      const detail = result[0] || {};
      const logs = result[1] || [];

      this.setData({
        loading: false,
        detailFields: this.mapDetailFields(detail),
        statusLogs: this.mapStatusLogs(logs),
      });
    } catch (error) {
      this.setData({ loading: false });
      wx.showToast({
        title: 'Load failed',
        icon: 'none',
      });
      console.error('loadOrderDetail error:', error);
    }
  },

  mapDetailFields(detail: Record<string, any>) {
    return [
      { label: 'Order No', value: this.formatValue(detail.orderNo) },
      { label: 'User ID', value: this.formatValue(detail.userId) },
      { label: 'Store ID', value: this.formatValue(detail.storeId) },
      { label: 'Device ID', value: this.formatValue(detail.deviceId) },
      { label: 'Order Status', value: this.formatValue(detail.orderStatus) },
      { label: 'Payment Status', value: this.formatValue(detail.paymentStatus) },
      { label: 'Final Amount', value: this.formatAmount(detail.finalAmount) },
      { label: 'Start Time', value: this.formatTime(detail.startTime) },
      { label: 'End Time', value: this.formatTime(detail.endTime) },
      { label: 'Settle Time', value: this.formatTime(detail.settleTime) },
      { label: 'Remark', value: this.formatValue(detail.remark) },
      { label: 'Created At', value: this.formatTime(detail.createdAt) },
    ];
  },

  mapStatusLogs(logs: Record<string, any>[]) {
    if (!Array.isArray(logs)) {
      return [];
    }

    return logs.map((item: Record<string, any>) => ({
      id: Number(item.id || 0),
      fromStatus: this.formatValue(item.fromStatus),
      toStatus: this.formatValue(item.toStatus),
      actionType: this.formatValue(item.actionType),
      createdAt: this.formatTime(item.createdAt),
    }));
  },

  formatValue(value: unknown) {
    if (value === null || value === undefined || value === '') {
      return 'N/A';
    }
    return String(value);
  },

  formatAmount(value: unknown) {
    if (value === null || value === undefined || value === '') {
      return '0.00';
    }

    const amount = Number(value);
    if (Number.isNaN(amount)) {
      return String(value);
    }

    return amount.toFixed(2);
  },

  formatTime(value: unknown) {
    if (!value) {
      return 'N/A';
    }
    return String(value).replace('T', ' ').slice(0, 19);
  },
});
