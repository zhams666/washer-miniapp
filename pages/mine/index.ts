Page({
  data: {
    userInfo: {
      nickName: '车主小王',
      avatarUrl: '/assets/icons/user.png',
      memberText: '普通会员，可享门店储值与优惠券服务',
    },
    stats: [
      { label: '商户数', value: '3' },
      { label: '总余额', value: '68.00' },
      { label: '洗车次数', value: '12' },
      { label: '卡券数', value: '4' },
      { label: '积分', value: '286' },
    ],
    menus: [
      { key: 'order', title: '洗车订单', icon: '/assets/icons/order.png' },
      { key: 'merchant', title: '我的商户', icon: '/assets/icons/market.png' },
      { key: 'wallet', title: '消费明细', icon: '/assets/icons/wallet.png' },
      { key: 'car', title: '我的车辆', icon: '/assets/icons/car.png' },
      { key: 'profile', title: '个人信息', icon: '/assets/icons/user.png' },
      { key: 'service', title: '客服中心', icon: '/assets/icons/service.png' },
      { key: 'coupon', title: '我的卡券', icon: '/assets/icons/discount.png' },
    ],
  },

  handleHeaderAction() {
    wx.showToast({
      title: '设置功能待接入',
      icon: 'none',
    });
  },

  handleMenuTap(e: WechatMiniprogram.TouchEvent) {
    const { key } = e.currentTarget.dataset;

    if (key === 'order') {
      wx.switchTab({
        url: '/pages/order/index',
      });
      return;
    }

    if (key === 'wallet') {
      wx.navigateTo({
        url: '/pages/wallet/index',
      });
      return;
    }

    if (key === 'service' || key === 'merchant') {
      wx.switchTab({
        url: '/pages/service/index',
      });
      return;
    }

    if (key === 'coupon') {
      wx.navigateTo({
        url: '/pages/discount/index',
      });
      return;
    }

    wx.showToast({
      title: '功能待接入',
      icon: 'none',
    });
  },
});
