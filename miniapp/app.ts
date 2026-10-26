import { appConfig } from './utils/config'

App<IAppOption>({
  globalData: {
    brandName: appConfig.brandName,
    apiBaseUrl: appConfig.apiBaseUrl,
  },
  onLaunch() {
    console.info(`[MallMini] ${appConfig.brandName} launched`)
  },
})
