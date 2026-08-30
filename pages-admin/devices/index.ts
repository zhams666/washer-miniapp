import {
  getMiniAdminDevices,
  getMiniAdminStores,
  startMiniAdminDevice,
  stopMiniAdminDevice,
} from '../../apis/admin';
import { ensureAdminToken } from '../../utils/admin-auth';

const statusMap: Record<string, string> = {
  online: '在线',
  offline: '离线',
  running: '运行中',
  idle: '空闲',
  paused: '暂停',
  fault: '故障',
  disabled: '停用',
};

Page({
  data: {
    loading: false,
    operatingId: 0,
    stores: [] as any[],
    storePickerOptions: ['全部门店'] as string[],
    selectedStoreIndex: 0,
    selectedStoreId: '',
    keyword: '',
    devices: [] as any[],
  },

  onLoad() {
    this.loadInitial();
  },

  async loadInitial() {
    try {
      ensureAdminToken();
    } catch (error) {
      return;
    }
    const stores = await getMiniAdminStores().catch(() => []);
    this.setData({
      stores,
      storePickerOptions: ['全部门店'].concat(stores.map((store) => store.storeName || `门店${store.id}`)),
    });
    this.loadDevices();
  },

  async loadDevices() {
    this.setData({ loading: true });
    try {
      const devices = await getMiniAdminDevices({
        storeId: this.data.selectedStoreId || undefined,
        keyword: this.data.keyword || undefined,
      });
      this.setData({
        devices: devices.map((device) => ({
          ...device,
          displayStatus: statusMap[String(device.deviceStatus || device.status || '').toLowerCase()] || device.deviceStatus || '未知',
          statusClass: String(device.deviceStatus || device.status || 'idle').toLowerCase(),
        })),
      });
    } catch (error) {
      console.error('load mini admin devices failed:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  handleStoreChange(e: WechatMiniprogram.PickerChange) {
    const selectedStoreIndex = Number(e.detail.value || 0);
    const store = selectedStoreIndex > 0 ? this.data.stores[selectedStoreIndex - 1] : null;
    this.setData({
      selectedStoreIndex,
      selectedStoreId: store && store.id ? String(store.id) : '',
    });
    this.loadDevices();
  },

  handleKeywordInput(e: WechatMiniprogram.Input) {
    this.setData({
      keyword: e.detail.value,
    });
  },

  handleSearch() {
    this.loadDevices();
  },

  async handleStart(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0);
    if (!id) return;
    this.setData({ operatingId: id });
    try {
      await startMiniAdminDevice(id);
      wx.showToast({ title: '已启动', icon: 'success' });
      await this.loadDevices();
    } catch (error) {
      console.error('start device failed:', error);
    } finally {
      this.setData({ operatingId: 0 });
    }
  },

  async handleStop(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0);
    if (!id) return;
    this.setData({ operatingId: id });
    try {
      await stopMiniAdminDevice(id);
      wx.showToast({ title: '已停止', icon: 'success' });
      await this.loadDevices();
    } catch (error) {
      console.error('stop device failed:', error);
    } finally {
      this.setData({ operatingId: 0 });
    }
  },
});
