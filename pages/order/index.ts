import { getSimpleOrderList } from '../../apis/order';
import { requireCurrentUser } from '../../utils/user';

let orderRefreshTimer: number | undefined;

const ORDER_PAGE_SIZE = 50;
const ORDER_POLL_INTERVAL = 15000;
const TEXT_ORDER_LOAD_FAILED = '\u8ba2\u5355\u52a0\u8f7d\u5931\u8d25';
const TEXT_LOGIN_REQUIRED = '\u8bf7\u5148\u767b\u5f55';

type OrderCardItem = {
  id: number;
  orderNo: string;
  storeName: string;
  createTime: string;
  amount: string;
  amountText: string;
  amountLabel: string;
  amountValue: string;
  amountClass: string;
  payMode: string;
  payText: string;
  status: string;
  statusType: string;
  orderStatus: string;
  statusHint: string;
  timeLabel: string;
  isRunning: boolean;
  actionText: string;
};

type CurrentUser = {
  userId: number;
};

type LoadOptions = {
  showLoading?: boolean;
  showErrorToast?: boolean;
};

Page({
  data: {
    list: [] as OrderCardItem[],
    loading: false,
    isLogin: false,
    userId: 0,
    runningCount: 0,
    recordedCount: 0,
  },

  onLoad() {
    void this.enterPage();
  },

  onShow() {
    const tabBar = (this as any).getTabBar && (this as any).getTabBar();
    if (tabBar && tabBar.setData) {
      tabBar.setData({ selectedPath: 'pages/order/index' });
    }
    void this.enterPage();
  },

  onHide() {
    this.stopOrderPolling();
  },

  onUnload() {
    this.stopOrderPolling();
  },

  async enterPage() {
    const page = this as any;
    const pageEnterSequence = (page._pageEnterSequence || 0) + 1;
    page._pageEnterSequence = pageEnterSequence;

    try {
      const currentUser = await this.requirePageUser();
      if (pageEnterSequence !== page._pageEnterSequence) {
        return false;
      }

      const previousUserId = Number(this.data.userId || 0);
      const nextUserId = this.applyCurrentUser(currentUser);

      if (previousUserId && previousUserId !== nextUserId) {
        this.stopOrderPolling();
        this.setData({
          list: [],
          runningCount: 0,
          recordedCount: 0,
        });
      }

      await this.loadOrdersForUser(nextUserId, {
        showLoading: true,
        showErrorToast: true,
      });
      return true;
    } catch (error) {
      if (pageEnterSequence !== page._pageEnterSequence) {
        return false;
      }

      this.handleRequireCurrentUserError(error, true);
      console.error('enterPage error:', error);
      return false;
    }
  },

  normalizeUserId(value: unknown) {
    const userId = Number(value || 0);
    if (Number.isInteger(userId) && userId > 0) {
      return userId;
    }
    return 0;
  },

  async requirePageUser(): Promise<CurrentUser> {
    const result = await requireCurrentUser();
    const userId = this.normalizeUserId(result && result.costomerId);

    if (!userId) {
      throw new Error('current user is required');
    }

    return {
      userId,
    };
  },

  applyCurrentUser(currentUser: CurrentUser) {
    const userId = this.normalizeUserId(currentUser && currentUser.userId);

    this.setData({
      isLogin: Boolean(userId),
      userId,
    });

    return userId;
  },

  resetCurrentUserState() {
    this.stopOrderPolling();
    this.setData({
      list: [],
      loading: false,
      isLogin: false,
      userId: 0,
      runningCount: 0,
      recordedCount: 0,
    });
  },

  handleRequireCurrentUserError(error: unknown, showToast: boolean) {
    this.resetCurrentUserState();

    if (!showToast) {
      return;
    }

    wx.showToast({
      title: this.resolveRequireCurrentUserErrorMessage(error),
      icon: 'none',
    });
  },

  resolveRequireCurrentUserErrorMessage(error: unknown) {
    const message = this.extractErrorMessage(error);
    if (!message || message.includes('current user is required')) {
      return TEXT_LOGIN_REQUIRED;
    }
    return message;
  },

  extractErrorMessage(error: unknown) {
    const safeError = error as Record<string, any>;
    const candidates = [
      safeError && safeError.msg,
      safeError && safeError.message,
      safeError && safeError.errMsg,
    ];

    for (let i = 0; i < candidates.length; i += 1) {
      const message = candidates[i];
      if (typeof message === 'string' && message.trim()) {
        return message.trim();
      }
    }

    return '';
  },

  async loadOrdersForUser(userId: number, options: LoadOptions = {}) {
    const safeUserId = this.normalizeUserId(userId);
    if (!safeUserId) {
      return;
    }

    const page = this as any;
    const showLoading = options.showLoading !== false;
    const showErrorToast = options.showErrorToast !== false;
    const orderLoadSequence = (page._orderLoadSequence || 0) + 1;
    page._orderLoadSequence = orderLoadSequence;

    if (showLoading) {
      this.setData({ loading: true });
    }

    try {
      const orders = await getSimpleOrderList(safeUserId, ORDER_PAGE_SIZE);
      if (orderLoadSequence !== page._orderLoadSequence) {
        return;
      }

      if (Number(this.data.userId || 0) !== safeUserId) {
        return;
      }

      const list = orders.map((item) => this.mapOrderItem(item as Record<string, any>));
      const runningCount = list.filter((item) => item.orderStatus === 'running').length;
      const recordedCount = Math.max(list.length - runningCount, 0);

      this.setData({
        list,
        runningCount,
        recordedCount,
        loading: false,
      });
      this.updateOrderPolling(list);
    } catch (error) {
      if (orderLoadSequence !== page._orderLoadSequence) {
        return;
      }

      if (Number(this.data.userId || 0) !== safeUserId) {
        return;
      }

      this.setData({ loading: false });

      if (showErrorToast) {
        wx.showToast({
          title: TEXT_ORDER_LOAD_FAILED,
          icon: 'none',
        });
      }

      console.error('loadOrdersForUser error:', error);
    }
  },

  mapOrderItem(item: Record<string, any>): OrderCardItem {
    const orderStatus = String(item.orderStatus || '');
    const payMode = String(item.payMode || '').toLowerCase();
    const isCardOrder = payMode === 'card';
    const amountValue = this.resolveOrderAmountValue(item, isCardOrder);
    const isRunning = orderStatus === 'running';

    return {
      id: Number(item.id || 0),
      orderNo: this.resolveOrderNo(item),
      storeName: item.storeName || '\u6d17\u8f66\u8ba2\u5355',
      createTime: this.formatTime(item.createdAt),
      amount: this.resolveOrderAmount(item),
      amountText: this.resolveOrderAmountText(item, isCardOrder),
      amountLabel: this.resolveAmountLabel(item, isCardOrder),
      amountValue,
      amountClass: isCardOrder ? 'card' : 'wallet',
      payMode,
      payText: this.resolvePayText(item, isCardOrder),
      status: this.getStatusText(orderStatus),
      statusType: this.getStatusType(orderStatus),
      orderStatus,
      statusHint: this.getStatusHint(orderStatus),
      timeLabel: this.resolveTimeLabel(orderStatus),
      isRunning,
      actionText: isRunning ? '\u7ee7\u7eed\u6d17\u8f66' : '\u67e5\u770b\u8be6\u60c5',
    };
  },

  handleOrderTap(e: WechatMiniprogram.TouchEvent) {
    const dataset = (e.currentTarget.dataset || {}) as {
      id?: number | string;
      orderStatus?: string;
    };
    const orderId = Number(dataset.id || 0);
    const orderStatus = String(dataset.orderStatus || '');

    if (!orderId) {
      wx.showToast({
        title: '\u8ba2\u5355\u4fe1\u606f\u65e0\u6548',
        icon: 'none',
      });
      return;
    }

    if (orderStatus === 'running') {
      wx.navigateTo({
        url: `/pages/washing/index?orderId=${orderId}`,
      });
      return;
    }

    wx.navigateTo({
      url: `/pages/detail/index?id=${orderId}`,
    });
  },

  updateOrderPolling(list: OrderCardItem[]) {
    const hasRunningOrder =
      Array.isArray(list) && list.some((item) => item.orderStatus === 'running');

    if (hasRunningOrder) {
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
      const userId = Number(this.data.userId || 0);
      if (userId) {
        void this.loadOrdersForUser(userId, {
          showLoading: false,
          showErrorToast: false,
        });
      }
    }, ORDER_POLL_INTERVAL) as unknown as number;
  },

  stopOrderPolling() {
    if (orderRefreshTimer !== undefined) {
      clearInterval(orderRefreshTimer);
      orderRefreshTimer = undefined;
    }
  },

  getStatusText(orderStatus: string) {
    const statusMap: Record<string, string> = {
      pending: '\u5f85\u542f\u52a8',
      running: '\u8fdb\u884c\u4e2d',
      completed: '\u5df2\u5b8c\u6210',
      cancelled: '\u5df2\u53d6\u6d88',
      canceled: '\u5df2\u53d6\u6d88',
      failed: '\u5df2\u5931\u8d25',
      closed: '\u5df2\u5173\u95ed',
    };

    if (statusMap[orderStatus]) {
      return statusMap[orderStatus];
    }

    if (orderStatus) {
      return orderStatus;
    }

    return '\u672a\u77e5\u72b6\u6001';
  },

  getStatusType(orderStatus: string) {
    if (orderStatus === 'completed') {
      return 'done';
    }

    if (orderStatus === 'running') {
      return 'doing';
    }

    if (orderStatus === 'pending') {
      return 'pending';
    }

    return 'cancel';
  },

  getStatusHint(orderStatus: string) {
    const hintMap: Record<string, string> = {
      pending: '\u8ba2\u5355\u5df2\u521b\u5efa\uff0c\u7b49\u5f85\u8bbe\u5907\u542f\u52a8',
      running: '\u6b63\u5728\u4f7f\u7528\u4e2d\uff0c\u53ef\u7ee7\u7eed\u63a7\u5236\u8bbe\u5907',
      completed: '\u6d17\u8f66\u5df2\u5b8c\u6210',
      cancelled: '\u8ba2\u5355\u5df2\u53d6\u6d88',
      canceled: '\u8ba2\u5355\u5df2\u53d6\u6d88',
      failed: '\u8ba2\u5355\u5904\u7406\u5931\u8d25',
      closed: '\u8ba2\u5355\u5df2\u5173\u95ed',
    };

    return hintMap[orderStatus] || '\u67e5\u770b\u8ba2\u5355\u8be6\u60c5';
  },

  resolveTimeLabel(orderStatus: string) {
    if (orderStatus === 'running') {
      return '\u5f00\u59cb\u65f6\u95f4';
    }

    if (orderStatus === 'completed') {
      return '\u4e0b\u5355\u65f6\u95f4';
    }

    return '\u521b\u5efa\u65f6\u95f4';
  },

  resolveOrderNo(item: Record<string, any>) {
    const orderNo = String(item.orderNo || '').trim();
    if (orderNo) {
      return orderNo;
    }

    const id = Number(item.id || 0);
    if (id) {
      return `#${id}`;
    }

    return '';
  },

  formatTime(value: unknown) {
    if (!value) {
      return '';
    }

    return String(value).replace('T', ' ').slice(0, 16);
  },

  resolveOrderAmount(item: Record<string, any>) {
    if (String(item.payMode || '').toLowerCase() === 'card') {
      return '0.00';
    }

    const finalAmount = Number(item.finalAmount || 0);
    if (!Number.isNaN(finalAmount) && finalAmount > 0) {
      return finalAmount.toFixed(2);
    }

    const estimatedAmount = Number(item.estimatedAmount || 0);
    if (!Number.isNaN(estimatedAmount) && estimatedAmount > 0) {
      return estimatedAmount.toFixed(2);
    }

    return '0.00';
  },

  resolveOrderAmountText(item: Record<string, any>, isCardOrder: boolean) {
    if (isCardOrder) {
      const deductTimes = this.resolveCardDeductTimes(item);
      return `次卡支付 · ${deductTimes}次`;
    }

    return `金额 ¥${this.resolveOrderAmount(item)}`;
  },

  resolveAmountLabel(item: Record<string, any>, isCardOrder: boolean) {
    if (isCardOrder) {
      return '\u6b21\u5361\u6838\u9500';
    }

    const finalAmount = Number(item.finalAmount || 0);
    if (!Number.isNaN(finalAmount) && finalAmount > 0) {
      return '\u5b9e\u4ed8\u91d1\u989d';
    }

    return '\u9884\u4f30\u91d1\u989d';
  },

  resolveOrderAmountValue(item: Record<string, any>, isCardOrder: boolean) {
    if (isCardOrder) {
      return `${this.resolveCardDeductTimes(item)}次`;
    }

    return `¥${this.resolveOrderAmount(item)}`;
  },

  resolvePayText(item: Record<string, any>, isCardOrder: boolean) {
    if (isCardOrder) {
      const orderStatus = String(item.orderStatus || '');
      return orderStatus === 'running' ? '使用次卡中' : '次卡订单';
    }
    return '钱包订单';
  },

  resolveCardDeductTimes(item: Record<string, any>) {
    const value = Number(item.cardDeductTimes || 0);
    if (!Number.isNaN(value) && value > 0) {
      return value;
    }
    return 1;
  },
});
