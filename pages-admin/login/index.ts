import { miniAdminLocalLogin, miniAdminLogin, type MiniAdminLoginResult } from '../../apis/admin';
import { clearAdminSession, getAdminToken, setAdminSession } from '../../utils/admin-auth';

Page({
  data: {
    loading: false,
    openId: '',
    message: '',
    debugMessage: '',
    devRoles: [
      { key: 'platform', title: '总部演示', desc: '全部加盟商和门店' },
      { key: 'franchisee', title: '加盟老板', desc: '当前加盟体系门店' },
      { key: 'store', title: '门店店长', desc: '门店1现场管理' },
      { key: 'staff', title: '门店员工', desc: '门店1工作人员' },
    ],
  },

  onLoad() {
    if (getAdminToken()) {
      wx.redirectTo({
        url: '/pages-admin/home/index',
      });
    }
  },

  async handleLogin() {
    this.setData({
      loading: true,
      message: '',
      openId: '',
      debugMessage: '',
    });

    try {
      const result = await miniAdminLogin();
      this.resolveLoginResult(result);
    } catch (error) {
      console.error('mini admin login failed:', error);
      await this.tryLocalLogin(error);
    } finally {
      this.setData({
        loading: false,
      });
    }
  },

  async handleDevLogin(e: WechatMiniprogram.TouchEvent) {
    const role = String(e.currentTarget.dataset.role || '');
    this.setData({
      loading: true,
      message: '',
      openId: '',
      debugMessage: '',
    });

    try {
      const result = await miniAdminLocalLogin(role);
      this.resolveLoginResult(result);
    } catch (error) {
      clearAdminSession();
      this.setData({
        message: '演示账号登录失败',
        debugMessage: this.resolveErrorMessage(error),
      });
      console.error('mini admin dev login failed:', error);
    } finally {
      this.setData({
        loading: false,
      });
    }
  },

  handleExitAdmin() {
    if (this.data.loading) {
      return;
    }

    clearAdminSession();
    wx.switchTab({
      url: '/pages/home/index',
    });
  },

  resolveLoginResult(result: MiniAdminLoginResult) {
    if (result.bound && result.token) {
      setAdminSession(result.token, result.profile || null);
      wx.redirectTo({
        url: '/pages-admin/home/index',
      });
      return;
    }

    clearAdminSession();
    this.setData({
      openId: result.openId || '',
      message: result.message || '当前微信尚未绑定管理端员工账号',
      debugMessage: '',
    });
  },

  async tryLocalLogin(originError: unknown) {
    try {
      const result = await miniAdminLocalLogin();
      this.resolveLoginResult(result);
    } catch (fallbackError) {
      clearAdminSession();
      this.setData({
        message: '登录失败，请稍后重试',
        debugMessage: this.resolveErrorMessage(fallbackError || originError),
      });
      console.error('mini admin local login failed:', fallbackError);
    }
  },

  resolveErrorMessage(error: unknown) {
    const record = (error || {}) as Record<string, any>;
    const message = record.message || record.errMsg || record.msg;
    if (message) {
      return String(message);
    }
    try {
      return JSON.stringify(error);
    } catch (stringifyError) {
      return String(error || '');
    }
  },
});
