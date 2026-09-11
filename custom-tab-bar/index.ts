type TabItem = {
  key: string;
  pagePath: string;
  text: string;
};

const TAB_LIST: TabItem[] = [
  {
    key: 'home',
    pagePath: 'pages/home/index',
    text: '首页',
  },
  {
    key: 'store',
    pagePath: 'pages/service/index',
    text: '门店',
  },
  {
    key: 'ranking',
    pagePath: 'pages/ranking/index',
    text: '榜单',
  },
  {
    key: 'order',
    pagePath: 'pages/order/index',
    text: '订单',
  },
  {
    key: 'mine',
    pagePath: 'pages/mine/index',
    text: '我的',
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
