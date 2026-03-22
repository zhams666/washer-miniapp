Page({
  data: {
    stores: [
      {
        id: 1,
        name: '橙洗自助洗车·城南店',
        distance: '1.2km',
        businessHours: '00:00 - 24:00',
        status: '营业中',
        statusType: 'open',
        desc: '提供高压冲洗、泡沫清洁、吸尘服务',
        notice: '门店车位充足，支持全天自助使用',
        image: '/assets/images/washing.png',
      },
      {
        id: 2,
        name: '橙洗自助洗车·大学城店',
        distance: '2.8km',
        businessHours: '06:00 - 23:00',
        status: '空闲',
        statusType: 'idle',
        desc: '适合日常洗车，晚间客流较少',
        notice: '新用户到店可领取洗车优惠券',
        image: '/assets/images/washing.png',
      },
      {
        id: 3,
        name: '橙洗自助洗车·城北店',
        distance: '4.6km',
        businessHours: '08:00 - 22:30',
        status: '忙碌',
        statusType: 'busy',
        desc: '高峰时段请耐心排队，现场有引导员',
        notice: '周末下午较繁忙，建议错峰到店',
        image: '/assets/images/washing.png',
      },
    ],
  },

  navigateStore(e: WechatMiniprogram.TouchEvent) {
    const { name } = e.currentTarget.dataset;
    wx.showToast({
      title: `${name}导航待接入`,
      icon: 'none',
    });
  },
});
