import { getDurationRanking } from '../../apis/order';
import { getCachedUserId } from '../../utils/user';

type RankScope = 'day' | 'month' | 'total';

type RankingRow = {
  rank: number;
  userId: number;
  nickname: string;
  avatarUrl: string;
  durationText: string;
  orderCount: number;
};

const DEFAULT_AVATAR = '/assets/icons/user.png';

const RANK_TABS: Array<{ key: RankScope; title: string }> = [
  { key: 'day', title: '24H榜' },
  { key: 'month', title: '30日累计' },
  { key: 'total', title: '总榜' },
];

Page({
  data: {
    tabs: RANK_TABS,
    activeScope: 'day' as RankScope,
    rows: [] as RankingRow[],
    loading: false,
    hasMyRank: false,
    myRankText: '',
    myName: '自己',
    myAvatarUrl: DEFAULT_AVATAR,
    myDurationText: '',
  },

  onLoad() {
    this.loadRanking();
  },

  onShow() {
    const tabBar = (this as any).getTabBar && (this as any).getTabBar();
    if (tabBar && tabBar.setData) {
      tabBar.setData({ selectedPath: 'pages/ranking/index' });
    }
  },

  switchScope(e: WechatMiniprogram.TouchEvent) {
    const dataset = e.currentTarget.dataset as { scope?: RankScope };
    const scope = dataset.scope;
    if (!scope || scope === this.data.activeScope) {
      return;
    }

    this.setData({
      activeScope: scope,
      rows: [],
      hasMyRank: false,
      myRankText: '',
      myDurationText: '',
    });
    this.loadRanking();
  },

  async loadRanking() {
    if (this.data.loading) {
      return;
    }

    this.setData({ loading: true });
    try {
      const userId = getCachedUserId() || undefined;
      const result = await getDurationRanking(this.data.activeScope, userId, 10);
      const records = result && Array.isArray(result.rows) ? result.rows : [];
      const rows = records.map((item: Record<string, any>, index: number) =>
        this.normalizeRankingRow(item, index)
      );
      const myRank = result && result.myRank ? result.myRank : null;
      const myRow = myRank ? this.normalizeRankingRow(myRank as Record<string, any>, 0) : null;

      this.setData({
        rows,
        hasMyRank: Boolean(myRow),
        myRankText: myRow ? `第 ${myRow.rank} 名` : '',
        myName: myRow ? myRow.nickname : '自己',
        myAvatarUrl: myRow ? myRow.avatarUrl : DEFAULT_AVATAR,
        myDurationText: myRow ? myRow.durationText : '',
      });
    } catch (error) {
      console.error('loadRanking failed:', error);
      wx.showToast({
        title: '榜单加载失败',
        icon: 'none',
      });
    } finally {
      this.setData({ loading: false });
    }
  },

  normalizeRankingRow(item: Record<string, any>, index: number): RankingRow {
    const rankValue = Number(item.rank || index + 1);
    const userIdValue = Number(item.userId || 0);
    const nickname = String(item.nickname || item.name || '匿名车友').trim();
    const avatarUrl = String(item.avatarUrl || '').trim();
    const durationText = String(item.durationText || '00分钟').trim();
    const orderCount = Number(item.orderCount || 0);

    return {
      rank: Number.isNaN(rankValue) ? index + 1 : rankValue,
      userId: Number.isNaN(userIdValue) ? 0 : userIdValue,
      nickname: nickname || '匿名车友',
      avatarUrl: avatarUrl || DEFAULT_AVATAR,
      durationText,
      orderCount: Number.isNaN(orderCount) ? 0 : orderCount,
    };
  },
});
