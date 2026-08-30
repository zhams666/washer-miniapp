import {
  ensureCurrentUser,
  getCachedUserId,
  getCachedUserProfile,
  getLoginMobileHistory,
  isLoggedIn,
  loginCurrentUserWithMobile,
  loginCurrentUserWithPhoneCode,
  registerCurrentUser,
} from '../../utils/user';
import { uploadAvatar } from '../../apis/costomer';
import { getWalletStoreBalances, getWalletSummary } from '../../apis/wallet';
import { getOrderPage } from '../../apis/order';
import { getCardSummary } from '../../apis/card';

type GiftItem = {
  storeId: number;
  storeName: string;
  giftBalance: string;
};

type LoginMobileOption = {
  mobile: string;
  display: string;
  isLastUsed: boolean;
};

type TestLoginAccount = {
  nickname: string;
  mobile: string;
  display: string;
};

type MenuItem = {
  key: string;
  title: string;
  icon: string;
};

const DEFAULT_AVATAR = '/assets/icons/user.png';
const TEST_LOGIN_ACCOUNTS: TestLoginAccount[] = [
  { nickname: '测试用户1', mobile: '19552500939', display: '195****0939' },
  { nickname: '测试用户2', mobile: '19552500940', display: '195****0940' },
  { nickname: '测试用户3', mobile: '19552500941', display: '195****0941' },
  { nickname: '测试用户4', mobile: '19552500942', display: '195****0942' },
  { nickname: '测试用户5', mobile: '19552500943', display: '195****0943' },
];

Page({
  data: {
    isLogin: false,
    userId: 0,
    userInfo: {
      nickName: '未登录',
      avatarUrl: DEFAULT_AVATAR,
      memberText: '点击登录以继续',
      memberBadgeText: '未登录',
      memberBadgeClass: 'guest',
      memberBenefitText: '登录后可查看余额和会员身份',
      memberSinceText: '',
      memberActionText: '',
      showMemberSince: false,
    },
    registerForm: {
      avatarUrl: '',
      nickname: '',
    },
    nicknameForm: {
      nickname: '',
    },
    showRegisterModal: false,
    showNicknameModal: false,
    showPhoneLoginModal: false,
    showPhoneFallbackLogin: false,
    fallbackMobile: '',
    registerSubmitting: false,
    nicknameSubmitting: false,
    phoneLoginSubmitting: false,
    loginMobileHistory: [] as string[],
    loginMobileOptions: [] as LoginMobileOption[],
    testLoginAccounts: TEST_LOGIN_ACCOUNTS,
    avatarUploading: false,
    stats: [
      { key: 'balance', label: '总余额', value: '0.00' },
      { key: 'wash', label: '洗车次数', value: '0' },
      { key: 'coupon', label: '卡券数', value: '0' },
      { key: 'points', label: '积分', value: '0' },
    ],
    menus: [
      { key: 'profile', title: '修改信息', icon: '/assets/icons/mine-profile.png' },
      { key: 'order', title: '洗车订单', icon: '/assets/icons/mine-order.png' },
      { key: 'wallet', title: '消费明细', icon: '/assets/icons/mine-wallet.png' },
      { key: 'service', title: '客服中心', icon: '/assets/icons/mine-service.png' },
      { key: 'coupon', title: '我的卡券', icon: '/assets/icons/mine-coupon.png' },
      { key: 'pointsMall', title: '积分商城', icon: '/assets/icons/mine-points.png' },
    ] as MenuItem[],
    principalBalance: '0.00',
    showBalanceDetail: false,
    giftBalances: [] as GiftItem[],
    detailLoading: false,
    adminNavigating: false,
  },

  onShow() {
    const tabBar = (this as any).getTabBar && (this as any).getTabBar();
    if (tabBar && tabBar.setData) {
      tabBar.setData({ selectedPath: 'pages/mine/index' });
    }
    this.preloadAdminPackage();
    this.refreshUserInfo();
    this.refreshLoginMobileHistory();
    this.refreshLatestUserProfile();
    this.loadOverview();
  },

  preloadAdminPackage() {
    const preloadSubPackage = (wx as any).preloadSubPackage;
    if (typeof preloadSubPackage !== 'function') {
      return;
    }
    preloadSubPackage({
      name: 'pages-admin',
      fail(error: unknown) {
        console.warn('preload admin package failed:', error);
      },
    });
  },

  refreshUserInfo() {
    const loggedIn = isLoggedIn();
    const userId = getCachedUserId() || 0;
    const profile = getCachedUserProfile() || {};
    const profileNickname = String(profile.nickname || profile.nickName || '').trim();
    const profileAvatarUrl = String(profile.avatarUrl || '').trim();
    const nickname = this.resolveNickname(profileNickname, loggedIn);
    const avatarUrl = profileAvatarUrl || DEFAULT_AVATAR;
    const memberText = loggedIn && userId ? `用户 ID: ${userId}` : '点击登录以继续';
    const memberInfo = this.resolveMemberInfo(profile, loggedIn);

    this.setData({
      isLogin: loggedIn,
      userId,
      registerForm: {
        avatarUrl: loggedIn ? '' : profileAvatarUrl,
        nickname: loggedIn ? '' : profileNickname,
      },
      userInfo: {
        nickName: nickname,
        avatarUrl,
        memberText,
        memberBadgeText: memberInfo.badgeText,
        memberBadgeClass: memberInfo.badgeClass,
        memberBenefitText: memberInfo.benefitText,
        memberSinceText: memberInfo.sinceText,
        memberActionText: memberInfo.actionText,
        showMemberSince: Boolean(memberInfo.sinceText),
      },
    });
  },

  async refreshLatestUserProfile() {
    if (!isLoggedIn() || !getCachedUserId()) {
      return;
    }
    try {
      const result = await ensureCurrentUser({
        forceRefresh: true,
        silentCreate: false,
      });
      if (result && result.status === 0) {
        this.refreshUserInfo();
      }
    } catch (error) {
      console.warn('refreshLatestUserProfile failed:', error);
    }
  },

  refreshLoginMobileHistory() {
    const profile = getCachedUserProfile() || {};
    const currentMobile = String(profile.mobile || profile.phone || '').trim();
    const history = getLoginMobileHistory();
    this.setData({
      loginMobileHistory: history,
      loginMobileOptions: this.buildLoginMobileOptions(history, currentMobile),
    });
  },

  openPhoneLoginModal() {
    const profile = getCachedUserProfile() || {};
    const mobile = String(profile.mobile || profile.phone || '').trim();
    const history = getLoginMobileHistory();
    this.setData({
      showPhoneLoginModal: true,
      showPhoneFallbackLogin: false,
      fallbackMobile: this.normalizeMobile(mobile),
      loginMobileHistory: history,
      loginMobileOptions: this.buildLoginMobileOptions(history, mobile),
    });
  },

  openMembershipPage() {
    if (!isLoggedIn()) {
      this.openPhoneLoginModal();
      return;
    }
    wx.navigateTo({ url: '/pages/member/index' });
  },

  closePhoneLoginModal() {
    if (this.data.phoneLoginSubmitting) {
      return;
    }
    this.setData({
      showPhoneLoginModal: false,
      showPhoneFallbackLogin: false,
    });
  },

  async handlePhoneLogin(e: WechatMiniprogram.CustomEvent) {
    if (this.data.phoneLoginSubmitting) {
      return;
    }

    const detail = (e && e.detail ? e.detail : {}) as Record<string, any>;
    const phoneCode = String(detail.code || '').trim();
    const errMsg = String(detail.errMsg || '').toLowerCase();
    if (!phoneCode) {
      const noPermission =
        Number(detail.errno) === 102 ||
        errMsg.includes('jsapi has no permission') ||
        errMsg.includes('no permission');
      console.warn('getPhoneNumber failed:', detail);
      this.setData({
        showPhoneFallbackLogin: noPermission || this.data.showPhoneFallbackLogin,
      });
      wx.showToast({
        title: noPermission
          ? '手机号接口未开通'
          : errMsg.includes('deny') || errMsg.includes('cancel')
          ? '已取消手机号授权'
          : '手机号授权失败',
        icon: 'none',
      });
      return;
    }

    this.setData({ phoneLoginSubmitting: true });
    try {
      const result = await loginCurrentUserWithPhoneCode(phoneCode, null);
      if (result && result.status === 0) {
        this.finishPhoneLoginSuccess();
        return;
      }

      wx.showToast({
        title: '登录失败，请重试',
        icon: 'none',
      });
    } catch (error) {
      wx.showToast({
        title: '登录失败，请检查网络',
        icon: 'none',
      });
      console.error('handlePhoneLogin error:', error);
    } finally {
      this.setData({ phoneLoginSubmitting: false });
    }
  },

  onFallbackMobileInput(e: WechatMiniprogram.Input) {
    const detail = e && e.detail ? e.detail : { value: '' };
    this.setData({
      fallbackMobile: this.normalizeMobile(detail.value),
    });
  },

  async handleFallbackMobileLogin() {
    if (this.data.phoneLoginSubmitting) {
      return;
    }

    const mobile = this.normalizeMobile(this.data.fallbackMobile);
    if (!/^1\d{10}$/.test(mobile)) {
      wx.showToast({
        title: '请输入正确手机号',
        icon: 'none',
      });
      return;
    }

    await this.loginWithMobile(mobile, 'handleFallbackMobileLogin');
  },

  async handleTestAccountLogin(e: WechatMiniprogram.TouchEvent) {
    if (this.data.phoneLoginSubmitting) {
      return;
    }
    const mobile = this.normalizeMobile(e.currentTarget.dataset.mobile);
    if (!mobile) {
      wx.showToast({
        title: '测试账号无效',
        icon: 'none',
      });
      return;
    }
    this.setData({ fallbackMobile: mobile });
    await this.loginWithMobile(mobile, 'handleTestAccountLogin');
  },

  async handleHistoryMobileLogin(e: WechatMiniprogram.TouchEvent) {
    if (this.data.phoneLoginSubmitting) {
      return;
    }

    const mobile = this.normalizeMobile(e.currentTarget.dataset.mobile);
    if (!mobile) {
      wx.showToast({
        title: '手机号无效',
        icon: 'none',
      });
      return;
    }

    await this.loginWithMobile(mobile, 'handleHistoryMobileLogin');
  },

  async loginWithMobile(mobile: string, actionName: string) {
    this.setData({ phoneLoginSubmitting: true });
    try {
      const result = await loginCurrentUserWithMobile(mobile, null);
      if (result && result.status === 0) {
        this.finishPhoneLoginSuccess();
        return;
      }

      console.error(`${actionName} failed result:`, result);
      wx.showToast({
        title: '登录失败',
        icon: 'none',
      });
    } catch (error) {
      const errorMessage = this.resolveRequestErrorMessage(error);
      wx.showToast({
        title: errorMessage || '登录失败',
        icon: 'none',
      });
      console.error(`${actionName} error:`, error);
    } finally {
      this.setData({ phoneLoginSubmitting: false });
    }
  },

  finishPhoneLoginSuccess() {
    this.setData({
      showPhoneLoginModal: false,
      showPhoneFallbackLogin: false,
    });
    this.refreshUserInfo();
    this.refreshLoginMobileHistory();
    this.loadOverview();
    wx.showToast({
      title: '登录成功',
      icon: 'success',
    });
  },

  buildLoginMobileOptions(history: string[], currentMobile = ''): LoginMobileOption[] {
    const mobiles = [currentMobile, ...history]
      .map((item) => this.normalizeMobile(item))
      .filter((item, index, list) => item && list.indexOf(item) === index);

    return mobiles.slice(0, 4).map((mobile, index) => ({
      mobile,
      display: this.maskMobile(mobile),
      isLastUsed: index === 0,
    }));
  },

  maskMobile(value: any) {
    const mobile = this.normalizeMobile(value);
    if (mobile.length >= 11) {
      return `${mobile.slice(0, 3)}****${mobile.slice(-4)}`;
    }
    if (mobile.length > 6) {
      return `${mobile.slice(0, 2)}****${mobile.slice(-2)}`;
    }
    return mobile;
  },

  normalizeMobile(value: any) {
    let mobile = String(value || '').replace(/[\s-]/g, '').trim();
    if (mobile.indexOf('+86') === 0) {
      mobile = mobile.slice(3);
    } else if (mobile.indexOf('86') === 0 && mobile.length === 13) {
      mobile = mobile.slice(2);
    }
    return mobile;
  },

  resolveRequestErrorMessage(error: any) {
    const text = String(
      (error && (error.msg || error.message || error.errMsg)) || ''
    ).trim();
    if (!text) {
      return '';
    }
    if (text.indexOf('fail') >= 0 || text.indexOf('timeout') >= 0) {
      return '请检查后端连接';
    }
    if (text.length > 12) {
      return text.slice(0, 12);
    }
    return text;
  },

  onChooseAvatar(e: WechatMiniprogram.CustomEvent) {
    const detail = e && e.detail ? (e.detail as Record<string, any>) : {};
    const avatarUrl = String(detail.avatarUrl || '').trim();
    if (!avatarUrl) {
      wx.showToast({
        title: '头像选择失败',
        icon: 'none',
      });
      return;
    }
    this.setData({
      'registerForm.avatarUrl': avatarUrl,
    });
  },

  async onLoggedInAvatarChoose(e: WechatMiniprogram.CustomEvent) {
    if (!this.data.isLogin || this.data.avatarUploading) {
      return;
    }

    const detail = e && e.detail ? (e.detail as Record<string, any>) : {};
    const avatarUrl = String(detail.avatarUrl || '').trim();
    if (!avatarUrl) {
      wx.showToast({
        title: '头像选择失败',
        icon: 'none',
      });
      return;
    }

    this.setData({ avatarUploading: true });
    wx.showLoading({
      title: '上传头像',
      mask: true,
    });

    try {
      const uploadedAvatarUrl = await this.uploadSelectedAvatar(avatarUrl);
      const currentProfile = getCachedUserProfile() || {};
      const nickname = String(currentProfile.nickname || currentProfile.nickName || '').trim();
      const result = await registerCurrentUser(
        Object.assign({}, currentProfile, {
          avatarUrl: uploadedAvatarUrl,
          nickname,
          nickName: nickname,
        })
      );

      if (result && result.status === 0) {
        this.refreshUserInfo();
        wx.showToast({
          title: '头像已更新',
          icon: 'success',
        });
        return;
      }

      wx.showToast({
        title: '头像上传失败',
        icon: 'none',
      });
    } catch (error) {
      wx.showToast({
        title: '头像上传失败',
        icon: 'none',
      });
      console.error('onLoggedInAvatarChoose error:', error);
    } finally {
      wx.hideLoading();
      this.setData({ avatarUploading: false });
    }
  },

  onNicknameInput(e: WechatMiniprogram.Input) {
    const detail = e && e.detail ? e.detail : { value: '' };
    this.setData({
      'registerForm.nickname': String(detail.value || ''),
    });
  },

  onEditNicknameInput(e: WechatMiniprogram.Input) {
    const detail = e && e.detail ? e.detail : { value: '' };
    this.setData({
      'nicknameForm.nickname': String(detail.value || ''),
    });
  },

  openRegisterModal() {
    const profile = getCachedUserProfile() || {};
    this.setData({
      showRegisterModal: true,
      registerForm: {
        avatarUrl: String(profile.avatarUrl || '').trim(),
        nickname: String(profile.nickname || profile.nickName || '').trim(),
      },
    });
  },

  closeRegisterModal() {
    if (this.data.registerSubmitting) {
      return;
    }
    this.setData({
      showRegisterModal: false,
    });
  },

  openNicknameModal() {
    if (!this.data.isLogin) {
      wx.showToast({
        title: '请先登录',
        icon: 'none',
      });
      return;
    }
    const profile = getCachedUserProfile() || {};
    const nickname = String(profile.nickname || profile.nickName || this.data.userInfo.nickName || '').trim();
    this.setData({
      showNicknameModal: true,
      nicknameForm: {
        nickname,
      },
    });
  },

  closeNicknameModal() {
    if (this.data.nicknameSubmitting) {
      return;
    }
    this.setData({
      showNicknameModal: false,
    });
  },

  resolveRegisterProfile() {
    const form = this.data.registerForm || {};
    const avatarUrl = String(form.avatarUrl || '').trim();
    const nickname = String(form.nickname || '').trim();

    if (!avatarUrl) {
      wx.showToast({
        title: '请选择头像',
        icon: 'none',
      });
      return null;
    }
    if (!nickname) {
      wx.showToast({
        title: '请输入昵称',
        icon: 'none',
      });
      return null;
    }
    if (nickname.length > 24) {
      wx.showToast({
        title: '昵称不能超过24字',
        icon: 'none',
      });
      return null;
    }

    return {
      avatarUrl,
      nickname,
      nickName: nickname,
    };
  },

  resolveNicknameFormProfile() {
    const form = this.data.nicknameForm || {};
    const nickname = String(form.nickname || '').trim();
    if (!nickname) {
      wx.showToast({
        title: '请输入昵称',
        icon: 'none',
      });
      return null;
    }
    if (nickname.length > 24) {
      wx.showToast({
        title: '昵称不能超过24字',
        icon: 'none',
      });
      return null;
    }
    return {
      nickname,
      nickName: nickname,
    };
  },

  async uploadSelectedAvatar(avatarUrl: string) {
    const url = String(avatarUrl || '').trim();
    if (!url) {
      throw new Error('avatar is required');
    }
    if (url.indexOf('http://') === 0 || url.indexOf('https://') === 0) {
      return url;
    }
    return uploadAvatar(url);
  },

  async handleRegisterSubmit() {
    if (this.data.registerSubmitting) {
      return;
    }

    const registerProfile = this.resolveRegisterProfile();
    if (!registerProfile) {
      return;
    }

    this.setData({ registerSubmitting: true });
    try {
      const uploadedAvatarUrl = await this.uploadSelectedAvatar(registerProfile.avatarUrl);
      const result = await registerCurrentUser(
        Object.assign({}, registerProfile, {
          avatarUrl: uploadedAvatarUrl,
        })
      );
      if (result && result.status === 0) {
        this.setData({
          showRegisterModal: false,
        });
        this.refreshUserInfo();
        this.loadOverview();
        wx.showToast({
          title: '资料已保存',
          icon: 'success',
        });
        return;
      }
      wx.showToast({
        title: '资料保存失败',
        icon: 'none',
      });
    } catch (error) {
      wx.showToast({
        title: '资料保存失败',
        icon: 'none',
      });
      console.error('handleRegisterSubmit error:', error);
    } finally {
      this.setData({ registerSubmitting: false });
    }
  },

  async handleNicknameSubmit() {
    if (this.data.nicknameSubmitting) {
      return;
    }

    const nicknameProfile = this.resolveNicknameFormProfile();
    if (!nicknameProfile) {
      return;
    }

    this.setData({ nicknameSubmitting: true });
    try {
      const currentProfile = getCachedUserProfile() || {};
      const result = await registerCurrentUser(Object.assign({}, currentProfile, nicknameProfile));
      if (result && result.status === 0) {
        this.setData({
          showNicknameModal: false,
        });
        this.refreshUserInfo();
        wx.showToast({
          title: '昵称已更新',
          icon: 'success',
        });
        return;
      }
      wx.showToast({
        title: '昵称修改失败',
        icon: 'none',
      });
    } catch (error) {
      wx.showToast({
        title: '昵称修改失败',
        icon: 'none',
      });
      console.error('handleNicknameSubmit error:', error);
    } finally {
      this.setData({ nicknameSubmitting: false });
    }
  },

  resolveNickname(value: any, loggedIn: boolean) {
    const nickname = String(value || '').trim();
    if (!loggedIn) {
      return '未登录';
    }
    const reservedName = ['dev', String.fromCharCode(116, 101, 115, 116), 'user'].join(' ');
    if (!nickname || nickname.toLowerCase() === reservedName) {
      return '已登录用户';
    }
    return nickname;
  },

  resolveMemberInfo(profile: Record<string, any>, loggedIn: boolean) {
    if (!loggedIn) {
      return {
        badgeText: '未登录',
        badgeClass: 'guest',
        benefitText: '登录后可查看余额和会员身份',
        sinceText: '',
        actionText: '',
      };
    }

    const isMember = this.toBoolean(profile.isMember);
    if (isMember) {
      const level = String(profile.memberLevel || '').toLowerCase();
      const badgeText = level === 'monthly' ? '月会员' : level === 'yearly' ? '年会员' : '终身会员';
      const expireText = this.resolveMemberExpireText(profile.memberExpireTime);
      return {
        badgeText,
        badgeClass: 'member',
        benefitText: '余额充值会员，会员日洗车前10分钟享优惠',
        sinceText: expireText || this.resolveMemberSinceText(profile.memberSinceTime),
        actionText: '查看会员权益',
      };
    }

    return {
      badgeText: '普通用户',
      badgeClass: 'normal',
      benefitText: '充值余额后自动成为终身会员',
      sinceText: '',
      actionText: '会员充值',
    };
  },

  resolveMemberExpireText(value: any) {
    const text = String(value || '').trim();
    return text ? `有效期至 ${text.replace('T', ' ').slice(0, 16)}` : '';
  },

  resolveMemberSinceText(value: any) {
    const text = String(value || '').trim();
    if (!text) {
      return '';
    }
    return `入会时间 ${text.replace('T', ' ').slice(0, 16)}`;
  },

  async loadOverview() {
    const loggedIn = isLoggedIn();
    const userId = getCachedUserId();
    if (!loggedIn || !userId) {
      this.setData({
        stats: this.buildStats(0, 0, 0, 0),
        principalBalance: '0.00',
        giftBalances: [],
      });
      return;
    }

    try {
      const [summary, orderPage, cardSummary] = await Promise.all([
        getWalletSummary(userId),
        getOrderPage(1, 1, userId),
        getCardSummary(userId),
      ]);

      const principal = Number(summary && summary.principalBalance !== undefined ? summary.principalBalance : 0);
      const gift = Number(summary && summary.giftBalance !== undefined ? summary.giftBalance : 0);
      const total = Number(summary && summary.totalBalance !== undefined ? summary.totalBalance : principal + gift);
      const orderTotal = Number(orderPage && orderPage.total !== undefined ? orderPage.total : 0);
      const couponCount = Number(
        cardSummary && cardSummary.availableCount !== undefined
          ? cardSummary.availableCount
          : cardSummary && cardSummary.totalCount !== undefined
          ? cardSummary.totalCount
          : 0
      );

      const safePrincipal = Number.isNaN(principal) ? 0 : principal;
      const safeTotal = Number.isNaN(total) ? safePrincipal + (Number.isNaN(gift) ? 0 : gift) : total;
      const points = Number(summary && summary.points !== undefined ? summary.points : 0);

      this.setData({
        stats: this.buildStats(safeTotal, orderTotal, couponCount, points),
        principalBalance: safePrincipal.toFixed(2),
      });
    } catch (error) {
      console.error('loadOverview error:', error);
    }
  },

  buildStats(
    totalBalance: number,
    washTimes: number,
    couponCount: number,
    points: number
  ) {
    const safeBalance = Number.isNaN(totalBalance) ? 0 : totalBalance;
    const safeWash = Number.isNaN(washTimes) ? 0 : washTimes;
    const safeCoupon = Number.isNaN(couponCount) ? 0 : couponCount;
    const safePoints = Number.isNaN(points) ? 0 : points;

    return [
      { key: 'balance', label: '总余额', value: safeBalance.toFixed(2) },
      { key: 'wash', label: '洗车次数', value: String(safeWash) },
      { key: 'coupon', label: '卡券数', value: String(safeCoupon) },
      { key: 'points', label: '积分', value: String(safePoints) },
    ];
  },

  async loadBalanceDetail() {
    const userId = getCachedUserId();
    if (!userId || !isLoggedIn()) {
      wx.showToast({
        title: '请先登录',
        icon: 'none',
      });
      return;
    }

    this.setData({ detailLoading: true });
    try {
      const data = await getWalletStoreBalances(userId);
      const records = data && Array.isArray(data.records) ? data.records : [];
      const giftBalances = records.map((item: Record<string, any>) => ({
        storeId: Number(item.storeId || 0),
        storeName: item.storeName || `门店${item.storeId || ''}`,
        giftBalance: this.formatAmount(item.giftBalance),
      }));
      this.setData({ giftBalances });
    } catch (error) {
      wx.showToast({
        title: '余额明细加载失败',
        icon: 'none',
      });
      console.error('loadBalanceDetail error:', error);
    } finally {
      this.setData({ detailLoading: false });
    }
  },

  handleStatsTap(e: WechatMiniprogram.TouchEvent) {
    const { key } = e.currentTarget.dataset;
    if (key === 'balance') {
      this.openBalanceDetail();
    }
  },

  openBalanceDetail() {
    this.setData({ showBalanceDetail: true });
    if (this.data.giftBalances.length === 0) {
      this.loadBalanceDetail();
    }
  },

  closeBalanceDetail() {
    this.setData({ showBalanceDetail: false });
  },

  handleMenuTap(e: WechatMiniprogram.TouchEvent) {
    const { key } = e.currentTarget.dataset;

    if (key === 'profile') {
      this.openNicknameModal();
      return;
    }

    if (key === 'order') {
      wx.switchTab({ url: '/pages/order/index' });
      return;
    }

    if (key === 'wallet') {
      wx.navigateTo({ url: '/pages/wallet/index' });
      return;
    }

    if (key === 'service') {
      wx.switchTab({ url: '/pages/service/index' });
      return;
    }

    if (key === 'coupon') {
      wx.navigateTo({ url: '/pages/discount/index' });
      return;
    }

    if (key === 'pointsMall') {
      wx.navigateTo({ url: '/pages/points-mall/index' });
      return;
    }

    if (key === 'admin') {
      this.openAdminPortal();
    }
  },

  openAdminPortal() {
    if (this.data.adminNavigating) {
      return;
    }

    this.setData({ adminNavigating: true });
    wx.showLoading({ title: '正在进入管理端', mask: true });
    wx.navigateTo({
      url: '/pages-admin/login/index',
      fail: (error) => {
        console.error('navigate to mini admin login failed:', error);
        wx.showModal({
          title: '管理端加载失败',
          content: '请在微信开发者工具中点击“编译”后重试。',
          showCancel: false,
        });
      },
      complete: () => {
        wx.hideLoading();
        this.setData({ adminNavigating: false });
      },
    });
  },

  noop() {},

  formatAmount(value: any) {
    const num = Number(value || 0);
    if (Number.isNaN(num)) {
      return '0.00';
    }
    return num.toFixed(2);
  },

  toBoolean(value: any) {
    return value === true || value === 'true' || value === 1 || value === '1';
  },
});
