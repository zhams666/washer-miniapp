import {
  createMembershipOrder,
  getMembershipOverview,
  syncMembershipOrder,
  type MembershipPlan,
  type MembershipSetting,
} from '../../apis/membership';
import { getCachedOpenId, requireCurrentUser } from '../../utils/user';

Page({
  data: {
    loading: true,
    submitting: false,
    settings: {} as MembershipSetting,
    plans: [] as MembershipPlan[],
    user: {
      isMember: 0,
      memberLevel: '',
      memberExpireTime: '',
      points: 0,
    },
    memberDayText: '会员日以后台设置为准',
  },

  onShow() {
    void this.loadOverview();
  },

  async loadOverview() {
    this.setData({ loading: true });
    try {
      const current = await requireCurrentUser();
      const userId = Number(current.costomerId || 0);
      const overview = await getMembershipOverview(userId);
      const settings = overview.settings || {};
      const user = overview.user || {};
      this.setData({
        settings,
        plans: overview.plans || [],
        user: {
          isMember: Number(user.isMember || 0),
          memberLevel: String(user.memberLevel || ''),
          memberExpireTime: this.formatDate(user.memberExpireTime),
          points: Number(user.points || 0),
        },
        memberDayText: this.buildMemberDayText(settings),
      });
    } catch (error) {
      wx.showToast({ title: '请先登录', icon: 'none' });
      console.error('load membership overview failed:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  buildMemberDayText(settings: MembershipSetting) {
    const weekday = Number(settings.memberDayWeekday || 3);
    const names = ['', '周一', '周二', '周三', '周四', '周五', '周六', '周日'];
    const day = names[weekday] || '指定日期';
    const start = String(settings.memberDayStartTime || '00:00').slice(0, 5);
    const end = String(settings.memberDayEndTime || '23:59').slice(0, 5);
    return `${day} ${start}-${end}`;
  },

  formatDate(value: any) {
    const text = String(value || '').trim();
    return text ? text.replace('T', ' ').slice(0, 16) : '';
  },

  formatPrice(value: any) {
    const amount = Number(value || 0);
    return Number.isNaN(amount) ? '0.00' : amount.toFixed(2);
  },

  async handleBuy(e: WechatMiniprogram.TouchEvent) {
    if (this.data.submitting) {
      return;
    }
    const planId = Number(e.currentTarget.dataset.planId || 0);
    if (!planId) {
      wx.showToast({ title: '会员方案无效', icon: 'none' });
      return;
    }

    this.setData({ submitting: true });
    try {
      const current = await requireCurrentUser();
      console.info('membership order submit started', {
        userId: Number(current.costomerId),
        planId,
      });
      const result = await createMembershipOrder({
        userId: Number(current.costomerId),
        planId,
        openId: getCachedOpenId() || '',
      });
      const payParams = result.payParams;
      if (result.payStatus === 'pending' && payParams) {
        await this.requestPayment(payParams);
        if (result.orderNo) {
          await syncMembershipOrder(result.orderNo);
        }
      }
      if (result.payStatus === 'paid' || payParams) {
        console.info('membership order completed', {
          planId,
          orderNo: result.orderNo,
          payStatus: result.payStatus,
        });
        wx.showToast({ title: '会员已开通', icon: 'success' });
        await this.loadOverview();
      } else {
        wx.showToast({ title: '订单未完成', icon: 'none' });
      }
    } catch (error) {
      wx.showToast({ title: '会员充值未完成', icon: 'none' });
      console.error('membership purchase failed:', {
        planId,
        message: error instanceof Error ? error.message : String(error || ''),
        error,
      });
    } finally {
      this.setData({ submitting: false });
    }
  },

  requestPayment(payParams: any): Promise<void> {
    return new Promise((resolve, reject) => {
      wx.requestPayment({
        timeStamp: String(payParams.timeStamp || ''),
        nonceStr: String(payParams.nonceStr || ''),
        package: String(payParams.packageValue || payParams.package || ''),
        signType: String(payParams.signType || 'RSA') as any,
        paySign: String(payParams.paySign || ''),
        success: () => resolve(),
        fail: reject,
      });
    });
  },
});
