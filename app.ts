import { ensureCurrentUser, ensureLoginStorage } from './utils/user';

App<IAppOption>({
  globalData: {},
  onLaunch() {
    ensureLoginStorage();
    void ensureCurrentUser({ silentCreate: false }).catch((error) => {
      console.error('ensureCurrentUser onLaunch error:', error);
    });
  },
});
