import { getMiniAdminExchangeVouchers, getMiniAdminStores } from '../../apis/admin';
import { ensureAdminToken } from '../../utils/admin-auth';

const formatMoney = (value: any) => Number(value || 0).toFixed(2);
const formatTime = (value: any) => String(value || '').replace('T', ' ').slice(0, 16) || '--';

Page({
  data: {
    loading: false,
    stores: [] as any[],
    storePickerOptions: ['全部门店'] as string[],
    selectedStoreIndex: 0,
    selectedStoreId: '',
    statusOptions: ['全部状态', '未核销', '已核销'],
    statusValues: ['', 'unused', 'redeemed'],
    selectedStatusIndex: 0,
    keyword: '',
    page: 1,
    size: 20,
    hasMore: true,
    vouchers: [] as any[],
  },

  onLoad(options?: Record<string, string | undefined>) {
    const storeId = String((options && options.storeId) || '').trim();
    this.setData({ selectedStoreId: storeId });
    this.loadInitial(storeId);
  },

  async loadInitial(preselectedStoreId = '') {
    try {
      ensureAdminToken();
    } catch (error) {
      return;
    }
    const stores = await getMiniAdminStores().catch(() => []);
    const selectedIndex = preselectedStoreId
      ? stores.findIndex((store) => String(store.id) === preselectedStoreId) + 1
      : 0;
    this.setData({
      stores,
      storePickerOptions: ['全部门店'].concat(stores.map((store) => store.storeName || `门店${store.id}`)),
      selectedStoreIndex: Math.max(0, selectedIndex),
      selectedStoreId: selectedIndex > 0 ? preselectedStoreId : '',
    });
    this.reloadVouchers();
  },

  async reloadVouchers() {
    this.setData({
      page: 1,
      hasMore: true,
      vouchers: [],
    });
    await this.loadVouchers();
  },

  async loadVouchers() {
    if (this.data.loading || !this.data.hasMore) {
      return;
    }
    this.setData({ loading: true });
    try {
      const result = await getMiniAdminExchangeVouchers({
        page: this.data.page,
        size: this.data.size,
        storeId: this.data.selectedStoreId || undefined,
        status: this.data.statusValues[this.data.selectedStatusIndex] || undefined,
        keyword: String(this.data.keyword || '').trim() || undefined,
      });
      const records = Array.isArray(result.records) ? result.records : [];
      const mapped = records.map((item) => ({
        ...item,
        amountText: formatMoney(item.amount),
        statusText: String(item.status || '') === 'redeemed' ? '已核销' : '未核销',
        createdText: formatTime(item.createdAt),
        redeemedText: item.redeemedAt ? formatTime(item.redeemedAt) : '--',
        userText: item.redeemedUserNickname || item.redeemedUserMobile || (item.redeemedUserId ? `用户${item.redeemedUserId}` : '未核销'),
      }));
      const next = this.data.vouchers.concat(mapped);
      this.setData({
        vouchers: next,
        page: this.data.page + 1,
        hasMore: next.length < Number(result.total || 0),
      });
    } catch (error) {
      console.error('load exchange vouchers failed:', error);
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
    this.reloadVouchers();
  },

  handleStatusChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ selectedStatusIndex: Number(e.detail.value || 0) });
    this.reloadVouchers();
  },

  handleKeywordInput(e: WechatMiniprogram.Input) {
    this.setData({ keyword: e.detail.value });
  },

  handleSearch() {
    this.reloadVouchers();
  },

  handleLoadMore() {
    this.loadVouchers();
  },

  copyCode(e: WechatMiniprogram.TouchEvent) {
    const code = String(e.currentTarget.dataset.code || '').trim();
    if (!code) {
      return;
    }
    wx.setClipboardData({
      data: code,
      success: () => {
        wx.showToast({ title: '已复制券码', icon: 'success' });
      },
    });
  },

  goGenerate() {
    wx.navigateTo({ url: '/pages-admin/voucher-generate/index' });
  },
});
