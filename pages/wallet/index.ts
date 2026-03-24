Page({
  data: {
    walletNum: '256.00',
    currentNav: 0,
    list: [
      {
        num: 100.0,
        createTime: '2026.03.24 16:27:23',
      },
      {
        num: 100.0,
        createTime: '2026.03.17 12:25:20',
      },
      {
        num: 200.0,
        createTime: '2026.03.10 16:27:23',
      },
    ],
  },

  changeType() {
    this.setData({
      currentNav: this.data.currentNav == 0 ? 1 : 0,
    });
  },

  goPay() {
    wx.navigateTo({
      url: '/pages/pay/index',
    });
  },
});
