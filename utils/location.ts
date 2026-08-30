import type { IObject } from 'typings/interface.d';

/**
 * 导出获取定位方法
 */
export const getLocation = (): Promise<IObject> => {
  return new Promise((resolve, reject) => {
    const requestLocation = () => {
      wx.getLocation({
        type: 'gcj02',
        success(location: IObject) {
          resolve(location);
        },
        fail(error) {
          wx.showToast({
            title: '定位获取失败，请重试',
            icon: 'none',
          });
          reject(error);
        },
      });
    };

    wx.getSetting({
      success(settings: IObject) {
        const authSetting = settings.authSetting || {};
        if (authSetting['scope.userLocation'] === true) {
          requestLocation();
          return;
        }

        if (authSetting['scope.userLocation'] === false) {
          wx.showModal({
            title: '提示',
            content: '需要定位权限才能展示附近门店和距离，是否去开启？',
            success(res) {
              if (!res.confirm) {
                reject(new Error('location authorization denied'));
                return;
              }
              wx.openSetting({
                success(settingResult) {
                  const settingAuth = settingResult.authSetting || {};
                  if (settingAuth['scope.userLocation']) {
                    requestLocation();
                    return;
                  }
                  reject(new Error('location authorization denied'));
                },
                fail(error) {
                  reject(error);
                },
              });
            },
            fail(error) {
              reject(error);
            },
          });
          return;
        }

        wx.authorize({
          scope: 'scope.userLocation',
          success() {
            requestLocation();
          },
          fail(error) {
            reject(error);
          },
        });
      },
      fail(error) {
        reject(error);
      },
    });
  });
};
