Component({
  options: { multipleSlots: true },
  properties: {
    title: {
      type: String,
      value: '导航栏',
    },
    backIcon: {
      type: String,
      value: '../../assets/icons/back.png',
    },
    showBackIcon: {
      type: Boolean,
      value: true,
    },
    borderBottom: {
      type: Boolean,
      value: true,
    },
    bottomShadow: {
      type: Boolean,
      value: false,
    },
    backText: {
      type: String,
      value: '返回',
    },
    slotHeight: {
      type: Number,
      value: 0,
    },
    showBackText: {
      type: Boolean,
      value: false,
    },
    titleColor: {
      type: String,
      value: '#1F2933',
    },
    backTextColor: {
      type: String,
      value: '#6B7280',
    },
    backgroundColor: {
      type: String,
      value: '#FFFFFF',
    },
    backIconWidth: {
      type: String,
      value: '44rpx',
    },
    backIconHeight: {
      type: String,
      value: '44rpx',
    },
    customBackPath: {
      type: String,
      value: '',
    },
    customBackPathIsTabbar: {
      type: Boolean,
      value: true,
    },
    fixed: {
      type: Boolean,
      value: true,
    },
  },
  data: {
    navbarHeight: 0,
    navbarPaddingTop: 0,
    navbarPaddingBottom: 0,
    navbarPaddingLeft: 0,
    navbarPaddingRight: 0,
    menuWidth: 0,
    statusBarHeight: 0,
    navbarInnerTop: 0,
    collapseHeight: 0,
    titleWidth: 0,
  },
  attached() {
    const windowInfo = wx.getWindowInfo();
    const safeWindowInfo = windowInfo as {
      windowWidth?: number;
      screenWidth?: number;
      statusBarHeight?: number;
    };
    const menuButtonInfo = wx.getMenuButtonBoundingClientRect();
    const screenWidth = safeWindowInfo.windowWidth || safeWindowInfo.screenWidth || 375;
    const statusBarHeight = safeWindowInfo.statusBarHeight || 0;
    const menuTop = menuButtonInfo.top || statusBarHeight + 6;
    const menuHeight = menuButtonInfo.height || 32;
    const menuWidth = menuButtonInfo.width || 87;
    const menuRight = menuButtonInfo.right || screenWidth - 8;
    const edgePadding = Math.max(screenWidth - menuRight, 8);
    const navPaddingBottom = Math.max(menuTop - statusBarHeight, 4);
    const titleGuardWidth = Math.max(menuWidth + edgePadding + 12, edgePadding + 72);

    this.setData({
      navbarHeight: menuHeight,
      menuWidth,
      navbarPaddingTop: menuTop,
      navbarPaddingBottom: navPaddingBottom,
      navbarPaddingLeft: edgePadding,
      navbarPaddingRight: edgePadding,
      statusBarHeight,
      navbarInnerTop: menuHeight / 2,
      titleWidth: Math.max(screenWidth - titleGuardWidth * 2, 120),
    });
    this.getCollapseHeight();
  },
  methods: {
    goBack() {
      if (this.properties.customBackPath) {
        if (this.properties.customBackPathIsTabbar) {
          wx.switchTab({
            url: this.properties.customBackPath,
          });
        } else {
          wx.navigateTo({
            url: this.properties.customBackPath,
          });
        }
      } else {
        wx.navigateBack({
          delta: 1,
        });
      }
    },
    getCollapseHeight() {
      const _this = this;
      this.createSelectorQuery()
        .select('#slot')
        .boundingClientRect((res) => {
          const height = res ? Number(res.height) : 0;
          _this.setData({
            collapseHeight:
              _this.data.navbarPaddingTop +
              _this.data.navbarHeight +
              _this.data.navbarPaddingBottom +
              height,
          });
          _this.triggerEvent('getCollapseHeight', _this.data.collapseHeight);
        })
        .exec();
    },
  },
});
