import { ensureLoginStorage } from './utils/user';

App<IAppOption>({
  globalData: {},
  onLaunch() {
    ensureLoginStorage();
  },
});
