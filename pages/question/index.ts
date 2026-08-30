Page({
  data: {
    docs: [
      {
        step: '01',
        icon: '/assets/icons/market.png',
        title: '选择门店进场',
        duration: '进场前',
        content: '在门店页选择附近可用门店，确认空闲工位后驶入对应洗车位。',
        tip: '先看空闲工位，再开始订单。',
      },
      {
        step: '02',
        icon: '/assets/icons/scan.png',
        title: '扫码或开始洗车',
        duration: '1 分钟',
        content: '在小程序内开始洗车，或扫描现场设备二维码接入当前工位。',
        tip: '确保车停稳后再开启设备。',
      },
      {
        step: '03',
        icon: '/assets/icons/car.png',
        title: '冲洗与泡沫清洁',
        duration: '8-12 分钟',
        content: '先用清水冲掉泥沙，再均匀喷洒泡沫，最后从上到下冲净车身。',
        tip: '水枪不要长时间停留在同一位置。',
      },
      {
        step: '04',
        icon: '/assets/icons/order.png',
        title: '结束订单离场',
        duration: '完成后',
        content: '确认设备已关闭，收好水枪和吸尘管，查看订单状态后驶离工位。',
        tip: '离场前检查随身物品和车窗。',
        isLast: true,
      },
    ],
  },

  goService() {
    wx.switchTab({
      url: '/pages/service/index',
    });
  },

  goHome() {
    wx.switchTab({
      url: '/pages/home/index',
    });
  },
});
