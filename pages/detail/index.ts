import {
  getOrderDetail,
  getOrderPaymentDetails,
  getOrderStatusLogs,
} from '../../apis/order';

type FieldItem = {
  label: string;
  value: string;
};

type StatusLogItem = {
  id: number;
  fromStatus: string;
  toStatus: string;
  actionType: string;
  createdAt: string;
};

type PaymentDetailItem = {
  id: number;
  sourceType: string;
  amountType: string;
  amount: string;
  deductTimes: string;
  settleStage: string;
  allocationStrategy: string;
  createdAt: string;
};

Page({
  data: {
    orderId: 0,
    loading: false,
    detailFields: [] as FieldItem[],
    statusLogs: [] as StatusLogItem[],
    paymentDetails: [] as PaymentDetailItem[],
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
        getOrderPaymentDetails(orderId),
      ]);

      const detail = result[0] || {};
      const logs = result[1] || [];
      const paymentDetails = result[2] || [];

      this.setData({
        loading: false,
        detailFields: this.mapDetailFields(detail),
        statusLogs: this.mapStatusLogs(logs),
        paymentDetails: this.mapPaymentDetails(paymentDetails),
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
      { label: 'Estimated Amount', value: this.formatAmount(detail.estimatedAmount) },
      {
        label: 'First Period Discount Used',
        value: Number(detail.isFirstPeriodDiscountUsed || 0) === 1 ? 'Yes' : 'No',
      },
      { label: 'First Period Discount Amount', value: this.formatAmount(detail.firstPeriodDiscountAmount) },
      { label: 'Order Status', value: this.formatValue(detail.orderStatus) },
      { label: 'Pay Mode', value: this.formatValue(detail.payMode) },
      { label: 'Payment Status', value: this.formatValue(detail.paymentStatus) },
      { label: 'Paid Amount', value: this.formatAmount(detail.paidAmount) },
      { label: 'Final Amount', value: this.formatAmount(this.resolveFinalAmount(detail)) },
      { label: 'Card Usage ID', value: this.formatValue(detail.cardUsageId) },
      { label: 'Card Deduct Times', value: this.formatValue(detail.cardDeductTimes) },
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

  mapPaymentDetails(details: Record<string, any>[]) {
    if (!Array.isArray(details)) {
      return [];
    }

    return details.map((item: Record<string, any>) => ({
      id: Number(item.id || 0),
      sourceType: this.formatValue(item.sourceType),
      amountType: this.formatValue(item.amountType),
      amount: this.formatAmount(item.amount),
      deductTimes: this.formatValue(item.deductTimes),
      settleStage: this.formatValue(item.settleStage),
      allocationStrategy: this.formatValue(item.allocationStrategy),
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

  resolveFinalAmount(detail: Record<string, any>) {
    const finalAmount = Number(detail.finalAmount || 0);
    if (!Number.isNaN(finalAmount) && finalAmount > 0) {
      return finalAmount;
    }

    const estimatedAmount = Number(detail.estimatedAmount || 0);
    if (!Number.isNaN(estimatedAmount) && estimatedAmount > 0) {
      return estimatedAmount;
    }

    return detail.finalAmount;
  },

  formatTime(value: unknown) {
    if (!value) {
      return 'N/A';
    }
    return String(value).replace('T', ' ').slice(0, 19);
  },
});
