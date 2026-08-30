type TabItem = {
  pagePath: string;
  text: string;
  iconPath: string;
  selectedIconPath: string;
};

const TAB_LIST: TabItem[] = [
  {
    pagePath: 'pages/home/index',
    text: '首页',
    iconPath: '/assets/icons/tab-home.png',
    selectedIconPath: '/assets/icons/tab-home-active.png',
  },
  {
    pagePath: 'pages/service/index',
    text: '门店',
    iconPath: '/assets/icons/tab-store.png',
    selectedIconPath: '/assets/icons/tab-store-active.png',
  },
  {
    pagePath: 'pages/ranking/index',
    text: '榜单',
    iconPath: '/assets/icons/tab-ranking.png',
    selectedIconPath: '/assets/icons/tab-ranking-active.png',
  },
  {
    pagePath: 'pages/order/index',
    text: '订单',
    iconPath: '/assets/icons/tab-order.png',
    selectedIconPath: '/assets/icons/tab-order-active.png',
  },
  {
    pagePath: 'pages/mine/index',
    text: '我的',
    iconPath: '/assets/icons/tab-mine.png',
    selectedIconPath: '/assets/icons/tab-mine-active.png',
  },
];

const normalizePath = (value: any) => String(value || '').replace(/^\/+/, '');

Component({
  data: {
    list: TAB_LIST,
    selectedPath: 'pages/home/index',
  },

  lifetimes: {
    attached() {
      this.syncSelectedPath();
    },
  },

  pageLifetimes: {
    show() {
      this.syncSelectedPath();
    },
  },

  methods: {
    syncSelectedPath() {
      const pages = getCurrentPages();
      const current = pages[pages.length - 1];
      const route = normalizePath(current && current.route ? current.route : '');
      if (!route) {
        return;
      }
      this.setData({ selectedPath: route });
    },

    switchTab(e: WechatMiniprogram.TouchEvent) {
      const path = normalizePath(e.currentTarget.dataset.path);
      if (!path) {
        return;
      }
      const pages = getCurrentPages();
      const current = pages[pages.length - 1];
      const currentRoute = normalizePath(current && current.route ? current.route : '');
      if (path === currentRoute) {
        this.setData({ selectedPath: path });
        return;
      }
      wx.switchTab({
        url: `/${path}`,
        success: () => {
          this.syncSelectedPath();
        },
        fail: (error) => {
          this.syncSelectedPath();
          console.error('custom tabbar switchTab failed:', path, error);
          wx.showToast({
            title: '页面打开失败',
            icon: 'none',
          });
        },
      });
    },
  },
});
