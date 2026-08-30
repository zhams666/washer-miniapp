import { getWalletSummary } from '../../apis/wallet';
import { getPointMallProducts, redeemPointMallProduct } from '../../apis/points-mall';
import { getCachedUserId, isLoggedIn } from '../../utils/user';

Page({
  data: {
    points: 0,
    redeemingProductId: 0,
    products: [] as Array<{
      id: number;
      title: string;
      desc: string;
      points: number;
      tag: string;
      coverImage: string;
    }>,
  },

  async handleRedeem(e: WechatMiniprogram.TouchEvent) {
    const productId = Number(e.currentTarget.dataset.id || 0);
    const points = Number(e.currentTarget.dataset.points || 0);
    const userId = getCachedUserId();
    if (!isLoggedIn() || !userId) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      return;
    }
    if (!productId || this.data.redeemingProductId) return;
    if (this.data.points < points) {
      wx.showToast({ title: '积分不足', icon: 'none' });
      return;
    }
    const confirmed = await new Promise<boolean>((resolve) => wx.showModal({
      title: '确认兑换', content: `将扣除 ${points} 积分，是否继续？`, success: (res) => resolve(res.confirm), fail: () => resolve(false),
    }));
    if (!confirmed) return;
    this.setData({ redeemingProductId: productId });
    try {
      const order = await redeemPointMallProduct({ userId, productId, requestNo: `MP${Date.now()}${productId}` });
      if (!order.redemptionNo) throw new Error('兑换订单创建失败');
      wx.showToast({ title: order.fulfillmentStatus === 'completed' ? '兑换成功' : '兑换已受理', icon: 'success' });
      await this.loadPoints();
      await this.loadProducts();
    } catch (error) {
      console.error('redeem point mall product error:', error);
      wx.showToast({ title: '兑换失败，请稍后重试', icon: 'none' });
    } finally {
      this.setData({ redeemingProductId: 0 });
    }
  },

  onShow() {
    this.loadPoints();
    this.loadProducts();
  },

  async loadPoints() {
    const userId = getCachedUserId();
    if (!isLoggedIn() || !userId) {
      this.setData({ points: 0 });
      return;
    }

    try {
      const summary = await getWalletSummary(userId);
      if (!isLoggedIn() || getCachedUserId() !== userId) {
        return;
      }
      const points = Number(summary && summary.points !== undefined ? summary.points : 0);
      this.setData({ points: Number.isFinite(points) ? points : 0 });
    } catch (error) {
      if (!isLoggedIn() || getCachedUserId() !== userId) {
        return;
      }
      this.setData({ points: 0 });
      console.error('loadPoints error:', error);
    }
  },

  async loadProducts() {
    try {
      const products = await getPointMallProducts();
      this.setData({
        products: products.map((product) => ({
          id: product.id,
          title: String(product.title || ''),
          desc: String(product.description || ''),
          points: Number(product.pointsPrice || 0),
          tag: this.getProductTypeLabel(product.productType),
          coverImage: String(product.coverImage || ''),
        })),
      });
    } catch (error) {
      this.setData({ products: [] });
      console.error('loadProducts error:', error);
    }
  },

  getProductTypeLabel(productType: string) {
    if (productType === 'coupon') {
      return '优惠券';
    }
    if (productType === 'physical') {
      return '实物礼品';
    }
    return '洗车权益';
  },
});
