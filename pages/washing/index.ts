import { GET, POST } from '../../utils/request';

let orderRefreshTimer: number | undefined;
let displayTimer: number | undefined;
let isRedirectingToDetail = false;
let isLoadingOrder = false;

const ORDER_POLL_INTERVAL = 5000;
const TERMINAL_ORDER_STATUS = ['completed', 'cancelled', 'canceled', 'failed', 'closed'];
const CARD_ORDER_LIMIT_SECONDS = 30 * 60;

type OrderDetail = Record<string, any>;

Page({
  data: {
    orderId: 0,
    storeId: 0,
    bayId: 0,
    deviceId: 0,
    orderNo: '',
    summaryTitle: '洗车进行中',
    summaryDesc: '正在同步订单状态和计费信息。',
    storeName: '',
    deviceLabel: '',
    bayLabel: '',
    startTime: '',
    endTime: '',
    lastSyncTime: '',
    orderStatus: '',
    statusText: '加载中',
    statusDesc: '正在读取订单信息',
    isCardOrder: false,
    timeStatLabel: '展示计时',
    amountLabel: '当前金额',
    amount: '0.00',
    amountValuePrefix: 'CNY ',
    amountValueClass: 'fee',
    tipsText: '订单状态和计费信息会自动同步。',
    primaryActionText: '查看详情',
    timeLabel: '00:00',
    loading: false,
    completing: false,
    hasLoadedOnce: false,
    canComplete: false,
    pollIntervalSeconds: ORDER_POLL_INTERVAL / 1000,
    displayStartTimestamp: 0,
    displayEndTimestamp: 0,
  },

  onLoad(options: Record<string, string>) {
    const orderId = Number((options && options.orderId) || 0);
    const storeId = Number((options && options.storeId) || 0);
    const deviceId = Number((options && options.deviceId) || 0);
    const bayId = Number((options && options.bayId) || 0);

    if (!orderId) {
      this.handleMissingOrderId();
      return;
    }

    isRedirectingToDetail = false;
    this.setData({
      orderId,
      storeId,
      deviceId,
      bayId,
      deviceLabel: deviceId ? String(deviceId) : '',
      bayLabel: bayId ? `${bayId}号位` : '',
    });
    this.loadOrderDetail(orderId, true);
  },

  onShow() {
    const orderId = Number(this.data.orderId || 0);
    if (!this.data.hasLoadedOnce || !orderId || isRedirectingToDetail) {
      return;
    }

    this.loadOrderDetail(orderId, false);
  },

  onHide() {
    this.stopAllTimers();
  },

  onUnload() {
    this.stopAllTimers();
    isRedirectingToDetail = false;
    isLoadingOrder = false;
  },

  handleMissingOrderId() {
    wx.showModal({
      title: '参数错误',
      content: '缺少 orderId，无法进入洗车运行页。',
      showCancel: false,
      success: () => {
        this.navigateBackSafely();
      },
    });
  },

  navigateBackSafely() {
    wx.navigateBack({
      delta: 1,
      fail: () => {
        wx.reLaunch({
          url: '/pages/home/index',
        });
      },
    });
  },

  async loadOrderDetail(orderId: number, showLoading = false) {
    if (!orderId || isLoadingOrder) {
      return;
    }

    isLoadingOrder = true;

    if (showLoading || !this.data.hasLoadedOnce) {
      this.setData({
        loading: true,
        statusText: '加载中',
        statusDesc: '正在读取订单信息',
      });
    }

    try {
      const response = await GET<OrderDetail>(`/api/orders/${orderId}`);
      const detail = (response && response.data) || {};
      this.applyOrderDetail(detail, orderId);
    } catch (error) {
      if (showLoading || !this.data.hasLoadedOnce) {
        wx.showToast({
          title: '加载订单失败',
          icon: 'none',
        });
      }

      this.setData({
        statusText: '加载失败',
        statusDesc: '订单详情读取失败，请点击下方按钮重试。',
      });
      console.error('loadOrderDetail error:', error);
    } finally {
      isLoadingOrder = false;
      this.setData({
        loading: false,
        hasLoadedOnce: true,
        lastSyncTime: this.formatDateTime(new Date()),
      });
    }
  },

  applyOrderDetail(detail: OrderDetail, fallbackOrderId: number) {
    const orderId = Number(detail.id || fallbackOrderId || 0);
    const storeId = Number(detail.storeId || this.data.storeId || 0);
    const deviceId = Number(detail.deviceId || this.data.deviceId || 0);
    const bayId = Number(detail.bayId || this.data.bayId || 0);
    const orderStatus = String(detail.orderStatus || '');
    const isCardOrder = String(detail.payMode || '').toLowerCase() === 'card';
    const startTimestamp = this.parseTime(detail.startTime);
    const endTimestamp = this.parseTime(detail.endTime);
    const displaySeconds = this.resolveDisplaySeconds(
      startTimestamp,
      endTimestamp,
      orderStatus,
      isCardOrder
    );

    this.setData({
      orderId,
      storeId,
      bayId,
      deviceId,
      orderNo: this.formatValue(detail.orderNo || orderId),
      summaryTitle: this.resolveSummaryTitle(detail, orderId, storeId),
      summaryDesc: this.resolveSummaryDesc(orderId, isCardOrder),
      storeName: this.resolveStoreName(detail, storeId),
      deviceLabel: this.resolveDeviceLabel(detail, deviceId),
      bayLabel: this.resolveBayLabel(detail, bayId),
      startTime: this.formatTime(detail.startTime),
      endTime: this.formatTime(detail.endTime),
      orderStatus,
      statusText: this.getStatusText(orderStatus),
      statusDesc: this.getStatusDesc(orderStatus, isCardOrder),
      isCardOrder,
      timeStatLabel: isCardOrder ? '剩余时间' : '展示计时',
      amountLabel: isCardOrder ? '次卡状态' : this.getAmountLabel(orderStatus),
      amount: isCardOrder ? this.resolveCardStateText(orderStatus) : this.resolveOrderAmount(detail, orderStatus),
      amountValuePrefix: isCardOrder ? '' : 'CNY ',
      amountValueClass: isCardOrder ? 'card' : 'fee',
      tipsText: isCardOrder
        ? `次卡订单限时 ${CARD_ORDER_LIMIT_SECONDS / 60} 分钟，到时后自动停止并完成订单。`
        : `订单状态和计费信息每 ${ORDER_POLL_INTERVAL / 1000} 秒自动同步。`,
      primaryActionText: orderStatus === 'running'
        ? (isCardOrder ? '提前结束' : '结束订单')
        : '查看详情',
      timeLabel: this.formatDuration(displaySeconds),
      canComplete: orderStatus === 'running',
      displayStartTimestamp: startTimestamp,
      displayEndTimestamp: endTimestamp,
    });

    this.updateDisplayTimer(orderStatus);
    this.updateOrderPolling(orderStatus);

    if (orderStatus === 'completed') {
      this.goToOrderDetail(orderId);
    }
  },

  updateOrderPolling(orderStatus: string) {
    if (!TERMINAL_ORDER_STATUS.includes(orderStatus)) {
      this.startOrderPolling();
      return;
    }

    this.stopOrderPolling();
  },

  startOrderPolling() {
    if (orderRefreshTimer !== undefined) {
      return;
    }

    orderRefreshTimer = setInterval(() => {
      const orderId = Number(this.data.orderId || 0);
      if (orderId && !isRedirectingToDetail) {
        this.loadOrderDetail(orderId, false);
      }
    }, ORDER_POLL_INTERVAL) as unknown as number;
  },

  stopOrderPolling() {
    if (orderRefreshTimer !== undefined) {
      clearInterval(orderRefreshTimer);
      orderRefreshTimer = undefined;
    }
  },

  updateDisplayTimer(orderStatus: string) {
    this.stopDisplayTimer();

    const startTimestamp = Number(this.data.displayStartTimestamp || 0);
    if (orderStatus !== 'running' || !startTimestamp) {
      return;
    }

    displayTimer = setInterval(() => {
      const isCardOrder = Boolean(this.data.isCardOrder);
      const seconds = this.resolveDisplaySeconds(startTimestamp, 0, orderStatus, isCardOrder);
      this.setData({
        timeLabel: this.formatDuration(seconds),
      });
      if (isCardOrder && seconds <= 0) {
        const orderId = Number(this.data.orderId || 0);
        if (orderId) {
          this.stopDisplayTimer();
          this.loadOrderDetail(orderId, false);
        }
      }
    }, 1000) as unknown as number;
  },

  stopDisplayTimer() {
    if (displayTimer !== undefined) {
      clearInterval(displayTimer);
      displayTimer = undefined;
    }
  },

  stopAllTimers() {
    this.stopOrderPolling();
    this.stopDisplayTimer();
  },

  refreshOrder() {
    const orderId = Number(this.data.orderId || 0);
    if (!orderId) {
      return;
    }

    this.loadOrderDetail(orderId, true);
  },

  handlePrimaryAction() {
    if (this.data.canComplete) {
      this.completeCurrentOrder();
      return;
    }

    this.goToOrderDetail(Number(this.data.orderId || 0));
  },

  completeCurrentOrder() {
    const orderId = Number(this.data.orderId || 0);
    if (!orderId || this.data.completing) {
      return;
    }

    wx.showModal({
      title: '结束订单',
      content: this.data.isCardOrder
        ? '确认提前结束当前次卡订单吗？'
        : '确认结束当前订单吗？订单状态和金额以后端结算结果为准。',
      success: async ({ confirm }) => {
        if (!confirm) {
          return;
        }

        this.setData({ completing: true });

        try {
          await POST(`/api/orders/${orderId}/complete`, {});
          this.goToOrderDetail(orderId);
        } catch (error) {
          wx.showToast({
            title: '结束订单失败',
            icon: 'none',
          });
          console.error('completeCurrentOrder error:', error);
        } finally {
          this.setData({ completing: false });
        }
      },
    });
  },

  goToOrderDetail(orderId: number) {
    if (!orderId || isRedirectingToDetail) {
      return;
    }

    isRedirectingToDetail = true;
    this.stopAllTimers();

    wx.redirectTo({
      url: `/pages/detail/index?id=${orderId}`,
      fail: () => {
        wx.reLaunch({
          url: `/pages/detail/index?id=${orderId}`,
          fail: () => {
            isRedirectingToDetail = false;
          },
        });
      },
    });
  },

  resolveSummaryTitle(detail: OrderDetail, orderId: number, fallbackStoreId: number) {
    const storeName = this.resolveStoreName(detail, fallbackStoreId);
    if (storeName) {
      return storeName;
    }

    const orderNo = this.formatValue(detail.orderNo);
    if (orderNo) {
      return `订单 ${orderNo}`;
    }

    return `订单 ${orderId}`;
  },

  resolveStoreName(detail: OrderDetail, fallbackStoreId = 0) {
    const store = detail.store;
    const storeName = detail.storeName || (store && (store.storeName || store.name));
    if (storeName) {
      return this.formatValue(storeName);
    }

    if (fallbackStoreId) {
      return `门店 ${fallbackStoreId}`;
    }

    return '';
  },

  resolveSummaryDesc(orderId: number, isCardOrder: boolean) {
    if (isCardOrder) {
      return `订单 ${orderId} 正在使用次卡，限时 ${CARD_ORDER_LIMIT_SECONDS / 60} 分钟。`;
    }
    return `订单 ${orderId} 已接入真实运行页，状态和金额按服务端数据实时刷新。`;
  },

  resolveDeviceLabel(detail: OrderDetail, fallbackDeviceId = 0) {
    const deviceCode = this.formatValue(detail.deviceCode || detail.deviceNo);
    if (deviceCode) {
      return deviceCode;
    }

    return this.formatValue(detail.deviceId || fallbackDeviceId);
  },

  resolveBayLabel(detail: OrderDetail, fallbackBayId = 0) {
    const device = detail.device;
    const bayName = detail.bayName || (device && device.bayName);
    if (bayName) {
      return this.formatValue(bayName);
    }

    const bayId = Number(detail.bayId || fallbackBayId || 0);
    if (bayId) {
      return `${bayId}号位`;
    }

    return '';
  },

  resolveOrderAmount(detail: OrderDetail, orderStatus: string) {
    const priorityFields =
      orderStatus === 'completed'
        ? ['finalAmount', 'paidAmount', 'estimatedAmount', 'currentAmount', 'runningAmount']
        : ['currentAmount', 'runningAmount', 'estimatedAmount', 'finalAmount', 'paidAmount'];

    for (let i = 0; i < priorityFields.length; i += 1) {
      const key = priorityFields[i];
      const value = detail[key];

      if (value === null || value === undefined || value === '') {
        continue;
      }

      const amount = Number(value);
      if (!Number.isNaN(amount)) {
        return amount.toFixed(2);
      }

      return String(value);
    }

    return '0.00';
  },

  resolveDisplaySeconds(
    startTimestamp: number,
    endTimestamp: number,
    orderStatus: string,
    isCardOrder = false
  ) {
    if (!startTimestamp) {
      return 0;
    }

    if (isCardOrder && orderStatus === 'running') {
      const elapsedSeconds = Math.floor((Date.now() - startTimestamp) / 1000);
      return Math.max(0, CARD_ORDER_LIMIT_SECONDS - elapsedSeconds);
    }

    if (endTimestamp > startTimestamp) {
      return Math.max(0, Math.floor((endTimestamp - startTimestamp) / 1000));
    }

    if (orderStatus === 'running' || !endTimestamp) {
      return Math.max(0, Math.floor((Date.now() - startTimestamp) / 1000));
    }

    return 0;
  },

  getAmountLabel(orderStatus: string) {
    if (orderStatus === 'completed') {
      return '订单金额';
    }

    return '当前金额';
  },

  resolveCardStateText(orderStatus: string) {
    if (orderStatus === 'running') {
      return '使用次卡中';
    }
    if (orderStatus === 'completed') {
      return '次卡已核销';
    }
    return '次卡订单';
  },

  getStatusText(orderStatus: string) {
    const statusMap: Record<string, string> = {
      pending: '待启动',
      running: '洗车中',
      completed: '已完成',
      cancelled: '已取消',
      canceled: '已取消',
      failed: '已失败',
      closed: '已关闭',
    };

    if (statusMap[orderStatus]) {
      return statusMap[orderStatus];
    }

    if (orderStatus) {
      return orderStatus;
    }

    return '未知状态';
  },

  getStatusDesc(orderStatus: string, isCardOrder = false) {
    if (orderStatus === 'pending') {
      return '订单已创建，等待设备启动。';
    }

    if (orderStatus === 'running') {
      if (isCardOrder) {
        return `当前订单使用次卡，限时 ${CARD_ORDER_LIMIT_SECONDS / 60} 分钟，到时自动结束。`;
      }
      return `订单详情每 ${ORDER_POLL_INTERVAL / 1000} 秒自动同步一次，页面上的计时仅用于展示。`;
    }

    if (orderStatus === 'completed') {
      return '订单已完成，正在跳转到订单详情。';
    }

    if (TERMINAL_ORDER_STATUS.includes(orderStatus)) {
      return '订单已结束，页面已停止自动同步。';
    }

    return '正在等待订单状态更新。';
  },

  formatValue(value: unknown) {
    if (value === null || value === undefined || value === '') {
      return '';
    }

    return String(value);
  },

  formatTime(value: unknown) {
    if (!value) {
      return '';
    }

    return String(value).replace('T', ' ').slice(0, 19);
  },

  formatDateTime(date: Date) {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    const hours = `${date.getHours()}`.padStart(2, '0');
    const minutes = `${date.getMinutes()}`.padStart(2, '0');
    const seconds = `${date.getSeconds()}`.padStart(2, '0');

    return `${year}-${month}-${day} ${hours}:${minutes}:${seconds}`;
  },

  formatDuration(totalSeconds: number) {
    const safeSeconds = Math.max(0, Math.floor(totalSeconds || 0));
    const hours = Math.floor(safeSeconds / 3600);
    const minutes = Math.floor((safeSeconds % 3600) / 60);
    const seconds = safeSeconds % 60;

    if (hours > 0) {
      return [
        `${hours}`.padStart(2, '0'),
        `${minutes}`.padStart(2, '0'),
        `${seconds}`.padStart(2, '0'),
      ].join(':');
    }

    return [`${minutes}`.padStart(2, '0'), `${seconds}`.padStart(2, '0')].join(':');
  },

  parseTime(value: unknown) {
    if (!value) {
      return 0;
    }

    if (typeof value === 'number') {
      return value;
    }

    const normalized = String(value)
      .replace('T', ' ')
      .replace(/\.\d+/, '')
      .replace(/-/g, '/');
    const timestamp = new Date(normalized).getTime();

    return Number.isNaN(timestamp) ? 0 : timestamp;
  },
});
