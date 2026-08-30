import type { IObject } from 'typings/interface.d';
import { getCardProducts, purchaseCard } from '../../apis/card';
import { getStoreDetail } from '../../apis/store';
import { ensureCurrentUser, requireCurrentUser } from '../../utils/user';

const TEXT_BUYING_CARD = '\u8d2d\u4e70\u4e2d...';

interface CardProductView {
  productId: number;
  cardType: string;
  title: string;
  desc: string;
  totalTimes: number;
  validDays: number;
  salePrice: string;
  originalSalePrice: string;
  showOriginalPrice: boolean;
  tag: string;
  limitText: string;
  disabledReason: string;
  buttonText: string;
  cardClass: string;
  limitDisplayText: string;
  showLimitText: boolean;
  buttonClass: string;
  isNewUserOnly: boolean;
  isNewUserTrial: boolean;
  isMonthlyCard: boolean;
  vipDiscounted: boolean;
  purchasable: boolean;
  unitText: string;
  benefitText: string;
}

Page({
  data: {
    storeId: 0,
    storeName: '',
    loading: false,
    buyingProductId: 0,
    monthlyProducts: [] as CardProductView[],
    cardProducts: [] as CardProductView[],
  },

  onLoad(options: Record<string, string>) {
    const storeId = Number((options && options.storeId) || 0);
    const storeName = decodeURIComponent(String((options && options.storeName) || '')).trim();
    if (!storeId) {
      wx.showModal({
        title: '提示',
        content: '门店信息无效，请从门店详情重新进入。',
        showCancel: false,
        success: () => {
          wx.navigateBack();
        },
      });
      return;
    }

    this.setData({
      storeId,
      storeName,
    });

    if (!storeName) {
      void this.loadStoreName(storeId);
    }
    void this.loadCardProducts(storeId);
  },

  async loadStoreName(storeId: number) {
    this.setData({ loading: true });
    try {
      const detail = await getStoreDetail(storeId);
      const storeName = String((detail && (detail.storeName || detail.name)) || '').trim();
      if (storeName) {
        this.setData({ storeName });
      }
    } catch (error) {
      console.warn('loadStoreName failed:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadCardProducts(storeId: number) {
    try {
      const user = await ensureCurrentUser().catch(() => null);
      const userId = Number((user && user.costomerId) || 0);
      const records = await getCardProducts(storeId, userId || undefined);
      const products = records
        .map((item) => this.toCardProductView(item))
        .filter((item) => item.productId > 0);
      this.setData({
        monthlyProducts: products.filter((item) => item.isMonthlyCard),
        cardProducts: products.filter((item) => !item.isMonthlyCard),
      });
    } catch (error) {
      console.warn('loadCardProducts failed:', error);
    }
  },

  toCardProductView(item: IObject): CardProductView {
    const totalTimes = Number(item.totalTimes || 1);
    const validDays = Number(item.validDays || 0);
    const cardType = String(item.cardType || '').toLowerCase();
    const title = String(item.title || item.cardName || '');
    const isNewUserTrial = this.toBoolean(item.isNewUserTrial);
    const isMonthlyCard = this.toBoolean(item.isMonthlyCard) || cardType === 'monthly';
    const vipDiscounted = this.toBoolean(item.vipDiscounted);
    const purchasable = item.purchasable !== false;
    const limitText = String(item.limitText || '');
    const disabledReason = String(item.disabledReason || '');
    const cardClass = `${isMonthlyCard ? 'monthly' : ''} ${isNewUserTrial ? 'trial' : ''} ${
      vipDiscounted ? 'discounted' : ''
    } ${purchasable ? '' : 'locked'}`.trim();
    const limitDisplayText = disabledReason || limitText;
    const originalSalePrice = this.formatPrice(item.originalSalePrice || item.salePrice);
    const salePrice = this.formatPrice(item.salePrice);
    const showOriginalPrice = vipDiscounted && originalSalePrice !== salePrice;
    return {
      productId: Number(item.productId || item.id || 0),
      cardType,
      title,
      desc: String(item.desc || (isMonthlyCard ? '享本店VIP洗车价与次卡优惠' : `发放 ${totalTimes} 张本店单次卡`)),
      totalTimes,
      validDays,
      salePrice,
      originalSalePrice,
      showOriginalPrice,
      tag: String(item.tag || ''),
      limitText,
      disabledReason,
      buttonText: this.resolveBuyButtonText(item),
      cardClass,
      limitDisplayText,
      showLimitText: Boolean(limitDisplayText),
      buttonClass: purchasable ? '' : 'disabled',
      isNewUserOnly: this.toBoolean(item.isNewUserOnly),
      isNewUserTrial,
      isMonthlyCard,
      vipDiscounted,
      purchasable,
      unitText: isMonthlyCard ? `${validDays || 30} 天` : `${totalTimes} 次`,
      benefitText: isMonthlyCard ? 'VIP权益' : '单次权益',
    };
  },

  resolveBuyButtonText(item: IObject) {
    if (item.purchasable === false) {
      return String(item.disabledReason || '不可买');
    }
    return '购买';
  },

  formatPrice(value: unknown): string {
    const amount = Number(value || 0);
    if (Number.isNaN(amount)) {
      return '0.00';
    }
    return amount.toFixed(2);
  },

  async handleBuyCard(e: WechatMiniprogram.TouchEvent) {
    const { productId } = e.currentTarget.dataset as { productId: number };
    const safeProductId = Number(productId || 0);
    const product = [
      ...(this.data.monthlyProducts as CardProductView[]),
      ...(this.data.cardProducts as CardProductView[]),
    ].find((item) => item.productId === safeProductId);
    if (!product || this.data.buyingProductId) {
      return;
    }
    if (!product.purchasable) {
      wx.showToast({
        title: product.disabledReason || '该套餐暂不可购买',
        icon: 'none',
      });
      return;
    }

    const storeId = Number(this.data.storeId || 0);
    if (!storeId) {
      wx.showToast({
        title: '门店信息无效',
        icon: 'none',
      });
      return;
    }

    let userId = 0;
    try {
      const user = await requireCurrentUser();
      userId = Number((user && user.costomerId) || 0);
      if (!userId) {
        throw new Error('current user is required');
      }
    } catch (error) {
      console.error('require user before buy card failed:', error);
      wx.showToast({
        title: '请先登录',
        icon: 'none',
      });
      return;
    }

    try {
      this.setData({ buyingProductId: product.productId });
      wx.showLoading({ title: TEXT_BUYING_CARD });
        const card = await purchaseCard({
          userId,
          storeId,
          productId: product.productId,
        });

        wx.hideLoading();
        if (String(card && card.payStatus || '').toLowerCase() === 'pending') {
          wx.showModal({
            title: '等待支付',
            content: '次卡订单已创建，等待支付渠道返回结果后才会发卡。',
            showCancel: false,
          });
          return;
        }
        const createdCount = Number((card && (card.createdCount || card.remainingTimes)) || product.totalTimes);
      const successContent = product.isMonthlyCard
        ? `已开通${product.title}，${product.unitText}内享本店VIP洗车价和次卡优惠。`
        : `已获得${product.title}，已发放 ${createdCount} 张单次卡。`;
      wx.showModal({
        title: '购买成功',
        content: successContent,
        showCancel: false,
      });
      void this.loadCardProducts(storeId);
    } catch (error) {
      wx.hideLoading();
      console.error('handleBuyCard error:', error);
      wx.showToast({
        title: this.extractErrorMessage(error) || '购买失败，请重试',
        icon: 'none',
      });
    } finally {
      this.setData({ buyingProductId: 0 });
    }
  },

  toBoolean(value: unknown) {
    return value === true || value === 'true' || value === 1 || value === '1';
  },

  extractErrorMessage(error: any) {
    const candidates = [error && error.message, error && error.msg, error && error.errMsg];
    for (let i = 0; i < candidates.length; i += 1) {
      const text = String(candidates[i] || '').trim();
      if (text) {
        return text;
      }
    }
    return '';
  },
});
