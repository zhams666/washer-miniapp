import { createMiniAdminExchangeVouchers, getMiniAdminStores } from '../../apis/admin';
import { ensureAdminToken } from '../../utils/admin-auth';

const formatMoney = (value: any) => Number(value || 0).toFixed(2);
const normalizeAmountInput = (value: any) => String(value || '').replace(/[^\d.]/g, '');
const normalizeCountInput = (value: any) => String(value || '').replace(/[^\d]/g, '');

Page({
  data: {
    loading: false,
    submitting: false,
    stores: [] as any[],
    storePickerOptions: ['请选择门店'] as string[],
    selectedStoreIndex: 0,
    selectedStoreId: '',
    amount: '20',
    count: '50',
    remark: '',
    batchNo: '',
    generatedCards: [] as any[],
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

    this.setData({ loading: true });
    try {
      const stores = await getMiniAdminStores();
      this.setData({
        stores,
        storePickerOptions: ['请选择门店'].concat(stores.map((store) => store.storeName || `门店${store.id}`)),
        selectedStoreIndex: stores.length === 1 ? 1 : 0,
        selectedStoreId: stores.length === 1 ? String(stores[0].id) : '',
      });
    } catch (error) {
      wx.showToast({ title: '加载门店失败', icon: 'none' });
      console.error('load exchange voucher stores failed:', error);
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
      generatedCards: [],
      batchNo: '',
    });
  },

  onAmountInput(e: WechatMiniprogram.Input) {
    this.setData({ amount: normalizeAmountInput(e.detail.value) });
  },

  onCountInput(e: WechatMiniprogram.Input) {
    this.setData({ count: normalizeCountInput(e.detail.value) });
  },

  onRemarkInput(e: WechatMiniprogram.Input) {
    this.setData({ remark: String(e.detail.value || '') });
  },

  async submitGenerate() {
    if (this.data.submitting) {
      return;
    }
    const storeId = Number(this.data.selectedStoreId || 0);
    const amount = Number(this.data.amount || 0);
    const count = Number(this.data.count || 0);
    if (!storeId) {
      wx.showToast({ title: '请先选择门店', icon: 'none' });
      return;
    }
    if (!amount || amount <= 0) {
      wx.showToast({ title: '请输入兑换金额', icon: 'none' });
      return;
    }
    if (!count || count <= 0) {
      wx.showToast({ title: '请输入生成张数', icon: 'none' });
      return;
    }
    if (count > 500) {
      wx.showToast({ title: '单次最多500张', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    wx.showLoading({ title: '生成中', mask: true });
    try {
      const result = await createMiniAdminExchangeVouchers({
        storeId,
        amount,
        count,
        remark: String(this.data.remark || '').trim() || '门店批量生成兑换券',
      });
      const cards = Array.isArray(result.vouchers) ? result.vouchers : [];
      this.setData({
        batchNo: String(result.batchNo || ''),
        generatedCards: cards.map((item) => ({
          ...item,
          amountText: formatMoney(item.amount),
        })),
      });
      wx.showToast({ title: '生成成功', icon: 'success' });
    } catch (error) {
      console.error('create exchange vouchers failed:', error);
    } finally {
      wx.hideLoading();
      this.setData({ submitting: false });
    }
  },

  copyGeneratedCodes() {
    const cards = this.data.generatedCards || [];
    if (!cards.length) {
      wx.showToast({ title: '暂无券码', icon: 'none' });
      return;
    }
    const text = cards.map((item) => item.serialNo).filter(Boolean).join('\n');
    wx.setClipboardData({
      data: text,
      success: () => {
        wx.showToast({ title: '已复制券码', icon: 'success' });
      },
    });
  },

  goVoucherList() {
    wx.navigateTo({
      url: `/pages-admin/vouchers/index${this.data.selectedStoreId ? `?storeId=${this.data.selectedStoreId}` : ''}`,
    });
  },
});
