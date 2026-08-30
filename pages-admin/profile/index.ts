import { getMiniAdminCurrent } from '../../apis/admin';
import { clearAdminSession, ensureAdminToken, setAdminProfile } from '../../utils/admin-auth';

const PERMISSION_LABELS: Record<string, string> = {
  'dashboard:view': '经营看板',
  'device:view': '设备查看',
  'device:control': '设备控制',
  'order:view': '订单查看',
  'activity:view': '活动查看',
  'user:view': '用户查看',
  'wallet:adjust': '钱包调整',
  'card:adjust': '次卡调整',
  'finance:view': '财务查看',
  'settlement:view': '结算查看',
  'store:edit': '门店维护',
  'staff:manage': '员工管理',
};

const mapPermissionLabels = (permissions: string[]) => {
  const labels = permissions.map((permission) => PERMISSION_LABELS[permission] || '其他权限');
  return Array.from(new Set(labels));
};

Page({
  data: {
    loading: false,
    profile: null as any,
    stores: [] as any[],
    permissions: [] as string[],
  },

  onLoad() {
    this.loadProfile();
  },

  async loadProfile() {
    try {
      ensureAdminToken();
    } catch (error) {
      return;
    }
    this.setData({ loading: true });
    try {
      const profile = await getMiniAdminCurrent();
      setAdminProfile(profile);
      const rawPermissions = Array.isArray(profile.permissions) ? profile.permissions : [];
      this.setData({
        profile: {
          ...profile,
          staffNoText: profile.id ? `员工编号 ${profile.id}` : '员工编号 --',
        },
        stores: Array.isArray(profile.stores) ? profile.stores : [],
        permissions: mapPermissionLabels(rawPermissions),
      });
    } catch (error) {
      console.error('load admin profile failed:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  handleLogout() {
    clearAdminSession();
    wx.redirectTo({
      url: '/pages-admin/login/index',
    });
  },
});
