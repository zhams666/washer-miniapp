import { getStoreList } from '../../apis/store';

type StoreCardItem = {
  id: number;
  name: string;
  distance: string;
  businessHours: string;
  status: string;
  statusType: string;
  desc: string;
  notice: string;
  image: string;
};

Page({
  data: {
    stores: [] as StoreCardItem[],
  },

  onLoad() {
    this.loadStores();
  },

  async loadStores() {
    try {
      const pageData = await getStoreList(1, 10);
      const records = Array.isArray(pageData?.records) ? pageData.records : [];

      this.setData({
        stores: records.map((item: Record<string, any>) => this.mapStoreItem(item)),
      });
    } catch (error) {
      wx.showToast({
        title: 'Load failed',
        icon: 'none',
      });
      console.error('loadStores error:', error);
    }
  },

  mapStoreItem(item: Record<string, any>): StoreCardItem {
    const isOpen = Number(item.storeStatus) === 1;

    return {
      id: Number(item.id || 0),
      name: item.storeName || 'Unnamed Store',
      distance: 'N/A',
      businessHours: item.businessHours || 'Not set',
      status: isOpen ? 'Open' : 'Closed',
      statusType: isOpen ? 'open' : 'busy',
      desc: item.address || 'Address not set',
      notice: item.contactPhone
        ? `Phone: ${item.contactPhone}`
        : 'Phone not set',
      image: '/assets/images/washing.png',
    };
  },

  navigateStore(e: WechatMiniprogram.TouchEvent) {
    const { name } = e.currentTarget.dataset;
    wx.showToast({
      title: `${name} nav pending`,
      icon: 'none',
    });
  },
});
