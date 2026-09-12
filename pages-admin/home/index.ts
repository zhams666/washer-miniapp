import {
  getMiniAdminCurrent,
  getMiniAdminOperationOverview,
  getMiniAdminStoreSettings,
  updateMiniAdminStoreSettings,
  uploadMiniAdminStoreImage,
} from '../../apis/admin';
import { clearAdminSession, ensureAdminToken, setAdminProfile } from '../../utils/admin-auth';

const formatMoney = (value: any) => Number(value || 0).toFixed(2);
const formatCount = (value: any) => String(Number(value || 0));
const emptySettingsForm = () => ({
  storeName: '',
  province: '',
  city: '',
  district: '',
  address: '',
  contactName: '',
  contactPhone: '',
  coverImage: '',
  doorCloseIntervalOneStart: '',
  doorCloseIntervalOneEnd: '',
  doorCloseIntervalTwoStart: '',
  doorCloseIntervalTwoEnd: '',
  registerRewardAmount: '',
  inviteRewardAmount: '',
  activityIntro: '',
  rechargeDescription: '',
  memberDescription: '',
  cabinetMinRechargeAmount: '',
  cabinetMinBalanceAmount: '',
});

Page({
  data: {
    loading: false,
    profile: null as any,
    workspace: {
      tierCode: 'store',
      tierName: '门店店长',
      headline: '门店移动工作台',
      description: '聚焦本店设备、订单、流水和现场处理。',
      scopeName: '',
    },
    stores: [] as any[],
    storePickerOptions: ['全部门店'] as string[],
    selectedStoreIndex: 0,
    selectedStoreId: '',
    scopeSummaryCards: [] as any[],
    metricCards: [] as any[],
    deviceStatus: null as any,
    alerts: [] as any[],
    storeRankings: [] as any[],
    recentActivities: [] as any[],
    quickActions: [] as any[],
    settingsVisible: false,
    settingsLoading: false,
    settingsSaving: false,
    uploadingStoreImage: false,
    settingsStoreId: 0,
    settingsForm: emptySettingsForm(),
  },

  onLoad() {
    this.loadPage();
  },

  async loadPage() {
    try {
      ensureAdminToken();
    } catch (error) {
      return;
    }

    this.setData({ loading: true });
    let phase = '校验管理端登录';
    try {
      const profile = await getMiniAdminCurrent();
      setAdminProfile(profile);
      phase = '加载经营数据';
      const overview = await getMiniAdminOperationOverview({
        storeId: this.data.selectedStoreId || undefined,
      });
      const stores = Array.isArray(overview.stores) ? overview.stores : [];
      this.setData({
        profile,
        stores,
        workspace: this.mapWorkspace(overview),
        storePickerOptions: this.buildStorePickerOptions(overview.tierCode, stores),
        scopeSummaryCards: this.mapScopeSummary(overview.scopeSummary || []),
        metricCards: this.mapMetrics(overview.metrics || []),
        deviceStatus: overview.deviceStatus || {},
        alerts: (overview.deviceStatus && overview.deviceStatus.alerts) || [],
        storeRankings: this.mapStoreRankings(overview.storeRankings || []),
        recentActivities: this.mapRecentActivities(overview.recentActivities || []),
        quickActions: this.buildQuickActions(overview.tierCode, profile.roleCode),
      });
    } catch (error) {
      const diagnostic = this.resolveLoadDiagnostic(error, phase);
      console.error('mini_admin_home_load_failed', {
        phase,
        traceId: diagnostic.traceId,
        selectedStoreId: this.data.selectedStoreId || '',
        message: diagnostic.message,
        error,
      });
      wx.showModal({
        title: '管理端数据加载失败',
        content: `步骤：${phase}\n诊断编号：${diagnostic.traceId}\n原因：${diagnostic.message}\n请在云托管日志搜索该编号。`,
        showCancel: false,
        confirmText: '知道了',
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  resolveLoadDiagnostic(error: unknown, phase: string) {
    const record = error && typeof error === 'object' ? (error as Record<string, any>) : {};
    const traceId = String(record.traceId || record.requestId || '未返回追踪编号').trim();
    const rawMessage = record.message || record.errMsg || record.msg || error || `${phase}失败`;
    const message = String(rawMessage)
      .replace(/[\r\n]+/g, ' ')
      .replace(/\s+/g, ' ')
      .trim()
      .slice(0, 160);
    return {
      traceId: traceId || '未返回追踪编号',
      message: message || `${phase}失败`,
    };
  },

  mapWorkspace(overview: any) {
    const tierCode = String(overview.tierCode || 'store');
    return {
      tierCode,
      tierName: overview.tierName || this.resolveTierName(tierCode),
      headline: overview.headline || this.resolveHeadline(tierCode),
      description: overview.description || '',
      scopeName: overview.scopeName || '',
    };
  },

  buildStorePickerOptions(tierCode: string, stores: any[]) {
    const firstLabel =
      tierCode === 'platform'
        ? '全部门店'
        : tierCode === 'franchisee'
        ? '全部旗下门店'
        : '全部可管门店';
    return [firstLabel].concat(stores.map((store) => store.storeName || `门店${store.id}`));
  },

  mapScopeSummary(items: any[]) {
    return items.map((item) => ({
      key: item.key,
      title: item.title,
      value: formatCount(item.count),
      unit: item.unit || '',
      desc: item.description || '',
    }));
  },

  mapMetrics(metrics: any[]) {
    return metrics.map((metric) => {
      const isMoney = metric.unit === '元' || String(metric.key || '').toLowerCase().includes('amount');
      return {
        key: metric.key,
        title: metric.title,
        value: isMoney ? formatMoney(metric.amount) : formatCount(metric.count),
        unit: metric.unit || '',
        desc: metric.description || '',
      };
    });
  },

  mapStoreRankings(items: any[]) {
    return items.map((item, index) => ({
      ...item,
      rank: index + 1,
      consumeAmountText: formatMoney(item.consumeAmount),
      rechargeAmountText: formatMoney(item.rechargeAmount),
      washCountText: formatCount(item.washCount),
      abnormalText: formatCount(item.abnormalDeviceCount),
      deviceText: formatCount(item.totalDeviceCount),
    }));
  },

  mapRecentActivities(items: any[]) {
    return items.map((item) => ({
      ...item,
      amountText: formatMoney(item.amount),
    }));
  },

  buildQuickActions(tierCode: string, roleCode?: string) {
    if (tierCode === 'platform') {
      return [
        { key: 'devices', title: '全局设备', desc: '跨门店巡检异常', icon: '/assets/icons/device.png' },
        { key: 'orders', title: '订单总览', desc: '全部门店订单', icon: '/assets/icons/order.png' },
        { key: 'assets', title: '用户资产', desc: '余额、罚款、次卡', icon: '/assets/icons/wallet.png' },
        { key: 'finance', title: '经营流水', desc: '总部财务视角', icon: '/assets/icons/wallet.png' },
        { key: 'voucherGenerate', title: '生成兑换券', desc: '批量生成金额券码', icon: '/assets/icons/home-voucher.png' },
        { key: 'vouchers', title: '兑换券列表', desc: '查询核销和流水', icon: '/assets/icons/discount.png' },
        { key: 'features', title: '功能整理', desc: '需求和接入状态', icon: '/assets/icons/question.png' },
        { key: 'profile', title: '权限账号', desc: '总部管理权限', icon: '/assets/icons/user.png' },
      ];
    }
    if (tierCode === 'franchisee') {
      return [
        { key: 'devices', title: '门店设备', desc: '旗下门店状态', icon: '/assets/icons/device.png' },
        { key: 'orders', title: '订单排行', desc: '门店订单对比', icon: '/assets/icons/order.png' },
        { key: 'assets', title: '用户资产', desc: '加款、罚款、次卡', icon: '/assets/icons/wallet.png' },
        { key: 'finance', title: '分账流水', desc: '加盟财务概览', icon: '/assets/icons/wallet.png' },
        { key: 'voucherGenerate', title: '生成兑换券', desc: '批量生成金额券码', icon: '/assets/icons/home-voucher.png' },
        { key: 'vouchers', title: '兑换券列表', desc: '查询核销和流水', icon: '/assets/icons/discount.png' },
        { key: 'features', title: '功能整理', desc: '需求和接入状态', icon: '/assets/icons/question.png' },
        { key: 'profile', title: '加盟权限', desc: '账号与门店范围', icon: '/assets/icons/user.png' },
      ];
    }
    const actions = [
      { key: 'devices', title: '设备管理', desc: '状态、启停、异常', icon: '/assets/icons/device.png' },
      { key: 'orders', title: '订单查询', desc: '本店订单和支付', icon: '/assets/icons/order.png' },
      { key: 'assets', title: '用户资产', desc: '加钱、罚款、次卡', icon: '/assets/icons/wallet.png' },
      { key: 'finance', title: '流水中心', desc: '充值、消费、核销', icon: '/assets/icons/wallet.png' },
      { key: 'voucherGenerate', title: '生成兑换券', desc: '批量生成金额券码', icon: '/assets/icons/home-voucher.png' },
      { key: 'vouchers', title: '兑换券列表', desc: '查询核销和流水', icon: '/assets/icons/discount.png' },
      { key: 'features', title: '功能整理', desc: '需求和接入状态', icon: '/assets/icons/question.png' },
      { key: 'profile', title: '我的权限', desc: '账号、门店、角色', icon: '/assets/icons/user.png' },
    ];
    if (String(roleCode || '').toLowerCase() === 'store_manager') {
      actions.splice(1, 0, {
        key: 'rankingManagement',
        title: '排行榜管理',
        desc: '设置本店用户展示时长',
        icon: '/assets/icons/tab-ranking-active.png',
      });
    }
    return actions;
  },

  resolveTierName(tierCode: string) {
    if (tierCode === 'platform') return '总部';
    if (tierCode === 'franchisee') return '加盟老板';
    return '门店店长';
  },

  resolveHeadline(tierCode: string) {
    if (tierCode === 'platform') return '总部经营驾驶舱';
    if (tierCode === 'franchisee') return '加盟老板驾驶舱';
    return '门店移动工作台';
  },

  handleStoreChange(e: WechatMiniprogram.PickerChange) {
    const selectedStoreIndex = Number(e.detail.value || 0);
    const store = selectedStoreIndex > 0 ? this.data.stores[selectedStoreIndex - 1] : null;
    this.setData({
      selectedStoreIndex,
      selectedStoreId: store && store.id ? String(store.id) : '',
    });
    this.loadPage();
  },

  async openStoreSettings() {
    const storeId = this.resolveEditableStoreId();
    if (!storeId) {
      wx.showToast({ title: '请先切换到具体门店', icon: 'none' });
      return;
    }
    this.setData({
      settingsVisible: true,
      settingsLoading: true,
      settingsStoreId: storeId,
      settingsForm: emptySettingsForm(),
    });
    try {
      const settings = await getMiniAdminStoreSettings(storeId);
      this.setData({
        settingsForm: this.mapSettingsForm(settings),
      });
    } catch (error) {
      console.error('load store settings failed:', error);
    } finally {
      this.setData({ settingsLoading: false });
    }
  },

  closeStoreSettings() {
    if (this.data.settingsSaving || this.data.uploadingStoreImage) return;
    this.setData({
      settingsVisible: false,
      settingsStoreId: 0,
      settingsForm: emptySettingsForm(),
    });
  },

  noop() {},

  handleSettingsInput(e: WechatMiniprogram.Input) {
    const field = String(e.currentTarget.dataset.field || '');
    if (!field) return;
    this.setData({
      [`settingsForm.${field}`]: e.detail.value,
    });
  },

  chooseStoreImage() {
    const storeId = Number(this.data.settingsStoreId || 0);
    if (!storeId) return;
    wx.chooseImage({
      count: 1,
      sizeType: ['compressed'],
      sourceType: ['album', 'camera'],
      success: async (res) => {
        const path = res.tempFilePaths && res.tempFilePaths[0];
        if (!path) return;
        this.setData({ uploadingStoreImage: true });
        try {
          const coverImage = await uploadMiniAdminStoreImage(storeId, path);
          this.setData({
            'settingsForm.coverImage': coverImage,
          });
          wx.showToast({ title: '图片已上传', icon: 'success' });
        } catch (error) {
          wx.showToast({ title: '当前环境暂不支持上传', icon: 'none' });
          console.error('upload store image failed:', error);
        } finally {
          this.setData({ uploadingStoreImage: false });
        }
      },
    });
  },

  async handleSaveStoreSettings() {
    const storeId = Number(this.data.settingsStoreId || 0);
    const form = this.data.settingsForm;
    if (!storeId) return;
    if (!String(form.storeName || '').trim()) {
      wx.showToast({ title: '请填写场所名称', icon: 'none' });
      return;
    }
    this.setData({ settingsSaving: true });
    try {
      await updateMiniAdminStoreSettings(storeId, {
        storeName: String(form.storeName || '').trim(),
        province: String(form.province || '').trim(),
        city: String(form.city || '').trim(),
        district: String(form.district || '').trim(),
        address: String(form.address || '').trim(),
        contactName: String(form.contactName || '').trim(),
        contactPhone: String(form.contactPhone || '').trim(),
        coverImage: String(form.coverImage || '').trim(),
        doorCloseIntervalOneStart: this.toOptionalNumber(form.doorCloseIntervalOneStart),
        doorCloseIntervalOneEnd: this.toOptionalNumber(form.doorCloseIntervalOneEnd),
        doorCloseIntervalTwoStart: this.toOptionalNumber(form.doorCloseIntervalTwoStart),
        doorCloseIntervalTwoEnd: this.toOptionalNumber(form.doorCloseIntervalTwoEnd),
        registerRewardAmount: this.toOptionalNumber(form.registerRewardAmount),
        inviteRewardAmount: this.toOptionalNumber(form.inviteRewardAmount),
        activityIntro: String(form.activityIntro || '').trim(),
        rechargeDescription: String(form.rechargeDescription || '').trim(),
        memberDescription: String(form.memberDescription || '').trim(),
        cabinetMinRechargeAmount: this.toOptionalNumber(form.cabinetMinRechargeAmount),
        cabinetMinBalanceAmount: this.toOptionalNumber(form.cabinetMinBalanceAmount),
      });
      wx.showToast({ title: '门店设置已保存', icon: 'success' });
      this.closeStoreSettings();
      await this.loadPage();
    } catch (error) {
      console.error('save store settings failed:', error);
    } finally {
      this.setData({ settingsSaving: false });
    }
  },

  handleQuickTap(e: WechatMiniprogram.TouchEvent) {
    const key = e.currentTarget.dataset.key;
    const map: Record<string, string> = {
      devices: '/pages-admin/devices/index',
      rankingManagement: '/pages-admin/ranking-management/index',
      orders: '/pages-admin/orders/index',
      assets: '/pages-admin/assets/index',
      finance: '/pages-admin/finance/index',
      voucherGenerate: '/pages-admin/voucher-generate/index',
      vouchers: '/pages-admin/vouchers/index',
      features: '/pages-admin/features/index',
      profile: '/pages-admin/profile/index',
    };
    const url = map[String(key)];
    if (url) {
      wx.navigateTo({ url });
    }
  },

  handleMetricTap(e: WechatMiniprogram.TouchEvent) {
    const key = String(e.currentTarget.dataset.key || '');
    if (!key) {
      return;
    }
    const params = [
      `metricKey=${key}`,
      this.data.selectedStoreId ? `storeId=${this.data.selectedStoreId}` : '',
    ]
      .filter(Boolean)
      .join('&');
    wx.navigateTo({ url: `/pages-admin/finance/index?${params}` });
  },

  handleScopeTap(e: WechatMiniprogram.TouchEvent) {
    const key = String(e.currentTarget.dataset.key || '');
    const deviceKeys = ['devices', 'abnormalDevices'];
    if (deviceKeys.includes(key)) {
      wx.navigateTo({ url: '/pages-admin/devices/index' });
      return;
    }
    if (key === 'stores') {
      wx.navigateTo({ url: '/pages-admin/orders/index' });
      return;
    }
    wx.navigateTo({ url: '/pages-admin/profile/index' });
  },

  handleDeviceTap() {
    wx.navigateTo({ url: '/pages-admin/devices/index' });
  },

  handleRankingTap(e: WechatMiniprogram.TouchEvent) {
    const storeId = String(e.currentTarget.dataset.storeId || '');
    wx.navigateTo({
      url: `/pages-admin/orders/index${storeId ? `?storeId=${storeId}` : ''}`,
    });
  },

  handleActivityTap(e: WechatMiniprogram.TouchEvent) {
    const type = String(e.currentTarget.dataset.type || '');
    const referenceNo = String(e.currentTarget.dataset.referenceNo || '');
    if (type === 'order') {
      wx.navigateTo({
        url: `/pages-admin/orders/index${referenceNo ? `?keyword=${encodeURIComponent(referenceNo)}` : ''}`,
      });
      return;
    }
    wx.navigateTo({ url: '/pages-admin/finance/index' });
  },

  refresh() {
    this.loadPage();
  },

  handleSwitchIdentity() {
    clearAdminSession();
    wx.redirectTo({
      url: '/pages-admin/login/index',
    });
  },

  returnToMiniHome() {
    wx.switchTab({
      url: '/pages/home/index',
    });
  },

  resolveEditableStoreId() {
    const selectedStoreId = Number(this.data.selectedStoreId || 0);
    if (selectedStoreId > 0) {
      return selectedStoreId;
    }
    if (this.data.stores.length === 1 && this.data.stores[0].id) {
      return Number(this.data.stores[0].id);
    }
    return 0;
  },

  mapSettingsForm(settings: any) {
    const toText = (value: any) => (value === undefined || value === null ? '' : String(value));
    return {
      storeName: toText(settings.storeName),
      province: toText(settings.province),
      city: toText(settings.city),
      district: toText(settings.district),
      address: toText(settings.address),
      contactName: toText(settings.contactName),
      contactPhone: toText(settings.contactPhone),
      coverImage: toText(settings.coverImage),
      doorCloseIntervalOneStart: toText(settings.doorCloseIntervalOneStart),
      doorCloseIntervalOneEnd: toText(settings.doorCloseIntervalOneEnd),
      doorCloseIntervalTwoStart: toText(settings.doorCloseIntervalTwoStart),
      doorCloseIntervalTwoEnd: toText(settings.doorCloseIntervalTwoEnd),
      registerRewardAmount: toText(settings.registerRewardAmount),
      inviteRewardAmount: toText(settings.inviteRewardAmount),
      activityIntro: toText(settings.activityIntro),
      rechargeDescription: toText(settings.rechargeDescription),
      memberDescription: toText(settings.memberDescription),
      cabinetMinRechargeAmount: toText(settings.cabinetMinRechargeAmount),
      cabinetMinBalanceAmount: toText(settings.cabinetMinBalanceAmount),
    };
  },

  toOptionalNumber(value: string) {
    const text = String(value || '').trim();
    if (!text) return undefined;
    const number = Number(text);
    return Number.isFinite(number) ? number : undefined;
  },
});
