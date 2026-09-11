import { redeemExchangeVoucher, redeemVoucher } from '../../apis/card';
import { getMiniStoreList, getStoreList } from '../../apis/store';
import { requireCurrentUser } from '../../utils/user';

type VoucherPlatform = 'douyin' | 'meituan' | 'dazhong';

type StoreOption = {
  id: number;
  name: string;
  label: string;
};

const TEXT_SCAN_FAILED = '\u626b\u7801\u5931\u8d25\uff0c\u8bf7\u91cd\u8bd5';
const TEXT_VOUCHER_EMPTY = '\u8bf7\u8f93\u5165\u6216\u626b\u63cf\u5238\u53f7';
const TEXT_VOUCHER_REDEEMING = '\u6838\u9500\u4e2d...';
const TEXT_VOUCHER_REDEEM_FAILED =
  '\u6838\u9500\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u5238\u53f7';
const TEXT_EXCHANGE_REDEEM_FAILED =
  '\u5151\u6362\u5931\u8d25\uff0c\u8bf7\u68c0\u67e5\u5238\u7801';
const TEXT_LOGIN_REQUIRED = '\u8bf7\u5148\u767b\u5f55\u540e\u518d\u6838\u9500';
const TEXT_STORE_EMPTY = '\u8bf7\u5148\u9009\u62e9\u95e8\u5e97';

const PLATFORM_OPTIONS: Array<{ key: VoucherPlatform; label: string }> = [
  { key: 'douyin', label: '抖音' },
  { key: 'meituan', label: '美团' },
  { key: 'dazhong', label: '大众点评' },
];

Page({
  data: {
    redeemMode: 'platform',
    isExchangeMode: false,
    preselectedStoreId: 0,
    pageTitle: '券号核销',
    heroKicker: '第三方平台券',
    heroTitle: '核销抖音 / 美团次卡券',
    heroDesc: '输入券号或扫码核销，成功后会生成本门店可用单次卡。',
    codeLabel: '券号',
    codePlaceholder: '请输入券号',
    platforms: PLATFORM_OPTIONS,
    selectedPlatform: 'douyin' as VoucherPlatform,
    voucherCode: '',
    stores: [] as StoreOption[],
    storeNames: [] as string[],
    storeIndex: 0,
    selectedStoreId: 0,
    selectedStoreName: '',
    storesLoading: false,
    loading: false,
    resultVisible: false,
    resultStoreName: '',
    resultTimes: 0,
    resultCardNo: '',
    resultAmount: '0.00',
    resultBalance: '0.00',
    resultTransactionNo: '',
  },

  onLoad(options?: Record<string, string | undefined>) {
    const mode = String((options && options.mode) || '').trim();
    const isExchangeMode = mode === 'exchange';
    const storeId = Number((options && options.storeId) || 0);
    this.setData({
      redeemMode: isExchangeMode ? 'exchange' : 'platform',
      isExchangeMode,
      preselectedStoreId: storeId,
      pageTitle: isExchangeMode ? '兑换券核销' : '券号核销',
      heroKicker: isExchangeMode ? '门店兑换券' : '第三方平台券',
      heroTitle: isExchangeMode ? '核销门店金额兑换券' : '核销抖音 / 美团次卡券',
      heroDesc: isExchangeMode
        ? '输入店长生成的兑换券码，核销后进入本门店赠送余额。'
        : '输入券号或扫码核销，成功后会生成本门店可用单次卡。',
      codeLabel: isExchangeMode ? '兑换券码' : '券号',
      codePlaceholder: isExchangeMode ? '请输入字母数字券码' : '请输入券号',
    });
    void this.loadStores();
  },

  async loadStores() {
    this.setData({ storesLoading: true });
    try {
      let records = await this.loadMiniStoreRecords();
      if (records.length === 0) {
        records = await this.loadBaseStoreRecords();
      }

      const stores = (records as Record<string, any>[])
        .map((item: Record<string, any>) => this.normalizeStoreOption(item))
        .filter((item): item is StoreOption => item !== null);

      if (stores.length === 0) {
        this.clearStores();
        return;
      }

      this.setSelectedStore(stores, this.resolveInitialStoreIndex(stores));
    } catch (error) {
      console.error('load voucher stores failed:', error);
      this.clearStores();
    } finally {
      this.setData({ storesLoading: false });
    }
  },

  async loadMiniStoreRecords() {
    const pageData = await getMiniStoreList(1, 50);
    return pageData && Array.isArray(pageData.records) ? pageData.records : [];
  },

  async loadBaseStoreRecords() {
    const pageData = await getStoreList(1, 50);
    return pageData && Array.isArray(pageData.records) ? pageData.records : [];
  },

  normalizeStoreOption(item: Record<string, any>): StoreOption | null {
    const id = Number(item.id || item.storeId || 0);
    if (!id) {
      return null;
    }

    const name = String(item.name || item.storeName || '').trim();
    if (!name) {
      return null;
    }
    return {
      id,
      name,
      label: `${name}（ID ${id}）`,
    };
  },

  clearStores() {
    this.setData({
      stores: [],
      storeNames: [],
      storeIndex: 0,
      selectedStoreId: 0,
      selectedStoreName: '',
    });
  },

  setSelectedStore(stores: StoreOption[], index: number) {
    const safeIndex = Math.max(0, Math.min(index, stores.length - 1));
    const store = stores[safeIndex];
    this.setData({
      stores,
      storeNames: stores.map((item) => item.label),
      storeIndex: safeIndex,
      selectedStoreId: store.id,
      selectedStoreName: store.label,
      resultVisible: false,
    });
  },

  resolveInitialStoreIndex(stores: StoreOption[]) {
    const targetStoreId = Number(this.data.preselectedStoreId || 0);
    if (!targetStoreId) {
      return 0;
    }
    const index = stores.findIndex((store) => store.id === targetStoreId);
    return index >= 0 ? index : 0;
  },

  selectPlatform(e: WechatMiniprogram.TouchEvent) {
    const { platform } = e.currentTarget.dataset as { platform: VoucherPlatform };
    if (!platform || platform === this.data.selectedPlatform) {
      return;
    }
    this.setData({ selectedPlatform: platform });
  },

  handleVoucherCodeInput(e: WechatMiniprogram.Input) {
    this.setData({
      voucherCode: String(e.detail.value || '').trim(),
      resultVisible: false,
    });
  },

  handleStoreChange(e: WechatMiniprogram.PickerChange) {
    const index = Number(e.detail.value || 0);
    this.setSelectedStore(this.data.stores as StoreOption[], index);
  },

  scanVoucherCode() {
    if (this.data.loading) {
      return;
    }
    wx.scanCode({
      onlyFromCamera: false,
      success: (res) => {
        const code = String(res.result || '').trim();
        this.setData({ voucherCode: code, resultVisible: false });
      },
      fail: (error) => {
        if (String((error && error.errMsg) || '').includes('cancel')) {
          return;
        }
        console.error('scan voucher failed:', error);
        wx.showToast({
          title: TEXT_SCAN_FAILED,
          icon: 'none',
        });
      },
    });
  },

  async handleSubmit() {
    if (this.data.loading) {
      return;
    }

    const voucherCode = String(this.data.voucherCode || '').trim();
    if (!voucherCode) {
      wx.showToast({
        title: TEXT_VOUCHER_EMPTY,
        icon: 'none',
      });
      return;
    }

    if (!this.data.selectedStoreId) {
      wx.showToast({
        title: TEXT_STORE_EMPTY,
        icon: 'none',
      });
      return;
    }

    let userId = 0;
    try {
      const user = await requireCurrentUser();
      userId = Number((user && user.costomerId) || 0);
    } catch (error) {
      console.error('require user before voucher redeem failed:', error);
    }

    if (!userId) {
      wx.showToast({
        title: TEXT_LOGIN_REQUIRED,
        icon: 'none',
      });
      return;
    }

    try {
      this.setData({ loading: true, resultVisible: false });
      wx.showLoading({ title: TEXT_VOUCHER_REDEEMING });
      const card = this.data.isExchangeMode
        ? await redeemExchangeVoucher({
            userId,
            serialNo: voucherCode,
            voucherCode,
            storeId: Number(this.data.selectedStoreId),
          })
        : await redeemVoucher({
            userId,
            voucherCode,
            sourceChannel: this.data.selectedPlatform,
            storeId: Number(this.data.selectedStoreId),
          });

      wx.hideLoading();
      this.setData({
        voucherCode: '',
        resultVisible: true,
        resultStoreName: String((card && card.storeName) || '门店'),
        resultTimes: Number((card && (card.createdCount || card.remainingTimes)) || 1),
        resultCardNo: String((card && card.cardNo) || ''),
        resultAmount: this.formatAmount(card && card.amount),
        resultBalance: this.formatAmount(card && card.balanceAfter),
        resultTransactionNo: String((card && card.transactionNo) || ''),
      });
    } catch (error) {
      wx.hideLoading();
      console.error('redeem voucher error:', error);
      wx.showToast({
        title: this.data.isExchangeMode ? TEXT_EXCHANGE_REDEEM_FAILED : TEXT_VOUCHER_REDEEM_FAILED,
        icon: 'none',
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  formatAmount(value: unknown) {
    const amount = Number(value || 0);
    if (Number.isNaN(amount)) {
      return '0.00';
    }
    return amount.toFixed(2);
  },
});
