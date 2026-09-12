import {
  deleteMiniAdminRankingDisplayAdjustment,
  getMiniAdminRankingAdjustments,
  getMiniAdminRankings,
  setMiniAdminRankingDisplayDuration,
} from '../../apis/admin';
import { ensureAdminToken } from '../../utils/admin-auth';

const scopeOptions = [
  { value: 'day', label: '24小时榜' },
  { value: 'month', label: '30日榜' },
  { value: 'total', label: '总榜' },
];

Page({
  data: {
    loading: false,
    adjustmentLoading: false,
    saving: false,
    deletingId: 0,
    scopeOptions,
    activeScope: 'day',
    activeScopeLabel: '24小时榜',
    rankings: [] as any[],
    adjustments: [] as any[],
    total: 0,
    pageCount: 0,
    page: 1,
    size: 10,
    dialogVisible: false,
    selectedUser: null as any,
    form: {
      durationMinutes: '',
      remark: '',
    },
  },

  onLoad() {
    try {
      ensureAdminToken();
    } catch (error) {
      return;
    }
    this.loadAll();
  },

  getScopeLabel(scope: string) {
    return scopeOptions.find((item) => item.value === scope)?.label || '总榜';
  },

  async loadAll() {
    await Promise.all([this.loadRankings(), this.loadAdjustments()]);
  },

  async loadRankings() {
    this.setData({ loading: true });
    try {
      const rows = await getMiniAdminRankings({ scope: this.data.activeScope, limit: 100 });
      this.setData({ rankings: (Array.isArray(rows) ? rows : []).map((item, index) => ({
        ...item,
        rank: item.rank || index + 1,
        durationText: item.durationText || this.formatDuration(item.durationSeconds),
        realDurationText: item.realDurationText || this.formatDuration(item.realDurationSeconds),
        displayAdjustmentText: item.displayAdjustmentText || this.formatSignedDuration(item.displayAdjustmentSeconds),
      })) });
    } catch (error) {
      console.error('load mini admin rankings failed:', error);
    } finally {
      this.setData({ loading: false });
    }
  },

  async loadAdjustments() {
    this.setData({ adjustmentLoading: true });
    try {
      const result = await getMiniAdminRankingAdjustments({
        scope: this.data.activeScope,
        page: this.data.page,
        size: this.data.size,
      });
      this.setData({
        adjustments: Array.isArray(result.records) ? result.records : [],
        total: Number(result.total || 0),
        pageCount: Math.max(1, Math.ceil(Number(result.total || 0) / this.data.size)),
      });
    } catch (error) {
      console.error('load mini admin ranking adjustments failed:', error);
    } finally {
      this.setData({ adjustmentLoading: false });
    }
  },

  handleScopeChange(e: WechatMiniprogram.TouchEvent) {
    const scope = String(e.currentTarget.dataset.scope || 'day');
    this.setData({ activeScope: scope, activeScopeLabel: this.getScopeLabel(scope), page: 1 });
    this.loadAll();
  },

  handleRefresh() {
    this.loadAll();
  },

  openSetDialog(e: WechatMiniprogram.TouchEvent) {
    const userId = Number(e.currentTarget.dataset.userid || 0);
    const user = this.data.rankings.find((item) => Number(item.userId) === userId);
    if (!userId || !user) return;
    this.setData({
      dialogVisible: true,
      selectedUser: user,
      form: {
        durationMinutes: String(user.durationMinutes === undefined || user.durationMinutes === null ? 0 : user.durationMinutes),
        remark: '',
      },
    });
  },

  closeDialog() {
    if (this.data.saving) return;
    this.setData({ dialogVisible: false, selectedUser: null, form: { durationMinutes: '', remark: '' } });
  },

  noop() {},

  handleFormInput(e: WechatMiniprogram.Input) {
    const field = String(e.currentTarget.dataset.field || '');
    if (!field) return;
    this.setData({ [`form.${field}`]: e.detail.value });
  },

  async handleSave() {
    const user = this.data.selectedUser;
    const durationMinutes = Number(this.data.form.durationMinutes);
    if (!user || !Number(user.userId)) return;
    if (!Number.isInteger(durationMinutes) || durationMinutes < 0 || durationMinutes > 100000) {
      wx.showToast({ title: '分钟数需为0到100000的整数', icon: 'none' });
      return;
    }
    this.setData({ saving: true });
    try {
      await setMiniAdminRankingDisplayDuration({
        userId: Number(user.userId),
        scope: this.data.activeScope,
        durationMinutes,
        remark: String(this.data.form.remark || '').trim(),
      });
      wx.showToast({ title: '排行榜已保存', icon: 'success' });
      this.closeDialog();
      await this.loadAll();
    } catch (error) {
      console.error('save mini admin ranking duration failed:', error);
    } finally {
      this.setData({ saving: false });
    }
  },

  handleDelete(e: WechatMiniprogram.TouchEvent) {
    const id = Number(e.currentTarget.dataset.id || 0);
    if (!id || this.data.deletingId) return;
    wx.showModal({
      title: '删除调整记录',
      content: '删除后该用户将恢复为真实洗车时长，确定继续吗？',
      success: async (result) => {
        if (!result.confirm) return;
        this.setData({ deletingId: id });
        try {
          await deleteMiniAdminRankingDisplayAdjustment(id);
          wx.showToast({ title: '已删除', icon: 'success' });
          await this.loadAll();
        } catch (error) {
          console.error('delete mini admin ranking adjustment failed:', error);
        } finally {
          this.setData({ deletingId: 0 });
        }
      },
    });
  },

  handlePageChange(e: WechatMiniprogram.PickerChange) {
    const page = Math.max(1, Number(e.detail.value || 1));
    this.setData({ page });
    this.loadAdjustments();
  },

  handlePrevPage() {
    if (this.data.page <= 1 || this.data.adjustmentLoading) return;
    this.setData({ page: this.data.page - 1 });
    this.loadAdjustments();
  },

  handleNextPage() {
    if (this.data.page >= this.data.pageCount || this.data.adjustmentLoading) return;
    this.setData({ page: this.data.page + 1 });
    this.loadAdjustments();
  },

  formatDuration(seconds: any) {
    const minutes = Math.max(0, Math.ceil(Number(seconds || 0) / 60));
    const hours = Math.floor(minutes / 60);
    const remain = minutes % 60;
    return hours ? `${hours}时${remain}分` : `${minutes}分`;
  },

  formatSignedDuration(seconds: any) {
    const value = Number(seconds || 0);
    return value < 0 ? `-${this.formatDuration(Math.abs(value))}` : this.formatDuration(value);
  },
});
