import { REQUEST_URL } from '../../config/url';

Page({
  data: {
    storeInfo: {
      name: '橙洗自助洗车',
      desc: '24小时自助洗车，随到随洗',
      notice: '新用户首单立减 5 元，店内提供洗车液和吸尘设备',
    },
    featureCards: [
      {
        key: 'start',
        title: '开始洗车',
        desc: '直接进入洗车流程',
        icon: '/assets/icons/car.png',
        badge: '推荐',
      },
      {
        key: 'scan',
        title: '扫码接入',
        desc: '扫码连接门店设备',
        icon: '/assets/icons/scan.png',
        badge: '快捷',
      },
    ],
    quickActions: [
      {
        key: 'store',
        title: '门店导航',
        desc: '查看附近门店',
        icon: '/assets/icons/market.png',
      },
      {
        key: 'order',
        title: '洗车订单',
        desc: '查看订单记录',
        icon: '/assets/icons/order.png',
      },
      {
        key: 'wallet',
        title: '余额充值',
        desc: '账户充值更方便',
        icon: '/assets/icons/wallet.png',
      },
      {
        key: 'mine',
        title: '个人中心',
        desc: '查看账户信息',
        icon: '/assets/icons/user.png',
      },
    ],
    pingLoading: false,
    pingSuccess: false,
    pingMessage: '未请求',
    pingRaw: '',
  },

  onLoad() {
    this.loadPing();
  },

  loadPing() {
    this.setData({
      pingLoading: true,
      pingSuccess: false,
      pingMessage: '请求中...',
      pingRaw: '',
    });

    wx.request({
      url: `${REQUEST_URL}/ping`,
      method: 'GET',
      success: (res) => {
        const responseData = (res.data || {}) as Record<string, any>;
        const success = res.statusCode === 200 && responseData.code === 0;

        this.setData({
          pingLoading: false,
          pingSuccess: success,
          pingMessage: success
            ? `请求成功：${responseData?.data?.message || 'ok'}`
            : `请求失败：HTTP ${res.statusCode}`,
          pingRaw: JSON.stringify(responseData, null, 2),
        });
      },
      fail: (error) => {
        this.setData({
          pingLoading: false,
          pingSuccess: false,
          pingMessage: '请求失败，请检查后端地址、端口和合法域名配置',
          pingRaw: JSON.stringify(error, null, 2),
        });
      },
    });
  },

  startWash() {
    this.goWashing();
  },

  scan() {
    this.goWashing();
  },

  goWashing() {
    wx.navigateTo({
      url: '/pages/washing/index?deviceNo=XC-STORE-001',
    });
  },

  handleFeatureTap(e: WechatMiniprogram.TouchEvent) {
    const { key } = e.currentTarget.dataset;
    if (key === 'start') {
      this.startWash();
      return;
    }

    if (key === 'scan') {
      this.scan();
    }
  },

  handleQuickAction(e: WechatMiniprogram.TouchEvent) {
    const { key } = e.currentTarget.dataset;

    if (key === 'wallet') {
      wx.navigateTo({
        url: '/pages/wallet/index',
      });
      return;
    }

    const tabMap: Record<string, string> = {
      store: '/pages/service/index',
      order: '/pages/order/index',
      mine: '/pages/mine/index',
    };

    const url = tabMap[key];
    if (!url) return;

    wx.switchTab({ url });
  },
});
