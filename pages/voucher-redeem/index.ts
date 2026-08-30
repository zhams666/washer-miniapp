import { redeemVoucher } from '../../apis/card';
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
const TEXT_LOGIN_REQUIRED = '\u8bf7\u5148\u767b\u5f55\u540e\u518d\u6838\u9500';
const TEXT_STORE_EMPTY = '\u8bf7\u5148\u9009\u62e9\u95e8\u5e97';

const PLATFORM_OPTIONS: Array<{ key: VoucherPlatform; label: string }> = [
  { key: 'douyin', label: '抖音' },
  { key: 'meituan', label: '美团' },
  { key: 'dazhong', label: '大众点评' },
];

Page({
  data: {
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
  },

  onLoad() {
    void this.loadStores();
  },

  async loadStores() {
    this.setData({ storesLoading: true });
    try {
      let records = await this.loadMiniStoreRecords();
      if (records.length === 0) {
        records = await this.loadBaseStoreRecords();
      }

      const stores = records
        .map((item) => this.normalizeStoreOption(item))
        .filter((item): item is StoreOption => item !== null);

      if (stores.length === 0) {
        this.clearStores();
        return;
      }

      this.setSelectedStore(stores, 0);
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
      const card = await redeemVoucher({
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
      });
    } catch (error) {
      wx.hideLoading();
      console.error('redeem voucher error:', error);
      wx.showToast({
        title: TEXT_VOUCHER_REDEEM_FAILED,
        icon: 'none',
      });
    } finally {
      this.setData({ loading: false });
    }
  },
});
