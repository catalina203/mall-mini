import { listNotifications, markNotificationRead } from '../../services/notification'
import { isLoggedIn } from '../../store/auth'
import type { UserNotification } from '../../types/api'
import { formatDateTime } from '../../utils/format'

interface NotificationView extends UserNotification {
  createdText: string
  targetUrl: string
}

Page({
  data: {
    loggedIn: false,
    loading: false,
    error: '',
    items: [] as NotificationView[],
  },

  onShow() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (!loggedIn) {
      this.setData({ items: [], loading: false, error: '' })
      return
    }
    void this.load()
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const result = await listNotifications(1, 50)
      this.setData({
        items: result.items.map((item) => this.toView(item)),
        loading: false,
      })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '通知加载失败',
      })
    }
  },

  async open(event: WechatMiniprogram.TouchEvent) {
    const index = Number(event.currentTarget.dataset.index)
    const item = this.data.items[index]
    if (!item) {
      return
    }
    if (!item.read) {
      try {
        await markNotificationRead(item.id)
        const items = this.data.items.map((current, currentIndex) =>
          currentIndex === index ? { ...current, read: true } : current,
        )
        this.setData({ items })
      } catch {
        // 阅读状态失败不阻塞跳转
      }
    }
    if (item.targetUrl) {
      wx.navigateTo({ url: item.targetUrl })
    }
  },

  retry() {
    void this.load()
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },

  toView(item: UserNotification): NotificationView {
    let targetUrl = ''
    if (item.bizType === 'AFTER_SALE' && item.bizNo) {
      targetUrl = `/pages/after-sale-detail/index?afterSaleNo=${item.bizNo}`
    } else if (item.bizType === 'ORDER' && item.bizNo) {
      targetUrl = `/pages/order-detail/index?orderNo=${item.bizNo}`
    }
    return {
      ...item,
      createdText: formatDateTime(item.createdAt),
      targetUrl,
    }
  },
})
