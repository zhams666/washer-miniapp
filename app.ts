import { ensureCurrentUser, ensureLoginStorage } from './utils/user';
import { API_TRANSPORT, CLOUDBASE_ENV_ID } from './config/url';

App<IAppOption>({
  globalData: {},
  onLaunch() {
    if (API_TRANSPORT === 'cloudbase') {
      wx.cloud.init({ env: CLOUDBASE_ENV_ID });
    }
    ensureLoginStorage();
    void ensureCurrentUser({ silentCreate: false }).catch((error) => {
      console.error('ensureCurrentUser onLaunch error:', error);
    });
  },
});
