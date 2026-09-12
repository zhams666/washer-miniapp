import {
  createMiniAdminDevice,
  getMiniAdminDevices,
  getMiniAdminStores,
  operateMiniAdminDevice,
  updateMiniAdminDeviceConfig,
} from '../../apis/admin';
import { ensureAdminToken, getCachedAdminProfile } from '../../utils/admin-auth';

const statusMap: Record<string, string> = {
  online: '在线',
  offline: '离线',
  running: '运行中',
  idle: '空闲',
  paused: '暂停',
  fault: '故障',
  disabled: '停用',
};

const doorMap: Record<string, string> = {
  open: '门已开',
  closed: '门已关',
};

const powerMap: Record<string, string> = {
  on: '已开电',
  off: '已关电',
};

const actionTitleMap: Record<string, string> = {
  open_door: '一键开门',
  close_door: '一键关门',
  power_on: '开电',
  power_off: '关电',
  maintenance: '维护',
};

const emptyEditForm = () => ({
  deviceCode: '',
  deviceName: '',
  deviceStatus: 'offline',
  baseTimeMinutes: '',
  basePrice: '',
  overtimePrice: '',
  speakerSn: '',
  speakerVersion: '',
  cabinetName: '',
});

const emptyCreateForm = () => ({
  deviceCode: '',
  deviceName: '',
  deviceType: 'washer',
  deviceRole: 'main',
  deviceStatus: 'offline',
  protocolType: '',
  firmwareVersion: '',
  remark: '',
});

Page({
  data: {
    loading: false,
    saving: false,
    operatingId: 0,
    stores: [] as any[],
    storePickerOptions: ['全部门店'] as string[],
    selectedStoreIndex: 0,
    selectedStoreId: '',
    keyword: '',
    devices: [] as any[],
    editVisible: false,
    editDeviceId: 0,
    editStatusIndex: 0,
    editForm: emptyEditForm(),
    createVisible: false,
    creating: false,
    createStoreIndex: 0,
    createTypeIndex: 0,
    createRoleIndex: 0,
    createStatusIndex: 0,
    createStorePickerOptions: [] as string[],
    createForm: emptyCreateForm(),
    deviceTypeOptions: ['washer', 'controller', 'gateway'],
    deviceRoleOptions: ['main', 'assistant'],
    deviceStatusOptions: ['offline', 'idle', 'running', 'paused', 'fault', 'disabled'],
    canCreateDevice: false,
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
    const profile = getCachedAdminProfile();
    const stores = await getMiniAdminStores().catch(() => []);
    this.setData({
      canCreateDevice: String(profile && profile.roleCode || '').toLowerCase() === 'store_manager',
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
        devices: devices.map((device) => this.formatDevice(device)),
      });
    } catch (error) {
      console.error('load mini admin devices failed:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  formatDevice(device: any) {
    const status = String(device.deviceStatus || device.status || 'idle').toLowerCase();
    const doorState = String(device.doorState || '').toLowerCase();
    const powerState = String(device.powerState || '').toLowerCase();
    return {
      ...device,
      displayStatus: statusMap[status] || device.deviceStatus || '未知',
      statusClass: status,
      doorStateLabel: doorMap[doorState] || '门状态待同步',
      powerStateLabel: powerMap[powerState] || '电源待同步',
      maintenanceLabel: device.maintenanceMode ? '维护中' : '可服务',
      agentLevel: device.agentLevel || '1级代理',
      contactName: device.contactName || device.storeName || '',
      contactPhone: device.contactPhone || '',
      cabinetName: device.cabinetName || '',
      speakerSn: device.speakerSn || '',
      speakerVersion: device.speakerVersion || device.firmwareVersion || '',
    };
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

  async handleDeviceAction(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0);
    const action = String(e.currentTarget.dataset.action || '');
    if (!id || !action) return;
    this.setData({ operatingId: id });
    try {
      await operateMiniAdminDevice(id, action);
      wx.showToast({ title: actionTitleMap[action] || '操作成功', icon: 'success' });
      await this.loadDevices();
    } catch (error) {
      console.error('operate device failed:', error);
    } finally {
      this.setData({ operatingId: 0 });
    }
  },

  openEdit(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0);
    const device = this.data.devices.find((item) => Number(item.id) === id);
    if (!device) return;
    const deviceStatus = String(device.deviceStatus || device.status || 'offline').toLowerCase();
    const editStatusIndex = Math.max(0, this.data.deviceStatusOptions.indexOf(deviceStatus));
    this.setData({
      editVisible: true,
      editDeviceId: id,
      editStatusIndex,
      editForm: {
        deviceCode: device.deviceCode || '',
        deviceName: device.deviceName || '',
        deviceStatus,
        baseTimeMinutes: device.baseTimeMinutes === undefined || device.baseTimeMinutes === null
          ? ''
          : String(device.baseTimeMinutes),
        basePrice: device.basePrice === undefined || device.basePrice === null ? '' : String(device.basePrice),
        overtimePrice: device.overtimePrice === undefined || device.overtimePrice === null
          ? ''
          : String(device.overtimePrice),
        speakerSn: device.speakerSn || '',
        speakerVersion: device.speakerVersion || '',
        cabinetName: device.cabinetName || '',
      },
    });
  },

  closeEdit() {
    if (this.data.saving) return;
    this.setData({
      editVisible: false,
      editDeviceId: 0,
      editStatusIndex: 0,
      editForm: emptyEditForm(),
    });
  },

  openCreate() {
    const stores = this.data.stores;
    if (!stores.length) {
      wx.showToast({ title: '暂无可管理门店', icon: 'none' });
      return;
    }
    const selectedStoreId = Number(this.data.selectedStoreId || 0);
    const matchedIndex = stores.findIndex((store) => Number(store.id) === selectedStoreId);
    this.setData({
      createVisible: true,
      createStoreIndex: matchedIndex >= 0 ? matchedIndex : 0,
      createTypeIndex: 0,
      createRoleIndex: 0,
      createStatusIndex: 0,
      createStorePickerOptions: stores.map((store) => store.storeName || `门店${store.id}`),
      createForm: emptyCreateForm(),
    });
  },

  closeCreate() {
    if (this.data.creating) return;
    this.setData({
      createVisible: false,
      createStoreIndex: 0,
      createTypeIndex: 0,
      createRoleIndex: 0,
      createStatusIndex: 0,
      createStorePickerOptions: [],
      createForm: emptyCreateForm(),
    });
  },

  noop() {},

  handleEditInput(e: WechatMiniprogram.Input) {
    const field = String(e.currentTarget.dataset.field || '');
    if (!field) return;
    this.setData({
      [`editForm.${field}`]: e.detail.value,
    });
  },

  handleEditStatusChange(e: WechatMiniprogram.PickerChange) {
    const editStatusIndex = Number(e.detail.value || 0);
    const deviceStatus = this.data.deviceStatusOptions[editStatusIndex] || 'offline';
    this.setData({
      editStatusIndex,
      'editForm.deviceStatus': deviceStatus,
    });
  },

  handleCreateInput(e: WechatMiniprogram.Input) {
    const field = String(e.currentTarget.dataset.field || '');
    if (!field) return;
    this.setData({
      [`createForm.${field}`]: e.detail.value,
    });
  },

  handleCreateStoreChange(e: WechatMiniprogram.PickerChange) {
    this.setData({
      createStoreIndex: Number(e.detail.value || 0),
    });
  },

  handleCreateSelect(e: WechatMiniprogram.PickerChange) {
    const field = String(e.currentTarget.dataset.field || '');
    const optionMap: Record<string, string[]> = {
      deviceType: this.data.deviceTypeOptions,
      deviceRole: this.data.deviceRoleOptions,
      deviceStatus: this.data.deviceStatusOptions,
    };
    const indexMap: Record<string, string> = {
      deviceType: 'createTypeIndex',
      deviceRole: 'createRoleIndex',
      deviceStatus: 'createStatusIndex',
    };
    const index = Number(e.detail.value || 0);
    const value = optionMap[field] && optionMap[field][index];
    if (!field || !value) return;
    this.setData({
      [`createForm.${field}`]: value,
      [indexMap[field]]: index,
    });
  },

  async handleCreateDevice() {
    const store = this.data.stores[Number(this.data.createStoreIndex || 0)];
    const form = this.data.createForm;
    if (!store || !Number(store.id)) {
      wx.showToast({ title: '请选择归属门店', icon: 'none' });
      return;
    }
    if (!String(form.deviceName || '').trim()) {
      wx.showToast({ title: '请填写设备名称', icon: 'none' });
      return;
    }
    this.setData({ creating: true });
    try {
      await createMiniAdminDevice({
        storeId: Number(store.id),
        deviceCode: String(form.deviceCode || '').trim(),
        deviceName: String(form.deviceName || '').trim(),
        deviceType: String(form.deviceType || 'washer'),
        deviceRole: String(form.deviceRole || 'main'),
        deviceStatus: String(form.deviceStatus || 'offline'),
        protocolType: String(form.protocolType || '').trim(),
        firmwareVersion: String(form.firmwareVersion || '').trim(),
        remark: String(form.remark || '').trim(),
      });
      wx.showToast({ title: '设备已新增', icon: 'success' });
      this.closeCreate();
      await this.loadDevices();
    } catch (error) {
      console.error('create mini admin device failed:', error);
    } finally {
      this.setData({ creating: false });
    }
  },

  async handleSaveEdit() {
    const id = Number(this.data.editDeviceId || 0);
    const form = this.data.editForm;
    if (!id) return;
    if (!String(form.deviceCode || '').trim() || !String(form.deviceName || '').trim()) {
      wx.showToast({ title: '请填写设备号和设备名称', icon: 'none' });
      return;
    }
    this.setData({ saving: true });
    try {
      await updateMiniAdminDeviceConfig(id, {
        deviceCode: String(form.deviceCode || '').trim(),
        deviceName: String(form.deviceName || '').trim(),
        deviceStatus: String(form.deviceStatus || 'offline').trim().toLowerCase(),
        baseTimeMinutes: this.toOptionalNumber(form.baseTimeMinutes),
        basePrice: this.toOptionalNumber(form.basePrice),
        overtimePrice: this.toOptionalNumber(form.overtimePrice),
        speakerSn: String(form.speakerSn || '').trim(),
        speakerVersion: String(form.speakerVersion || '').trim(),
        cabinetName: String(form.cabinetName || '').trim(),
      });
      wx.showToast({ title: '已保存', icon: 'success' });
      this.closeEdit();
      await this.loadDevices();
    } catch (error) {
      console.error('save device config failed:', error);
    } finally {
      this.setData({ saving: false });
    }
  },

  showCabinetList(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0);
    const device = this.data.devices.find((item) => Number(item.id) === id);
    wx.showModal({
      title: '柜子列表',
      content: device && device.cabinetName ? `已绑定：${device.cabinetName}` : '暂未绑定柜子',
      showCancel: false,
    });
  },

  showRecognizerInfo(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0);
    const device = this.data.devices.find((item) => Number(item.id) === id);
    wx.showModal({
      title: '识别器信息',
      content: device
        ? `协议：${device.protocolType || '未填写'}\n版本：${device.speakerVersion || device.firmwareVersion || '未填写'}`
        : '暂无识别器信息',
      showCancel: false,
    });
  },

  toOptionalNumber(value: string) {
    const text = String(value || '').trim();
    if (!text) return undefined;
    const number = Number(text);
    return Number.isFinite(number) ? number : undefined;
  },
});
