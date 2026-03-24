Page({
  data: {
    list: [
      {
        price: '',
        type: 0,
        name: 'Free Wash Coupon',
        description: 'Experience coupon',
        use: 'Universal',
        time: '2026.03.01-2026.12.31',
      },
    ],
  },

  goExchange() {
    wx.navigateTo({
      url: '/pages/exchange/index',
    });
  },
});
