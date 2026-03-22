Page({
  data: {
    list: [] as Array<{
      id: number;
      storeName: string;
      createTime: string;
      amount: string;
      status: string;
      statusType: string;
    }>,
    mockOrders: [
      {
        id: 1,
        storeName: '橙洗自助洗车·城南店',
        createTime: '2026-03-21 15:20',
        amount: '18.00',
        status: '已完成',
        statusType: 'done',
      },
      {
        id: 2,
        storeName: '橙洗自助洗车·大学城店',
        createTime: '2026-03-19 09:30',
        amount: '12.00',
        status: '进行中',
        statusType: 'doing',
      },
    ],
  },

  showOrderDetail() {
    wx.showToast({
      title: '订单详情待接入',
      icon: 'none',
    });
  },
});
