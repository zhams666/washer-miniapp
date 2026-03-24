Page({
  timer: null as WechatMiniprogram.Timer | null,

  data: {
    deviceNo: 'XC-STORE-001',
    storeName: 'Self-service Car Wash',
    startTime: '',
    elapsedSeconds: 0,
    timeLabel: '00:00',
    fee: '0.00',
    isPaused: false,
    statusText: 'Washing',
    unitPriceText: 'Billing rule: 0.12 / second',
  },

  onLoad(query: Record<string, string>) {
    const deviceNo = query.deviceNo || this.data.deviceNo;
    const storeName = query.storeName || this.data.storeName;
    const startTime = this.formatDateTime(new Date());

    this.setData({
      deviceNo,
      storeName,
      startTime,
    });

    this.startTimer();
  },

  onUnload() {
    this.clearTimer();
  },

  startTimer() {
    this.clearTimer();
    this.timer = setInterval(() => {
      const elapsedSeconds = this.data.elapsedSeconds + 1;
      this.setData({
        elapsedSeconds,
        timeLabel: this.formatDuration(elapsedSeconds),
        fee: this.calculateFee(elapsedSeconds),
      });
    }, 1000);
  },

  clearTimer() {
    if (this.timer) {
      clearInterval(this.timer);
      this.timer = null;
    }
  },

  formatDuration(totalSeconds: number) {
    const minutes = Math.floor(totalSeconds / 60);
    const seconds = totalSeconds % 60;
    return `${`${minutes}`.padStart(2, '0')}:${`${seconds}`.padStart(2, '0')}`;
  },

  formatDateTime(date: Date) {
    const year = date.getFullYear();
    const month = `${date.getMonth() + 1}`.padStart(2, '0');
    const day = `${date.getDate()}`.padStart(2, '0');
    const hours = `${date.getHours()}`.padStart(2, '0');
    const minutes = `${date.getMinutes()}`.padStart(2, '0');
    return `${year}-${month}-${day} ${hours}:${minutes}`;
  },

  calculateFee(totalSeconds: number) {
    return (totalSeconds * 0.12).toFixed(2);
  },

  togglePause() {
    if (this.data.isPaused) {
      this.startTimer();
      this.setData({
        isPaused: false,
        statusText: 'Washing',
      });
      return;
    }

    this.clearTimer();
    this.setData({
      isPaused: true,
      statusText: 'Paused',
    });
  },

  endWash() {
    this.clearTimer();
    wx.showModal({
      title: 'Wash finished',
      content: `Duration ${this.data.timeLabel}, fee ${this.data.fee}`,
      showCancel: false,
      success: () => {
        wx.navigateBack({
          delta: 1,
          fail: () => {
            wx.reLaunch({
              url: '/pages/home/index',
            });
          },
        });
      },
    });
  },
});
