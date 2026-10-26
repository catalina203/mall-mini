import { devLogin, getMe, wechatLogin } from '../../services/auth'
import { isLoggedIn, logout } from '../../store/auth'
import type { User } from '../../types/api'
import { appConfig } from '../../utils/config'
import { syncTabBar } from '../../utils/tab-bar'

Page({
  data: {
    brandName: appConfig.brandName,
    loggedIn: false,
    loading: false,
    loggingIn: false,
    error: '',
    user: null as User | null,
    avatarText: '我',
  },

  onShow() {
    syncTabBar(this, 3)
    void this.refresh()
  },

  async refresh() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (!loggedIn) {
      this.setData({ user: null, loading: false })
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const user = await getMe()
      this.setData({
        user,
        avatarText: user.nickname ? user.nickname.slice(0, 1) : '我',
        loading: false,
      })
    } catch (error) {
      logout()
      this.setData({
        loggedIn: false,
        user: null,
        loading: false,
        error: error instanceof Error ? error.message : '用户信息加载失败',
      })
    }
  },

  async onDevLogin() {
    if (this.data.loggingIn) {
      return
    }
    this.setData({ loggingIn: true })
    try {
      await devLogin(appConfig.devLoginOpenId, '开发用户')
      wx.showToast({ title: '开发登录成功', icon: 'success' })
      await this.refresh()
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '登录失败',
        icon: 'none',
      })
    } finally {
      this.setData({ loggingIn: false })
    }
  },

  onWechatLogin() {
    if (this.data.loggingIn) {
      return
    }
    wx.login({
      success: (result) => {
        if (!result.code) {
          wx.showToast({ title: '未获取到微信登录 code', icon: 'none' })
          return
        }
        void this.doWechatLogin(result.code)
      },
      fail: () => {
        wx.showToast({ title: '微信登录调用失败', icon: 'none' })
      },
    })
  },

  async doWechatLogin(code: string) {
    this.setData({ loggingIn: true })
    try {
      await wechatLogin(code)
      wx.showToast({ title: '登录成功', icon: 'success' })
      await this.refresh()
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '微信登录失败',
        icon: 'none',
      })
    } finally {
      this.setData({ loggingIn: false })
    }
  },

  onLogout() {
    wx.showModal({
      title: '退出登录',
      content: '退出后购物车和地址需要重新登录查看。',
      success: (result) => {
        if (!result.confirm) {
          return
        }
        logout()
        this.setData({ loggedIn: false, user: null, error: '' })
        wx.showToast({ title: '已退出登录', icon: 'none' })
      },
    })
  },

  openOrders() {
    if (!this.requireLogin()) {
      return
    }
    wx.navigateTo({ url: '/pages/orders/index' })
  },

  openAddresses() {
    if (!this.requireLogin()) {
      return
    }
    wx.navigateTo({ url: '/pages/address-list/index' })
  },

  requireLogin(): boolean {
    if (this.data.loggedIn) {
      return true
    }
    wx.showToast({ title: '请先登录', icon: 'none' })
    return false
  },
})
