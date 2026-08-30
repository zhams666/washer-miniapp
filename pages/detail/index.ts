import {
  getOrderDetail,
  getOrderPaymentDetails,
  getOrderStatusLogs,
} from '../../apis/order';

let detailRefreshTimer: number | undefined;

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

  onHide() {
    this.stopDetailPolling();
  },

  onUnload() {
    this.stopDetailPolling();
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
      this.updateDetailPolling(String(detail.orderStatus || ''));
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
    const isCardOrder = String(detail.payMode || '').toLowerCase() === 'card';
    const fields: FieldItem[] = [
      { label: '订单号', value: this.formatValue(detail.orderNo) },
      { label: '用户 ID', value: this.formatValue(detail.userId) },
      { label: '门店 ID', value: this.formatValue(detail.storeId) },
      { label: '设备 ID', value: this.formatValue(detail.deviceId) },
      { label: '订单状态', value: this.formatStatus(detail.orderStatus) },
      { label: '支付方式', value: isCardOrder ? '次卡支付' : '钱包支付' },
      { label: '支付状态', value: this.formatValue(detail.paymentStatus) },
    ];

    if (isCardOrder) {
      fields.push(
        { label: '次卡状态', value: detail.orderStatus === 'running' ? '使用次卡中' : '已核销次卡' },
        { label: '次卡时长', value: '30 分钟' },
        { label: '核销次数', value: `${Number(detail.cardDeductTimes || 1)} 次` }
      );
    } else {
      fields.push(
        { label: '预估金额', value: this.formatAmount(detail.estimatedAmount) },
        {
          label: '优惠券',
          value: Number(detail.isFirstPeriodDiscountUsed || 0) === 1 ? '已使用' : '未使用',
        },
        { label: '优惠金额', value: this.formatAmount(detail.firstPeriodDiscountAmount) },
        { label: '实付金额', value: this.formatAmount(detail.paidAmount) },
        { label: '订单金额', value: this.formatAmount(this.resolveFinalAmount(detail)) }
      );
    }

    fields.push(
      { label: '开始时间', value: this.formatTime(detail.startTime) },
      { label: '结束时间', value: this.formatTime(detail.endTime) },
      { label: '结算时间', value: this.formatTime(detail.settleTime) },
      { label: '备注', value: this.formatValue(detail.remark) },
      { label: '创建时间', value: this.formatTime(detail.createdAt) }
    );

    return fields;
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
      sourceType: this.formatSourceType(item.sourceType),
      amountType: this.formatAmountType(item.amountType),
      amount: String(item.sourceType || '').toLowerCase() === 'card'
        ? '不计金额'
        : this.formatAmount(item.amount),
      deductTimes: this.formatValue(item.deductTimes),
      settleStage: this.formatValue(item.settleStage),
      allocationStrategy: this.formatValue(item.allocationStrategy),
      createdAt: this.formatTime(item.createdAt),
    }));
  },

  formatStatus(value: unknown) {
    const status = String(value || '');
    const statusMap: Record<string, string> = {
      pending: '待启动',
      running: '洗车中',
      completed: '已完成',
      cancelled: '已取消',
      canceled: '已取消',
      failed: '已失败',
      closed: '已关闭',
    };
    return statusMap[status] || this.formatValue(value);
  },

  formatSourceType(value: unknown) {
    const text = String(value || '').toLowerCase();
    if (text === 'card') {
      return '次卡';
    }
    if (text === 'wallet') {
      return '钱包';
    }
    return this.formatValue(value);
  },

  formatAmountType(value: unknown) {
    const text = String(value || '').toLowerCase();
    if (text === 'principal') {
      return '余额';
    }
    if (text === 'gift') {
      return '赠送余额';
    }
    if (text === 'card') {
      return '次卡';
    }
    return this.formatValue(value);
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

  updateDetailPolling(orderStatus: string) {
    if (orderStatus === 'running') {
      this.startDetailPolling();
      return;
    }
    this.stopDetailPolling();
  },

  startDetailPolling() {
    if (detailRefreshTimer !== undefined) {
      return;
    }
    detailRefreshTimer = setInterval(() => {
      const orderId = Number(this.data.orderId || 0);
      if (orderId) {
        this.loadOrderDetail(orderId);
      }
    }, 15000) as unknown as number;
  },

  stopDetailPolling() {
    if (detailRefreshTimer !== undefined) {
      clearInterval(detailRefreshTimer);
      detailRefreshTimer = undefined;
    }
  },
});
