import { getRechargeOrderResult, syncRechargeOrder } from '../../apis/wallet';

const TEXT_MISSING_ORDER = '缺少充值订单号';
const TEXT_QUERY_FAILED = '充值结果查询失败';

Page({
  data: {
    loading: false,
    rechargeOrderNo: '',
    principalAmount: '0.00',
    giftAmount: '0.00',
    payStatus: '',
    paymentResult: '',
    source: '',
    returnStoreId: 0,
    returnBayId: 0,
    returnDeviceId: 0,
    canReturnToWash: false,
    payStatusText: '处理中',
    statusTitle: '充值结果',
    statusDesc: '正在加载充值订单结果...',
    errorMessage: '',
  },

  onLoad(options: Record<string, string>) {
    const query = options || {};
    const rechargeOrderNo = String(query.rechargeOrderNo || '').trim();
    const principalAmount = this.formatAmount(query.principalAmount);
    const giftAmount = this.formatAmount(query.giftAmount);
    const payStatus = String(query.payStatus || '').trim();
    const paymentResult = String(query.paymentResult || '').trim();
    const source = String(query.source || '').trim();
    const returnStoreId = this.normalizeId(query.returnStoreId);
    const returnBayId = this.normalizeId(query.returnBayId);
    const returnDeviceId = this.normalizeId(query.returnDeviceId);
    const displayPayStatus = this.resolveDisplayPayStatus(payStatus, paymentResult);

    this.setData({
      rechargeOrderNo,
      principalAmount,
      giftAmount,
      payStatus,
      paymentResult,
      source,
      returnStoreId,
      returnBayId,
      returnDeviceId,
      canReturnToWash: source === 'startWash' && returnStoreId > 0,
      payStatusText: this.resolvePayStatusText(displayPayStatus),
      statusTitle: this.resolveStatusTitle(displayPayStatus, false),
      statusDesc: this.resolveStatusDesc(displayPayStatus, false),
    });

    if (!rechargeOrderNo) {
      this.setData({
        errorMessage: TEXT_MISSING_ORDER,
        statusTitle: '订单信息缺失',
        statusDesc: '未获取到有效的充值订单号，请返回上一页后重试。',
      });
      return;
    }

    void this.loadRechargeOrderResult(rechargeOrderNo);
  },

  normalizeId(value: unknown) {
    const id = Number(value || 0);
    if (Number.isInteger(id) && id > 0) {
      return id;
    }
    return 0;
  },

  async loadRechargeOrderResult(rechargeOrderNo: string) {
    this.setData({ loading: true, errorMessage: '' });

    try {
      const result = await getRechargeOrderResult(rechargeOrderNo);
      const latestResult =
        String((result && result.payStatus) || '').trim() === 'pending'
          ? await this.syncPendingRechargeOrder(rechargeOrderNo, result)
          : result;
      const nextPayStatus = String((latestResult && latestResult.payStatus) || this.data.payStatus || '').trim();
      const displayPayStatus = this.resolveDisplayPayStatus(nextPayStatus, this.data.paymentResult);
      this.setData({
        rechargeOrderNo: String((latestResult && latestResult.rechargeOrderNo) || rechargeOrderNo),
        principalAmount: this.formatAmount(latestResult && latestResult.principalAmount, this.data.principalAmount),
        giftAmount: this.formatAmount(latestResult && latestResult.giftAmount, this.data.giftAmount),
        payStatus: nextPayStatus,
        payStatusText: this.resolvePayStatusText(displayPayStatus),
        statusTitle: this.resolveStatusTitle(displayPayStatus, false),
        statusDesc: this.resolveStatusDesc(displayPayStatus, false),
      });
    } catch (error) {
      const errorMessage = this.extractErrorMessage(error) || TEXT_QUERY_FAILED;
      const currentPayStatus = String(this.data.payStatus || '').trim();
      const displayPayStatus = this.resolveDisplayPayStatus(currentPayStatus, this.data.paymentResult);
      this.setData({
        errorMessage,
        statusTitle: this.resolveStatusTitle(displayPayStatus, true),
        statusDesc: errorMessage,
      });
      console.error('loadRechargeOrderResult error:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  async syncPendingRechargeOrder(
    rechargeOrderNo: string,
    fallbackResult: Record<string, any>
  ) {
    try {
      const synced = await syncRechargeOrder(rechargeOrderNo);
      return synced && synced.rechargeOrderNo ? synced : fallbackResult;
    } catch (error) {
      console.warn('syncPendingRechargeOrder error:', error);
      return fallbackResult;
    }
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

  formatAmount(value: unknown, fallback = '0.00') {
    if (value === null || value === undefined || value === '') {
      return fallback;
    }
    const amount = Number(value);
    if (Number.isNaN(amount)) {
      return fallback;
    }
    return amount.toFixed(2);
  },

  resolvePayStatusText(payStatus: string) {
    const statusMap: Record<string, string> = {
      paid: '已支付',
      pending: '待支付',
      failed: '支付失败',
      cancelled: '已取消',
      canceled: '已取消',
      closed: '已关闭',
    };
    return statusMap[payStatus] || (payStatus ? payStatus : '处理中');
  },

  resolveDisplayPayStatus(payStatus: string, paymentResult: string) {
    if (payStatus === 'paid' || payStatus === 'failed' || payStatus === 'closed') {
      return payStatus;
    }
    if (payStatus === 'pending' && paymentResult === 'cancel') {
      return 'canceled';
    }
    if (payStatus === 'pending' && paymentResult === 'fail') {
      return 'failed';
    }
    return payStatus;
  },

  resolveStatusTitle(payStatus: string, hasError: boolean) {
    if (hasError) {
      return '结果查询失败';
    }
    if (payStatus === 'paid') {
      return '充值成功';
    }
    if (payStatus === 'failed') {
      return '充值失败';
    }
    if (payStatus === 'cancelled' || payStatus === 'canceled') {
      return '充值已取消';
    }
    if (payStatus === 'closed') {
      return '充值已关闭';
    }
    return '充值结果';
  },

  resolveStatusDesc(payStatus: string, hasError: boolean) {
    if (hasError) {
      return '未能实时获取充值结果，请稍后返回钱包页查看余额和流水。';
    }
    if (payStatus === 'paid') {
      return '充值订单已完成，稍后返回钱包页可查看余额与充值记录。';
    }
    if (payStatus === 'failed') {
      return '充值订单未完成，请返回上一页后重新发起充值。';
    }
    if (payStatus === 'cancelled' || payStatus === 'canceled') {
      return '充值流程已取消，当前不会写入钱包余额。';
    }
    if (payStatus === 'closed') {
      return '充值订单已关闭，当前不会写入钱包余额。';
    }
    return '正在等待充值订单结果，请稍候。';
  },

  goBack() {
    wx.navigateBack({
      delta: 1,
      fail: () => {
        this.goWalletPage();
      },
    });
  },

  goReturnStorePage() {
    const returnStoreId = Number(this.data.returnStoreId || 0);
    if (!returnStoreId) {
      this.goBack();
      return;
    }

    const pages = getCurrentPages();
    for (let i = pages.length - 1; i >= 0; i -= 1) {
      if (pages[i].route === 'pages/store-detail/index') {
        const targetPage = pages[i] as Record<string, any>;
        targetPage._shouldRefreshAfterRecharge = true;
        const delta = pages.length - 1 - i;
        if (delta > 0) {
          wx.navigateBack({
            delta,
            fail: () => {
              this.redirectToReturnStore();
            },
          });
          return;
        }
      }
    }

    this.redirectToReturnStore();
  },

  redirectToReturnStore() {
    const returnStoreId = Number(this.data.returnStoreId || 0);
    const params = [
      returnStoreId ? `id=${returnStoreId}` : '',
      this.data.returnBayId ? `bayId=${this.data.returnBayId}` : '',
      this.data.returnDeviceId ? `deviceId=${this.data.returnDeviceId}` : '',
      'from=recharge',
    ]
      .filter(Boolean)
      .join('&');
    const url = `/pages/store-detail/index?${params}`;

    wx.redirectTo({
      url,
      fail: () => {
        wx.navigateTo({
          url,
          fail: () => {
            wx.reLaunch({ url: '/pages/service/index' });
          },
        });
      },
    });
  },

  goWalletPage() {
    const pages = getCurrentPages();
    let walletIndex = -1;

    for (let i = pages.length - 1; i >= 0; i -= 1) {
      if (pages[i].route === 'pages/wallet/index') {
        walletIndex = i;
        break;
      }
    }

    if (walletIndex >= 0) {
      const delta = pages.length - 1 - walletIndex;
      if (delta > 0) {
        wx.navigateBack({
          delta,
          fail: () => {
            wx.redirectTo({ url: '/pages/wallet/index' });
          },
        });
        return;
      }
    }

    wx.redirectTo({
      url: '/pages/wallet/index',
      fail: () => {
        wx.navigateTo({
          url: '/pages/wallet/index',
          fail: () => {
            wx.reLaunch({ url: '/pages/home/index' });
          },
        });
      },
    });
  },
});
