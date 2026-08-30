import { getMiniAdminCurrent, getMiniAdminOperationOverview } from '../../apis/admin';
import { clearAdminSession, ensureAdminToken, setAdminProfile } from '../../utils/admin-auth';

const formatMoney = (value: any) => Number(value || 0).toFixed(2);
const formatCount = (value: any) => String(Number(value || 0));

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
    try {
      const profile = await getMiniAdminCurrent();
      setAdminProfile(profile);
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
        quickActions: this.buildQuickActions(overview.tierCode),
      });
    } catch (error) {
      wx.showToast({
        title: '管理端数据加载失败',
        icon: 'none',
      });
      console.error('load mini admin home failed:', error);
    } finally {
      this.setData({ loading: false });
    }
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

  buildQuickActions(tierCode: string) {
    if (tierCode === 'platform') {
      return [
        { key: 'devices', title: '全局设备', desc: '跨门店巡检异常', icon: '/assets/icons/device.png' },
        { key: 'orders', title: '订单总览', desc: '全部门店订单', icon: '/assets/icons/order.png' },
        { key: 'assets', title: '用户资产', desc: '余额、罚款、次卡', icon: '/assets/icons/wallet.png' },
        { key: 'finance', title: '经营流水', desc: '总部财务视角', icon: '/assets/icons/wallet.png' },
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
        { key: 'features', title: '功能整理', desc: '需求和接入状态', icon: '/assets/icons/question.png' },
        { key: 'profile', title: '加盟权限', desc: '账号与门店范围', icon: '/assets/icons/user.png' },
      ];
    }
    return [
      { key: 'devices', title: '设备管理', desc: '状态、启停、异常', icon: '/assets/icons/device.png' },
      { key: 'orders', title: '订单查询', desc: '本店订单和支付', icon: '/assets/icons/order.png' },
      { key: 'assets', title: '用户资产', desc: '加钱、罚款、次卡', icon: '/assets/icons/wallet.png' },
      { key: 'finance', title: '流水中心', desc: '充值、消费、核销', icon: '/assets/icons/wallet.png' },
      { key: 'features', title: '功能整理', desc: '需求和接入状态', icon: '/assets/icons/question.png' },
      { key: 'profile', title: '我的权限', desc: '账号、门店、角色', icon: '/assets/icons/user.png' },
    ];
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

  handleQuickTap(e: WechatMiniprogram.TouchEvent) {
    const key = e.currentTarget.dataset.key;
    const map: Record<string, string> = {
      devices: '/pages-admin/devices/index',
      orders: '/pages-admin/orders/index',
      assets: '/pages-admin/assets/index',
      finance: '/pages-admin/finance/index',
      features: '/pages-admin/features/index',
      profile: '/pages-admin/profile/index',
    };
    const url = map[String(key)];
    if (url) {
      wx.navigateTo({ url });
    }
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
});
