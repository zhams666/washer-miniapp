import {
  adjustMiniAdminCard,
  adjustMiniAdminWallet,
  createMiniAdminFine,
  getMiniAdminStores,
  getMiniAdminUserAssets,
  searchMiniAdminUsers,
} from '../../apis/admin';
import { ensureAdminToken } from '../../utils/admin-auth';

const formatMoney = (value: any) => Number(value || 0).toFixed(2);
const toNumber = (value: any) => Number(String(value || '').trim() || 0);

Page({
  data: {
    loading: false,
    submitting: false,
    stores: [] as any[],
    storePickerOptions: ['请选择门店'] as string[],
    selectedStoreIndex: 0,
    selectedStoreId: '',
    keyword: '',
    users: [] as any[],
    selectedUser: null as any,
    summary: null as any,
    cards: [] as any[],
    cardPickerOptions: ['自动选择/新建次卡'] as string[],
    selectedCardIndex: 0,
    walletChangeType: 'in',
    walletPrincipalAmount: '',
    walletGiftAmount: '',
    walletRemark: '',
    fineAmount: '',
    fineRemark: '',
    cardDeltaTimes: '',
    cardRemark: '',
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
      if (stores.length === 1) {
        await this.searchUsers();
      }
    } catch (error) {
      wx.showToast({ title: '加载门店失败', icon: 'none' });
      console.error('load admin asset initial failed:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  async searchUsers() {
    if (!this.data.selectedStoreId) {
      wx.showToast({ title: '请先选择门店', icon: 'none' });
      return;
    }
    this.setData({ loading: true });
    try {
      const users = await searchMiniAdminUsers({
        storeId: this.data.selectedStoreId,
        keyword: this.data.keyword || undefined,
      });
      this.setData({ users });
    } catch (error) {
      wx.showToast({ title: '用户搜索失败', icon: 'none' });
      console.error('search mini admin users failed:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadSummary() {
    const user = this.data.selectedUser;
    if (!user || !user.id || !this.data.selectedStoreId) {
      return;
    }
    try {
      const summary = await getMiniAdminUserAssets(Number(user.id), {
        storeId: this.data.selectedStoreId,
      });
      const cards = Array.isArray(summary.cards) ? summary.cards : [];
      this.setData({
        summary: {
          ...summary,
          principalText: formatMoney(summary.principalBalance),
          giftText: formatMoney(summary.giftBalance),
          totalText: formatMoney(summary.totalBalance),
        },
        cards,
        cardPickerOptions: ['自动选择/新建次卡'].concat(
          cards.map((card) => `${card.cardNo || '次卡'} / 剩${card.remainingTimes || 0}次`)
        ),
      });
    } catch (error) {
      wx.showToast({ title: '资产加载失败', icon: 'none' });
      console.error('load mini admin user asset summary failed:', error);
    }
  },

  handleStoreChange(e: WechatMiniprogram.PickerChange) {
    const selectedStoreIndex = Number(e.detail.value || 0);
    const store = selectedStoreIndex > 0 ? this.data.stores[selectedStoreIndex - 1] : null;
    this.setData({
      selectedStoreIndex,
      selectedStoreId: store && store.id ? String(store.id) : '',
      selectedUser: null,
      summary: null,
      cards: [],
      users: [],
    });
  },

  handleKeywordInput(e: WechatMiniprogram.Input) {
    this.setData({ keyword: e.detail.value });
  },

  handleUserTap(e: WechatMiniprogram.TouchEvent) {
    const index = Number(e.currentTarget.dataset.index || 0);
    const selectedUser = this.data.users[index];
    this.setData({ selectedUser });
    this.loadSummary();
  },

  handleCardChange(e: WechatMiniprogram.PickerChange) {
    this.setData({ selectedCardIndex: Number(e.detail.value || 0) });
  },

  setWalletIn() {
    this.setData({ walletChangeType: 'in' });
  },

  setWalletOut() {
    this.setData({ walletChangeType: 'out' });
  },

  onWalletPrincipalInput(e: WechatMiniprogram.Input) {
    this.setData({ walletPrincipalAmount: e.detail.value });
  },

  onWalletGiftInput(e: WechatMiniprogram.Input) {
    this.setData({ walletGiftAmount: e.detail.value });
  },

  onWalletRemarkInput(e: WechatMiniprogram.Input) {
    this.setData({ walletRemark: e.detail.value });
  },

  onFineAmountInput(e: WechatMiniprogram.Input) {
    this.setData({ fineAmount: e.detail.value });
  },

  onFineRemarkInput(e: WechatMiniprogram.Input) {
    this.setData({ fineRemark: e.detail.value });
  },

  onCardDeltaInput(e: WechatMiniprogram.Input) {
    this.setData({ cardDeltaTimes: e.detail.value });
  },

  onCardRemarkInput(e: WechatMiniprogram.Input) {
    this.setData({ cardRemark: e.detail.value });
  },

  async submitWalletAdjust() {
    const user = this.requireSelectedUser();
    if (!user) return;
    const principalAmount = toNumber(this.data.walletPrincipalAmount);
    const giftAmount = toNumber(this.data.walletGiftAmount);
    const remark = String(this.data.walletRemark || '').trim();
    if (principalAmount <= 0 && giftAmount <= 0) {
      wx.showToast({ title: '请输入调整金额', icon: 'none' });
      return;
    }
    if (!remark) {
      wx.showToast({ title: '请填写备注', icon: 'none' });
      return;
    }
    const actionText = this.data.walletChangeType === 'in' ? '加款' : '扣款';
    const ok = await this.confirmAction(`${actionText}确认`, `通用${formatMoney(principalAmount)}，赠送${formatMoney(giftAmount)}`);
    if (!ok) return;
    await this.submitOperation(() =>
      adjustMiniAdminWallet({
        userId: user.id,
        storeId: Number(this.data.selectedStoreId),
        changeType: this.data.walletChangeType,
        principalAmount,
        giftAmount,
        remark,
      })
    );
    this.setData({ walletPrincipalAmount: '', walletGiftAmount: '', walletRemark: '' });
  },

  async submitFine() {
    const user = this.requireSelectedUser();
    if (!user) return;
    const amount = toNumber(this.data.fineAmount);
    const remark = String(this.data.fineRemark || '').trim();
    if (amount <= 0) {
      wx.showToast({ title: '请输入罚款金额', icon: 'none' });
      return;
    }
    if (!remark) {
      wx.showToast({ title: '请填写罚款原因', icon: 'none' });
      return;
    }
    const ok = await this.confirmAction('罚款确认', `扣款 ¥${formatMoney(amount)}，原因：${remark}`);
    if (!ok) return;
    await this.submitOperation(() =>
      createMiniAdminFine({
        userId: user.id,
        storeId: Number(this.data.selectedStoreId),
        amount,
        remark,
      })
    );
    this.setData({ fineAmount: '', fineRemark: '' });
  },

  async submitCardAdjust() {
    const user = this.requireSelectedUser();
    if (!user) return;
    const deltaTimes = Number.parseInt(String(this.data.cardDeltaTimes || '0'), 10);
    const remark = String(this.data.cardRemark || '').trim();
    if (!deltaTimes) {
      wx.showToast({ title: '请输入增减次数', icon: 'none' });
      return;
    }
    if (!remark) {
      wx.showToast({ title: '请填写备注', icon: 'none' });
      return;
    }
    const card = this.data.selectedCardIndex > 0 ? this.data.cards[this.data.selectedCardIndex - 1] : null;
    const ok = await this.confirmAction('次卡调整确认', `${deltaTimes > 0 ? '增加' : '减少'} ${Math.abs(deltaTimes)} 次`);
    if (!ok) return;
    await this.submitOperation(() =>
      adjustMiniAdminCard({
        userId: user.id,
        storeId: Number(this.data.selectedStoreId),
        userCardId: card && card.id ? card.id : undefined,
        deltaTimes,
        remark,
      })
    );
    this.setData({ cardDeltaTimes: '', cardRemark: '', selectedCardIndex: 0 });
  },

  async submitOperation(action: () => Promise<any>) {
    if (this.data.submitting) return;
    this.setData({ submitting: true });
    try {
      await action();
      wx.showToast({ title: '操作成功', icon: 'success' });
      await this.loadSummary();
      await this.searchUsers();
    } catch (error) {
      console.error('mini admin asset operation failed:', error);
    } finally {
      this.setData({ submitting: false });
    }
  },

  requireSelectedUser() {
    if (!this.data.selectedStoreId) {
      wx.showToast({ title: '请先选择门店', icon: 'none' });
      return null;
    }
    if (!this.data.selectedUser || !this.data.selectedUser.id) {
      wx.showToast({ title: '请先选择用户', icon: 'none' });
      return null;
    }
    return this.data.selectedUser;
  },

  confirmAction(title: string, content: string): Promise<boolean> {
    return new Promise((resolve) => {
      wx.showModal({
        title,
        content,
        confirmText: '确认',
        cancelText: '取消',
        success: (res) => resolve(Boolean(res.confirm)),
        fail: () => resolve(false),
      });
    });
  },
});
