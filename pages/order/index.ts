import { getDeviceList } from '../../apis/device';
import {
  completeOrder,
  createSimpleOrder,
  getSimpleOrderList,
  startOrder,
} from '../../apis/order';

type OrderCardItem = {
  id: number;
  storeName: string;
  createTime: string;
  amount: string;
  status: string;
  statusType: string;
  orderStatus: string;
  actionText: string;
  actionType: string;
};

type DeviceOption = {
  id: number;
  label: string;
};

Page({
  data: {
    list: [] as OrderCardItem[],
    loading: false,
    storeId: 1,
    deviceOptions: [] as DeviceOption[],
    devicePickerRange: [] as string[],
    selectedDeviceIndex: 0,
    selectedDeviceId: 0,
  },

  onLoad() {
    this.loadOrders();
    this.loadDevices();
  },

  async loadOrders() {
    this.setData({ loading: true });

    try {
      const orders = await getSimpleOrderList(1, 10);
      this.setData({
        list: orders.map((item: Record<string, any>) => this.mapOrderItem(item)),
        loading: false,
      });
    } catch (error) {
      this.setData({ loading: false });
      wx.showToast({
        title: 'Load failed',
        icon: 'none',
      });
      console.error('loadOrders error:', error);
    }
  },

  async loadDevices() {
    try {
      const devices = await getDeviceList(this.data.storeId);
      const deviceOptions = devices.map((item: Record<string, any>) => ({
        id: Number(item.id || 0),
        label: this.getDeviceLabel(item),
      }));

      this.setData({
        deviceOptions: deviceOptions,
        devicePickerRange: deviceOptions.map((item: DeviceOption) => item.label),
        selectedDeviceIndex: 0,
        selectedDeviceId: deviceOptions.length > 0 ? deviceOptions[0].id : 0,
      });
    } catch (error) {
      wx.showToast({
        title: 'Load devices failed',
        icon: 'none',
      });
      console.error('loadDevices error:', error);
    }
  },

  getDeviceLabel(item: Record<string, any>) {
    const code = item.deviceCode || 'NoCode';
    const name = item.deviceName || 'Unnamed Device';
    const status = item.status || 'unknown';
    return `${name} (${code}) - ${status}`;
  },

  onDeviceChange(e: WechatMiniprogram.PickerChange) {
    const index = Number(e.detail.value || 0);
    const deviceOptions = this.data.deviceOptions as DeviceOption[];
    const selectedDevice = deviceOptions[index];

    this.setData({
      selectedDeviceIndex: index,
      selectedDeviceId: selectedDevice ? selectedDevice.id : 0,
    });
  },

  mapOrderItem(item: Record<string, any>): OrderCardItem {
    const statusMap: Record<string, { text: string; type: string; actionText: string; actionType: string }> = {
      pending: { text: 'Pending', type: 'pending', actionText: 'Start Order', actionType: 'start' },
      running: { text: 'Running', type: 'doing', actionText: 'Finish Order', actionType: 'complete' },
      completed: { text: 'Completed', type: 'done', actionText: 'View Detail', actionType: 'detail' },
    };

    const statusInfo = statusMap[item.orderStatus] || {
      text: item.orderStatus || 'Unknown',
      type: 'cancel',
      actionText: 'View Detail',
      actionType: 'detail',
    };

    return {
      id: Number(item.id || 0),
      storeName: item.storeName || 'Unknown Store',
      createTime: this.formatTime(item.createdAt),
      amount: Number(item.finalAmount || 0).toFixed(2),
      status: statusInfo.text,
      statusType: statusInfo.type,
      orderStatus: item.orderStatus || 'pending',
      actionText: statusInfo.actionText,
      actionType: statusInfo.actionType,
    };
  },

  formatTime(value: string) {
    if (!value) {
      return 'N/A';
    }
    return value.replace('T', ' ').slice(0, 16);
  },

  async createTestOrder() {
    try {
      await createSimpleOrder({
        userId: 1,
        storeId: this.data.storeId,
        deviceId: this.data.selectedDeviceId || null,
        payMode: 'wallet',
        finalAmount: 18,
        remark: 'miniapp test order',
      });

      wx.showToast({
        title: 'Created',
        icon: 'success',
      });

      this.loadOrders();
    } catch (error) {
      wx.showToast({
        title: 'Create failed',
        icon: 'none',
      });
      console.error('createTestOrder error:', error);
    }
  },

  async handleOrderAction(e: WechatMiniprogram.TouchEvent) {
    const { id, action } = e.currentTarget.dataset as { id: number; action: string };

    try {
      if (action === 'start') {
        await startOrder(Number(id));
        wx.showToast({
          title: 'Started',
          icon: 'success',
        });
        this.loadOrders();
        return;
      }

      if (action === 'complete') {
        await completeOrder(Number(id));
        wx.showToast({
          title: 'Completed',
          icon: 'success',
        });
        this.loadOrders();
        return;
      }

      this.showOrderDetail(Number(id));
    } catch (error) {
      wx.showToast({
        title: 'Action failed',
        icon: 'none',
      });
      console.error('handleOrderAction error:', error);
    }
  },

  handleViewDetail(e: WechatMiniprogram.TouchEvent) {
    const { id } = e.currentTarget.dataset as { id: number };
    this.showOrderDetail(Number(id));
  },

  showOrderDetail(id: number) {
    if (!id) {
      wx.showToast({
        title: 'Invalid order',
        icon: 'none',
      });
      return;
    }

    wx.navigateTo({
      url: `/pages/detail/index?id=${id}`,
    });
  },
});
