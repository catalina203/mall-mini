import { createReview } from '../../services/review'
import { isLoggedIn } from '../../store/auth'

Page({
  data: {
    orderNo: '',
    orderItemId: 0,
    productName: '',
    rating: 5,
    ratingOptions: [1, 2, 3, 4, 5],
    content: '',
    anonymous: false,
    submitting: false,
    loggedIn: true,
  },

  onLoad(options: Record<string, string | undefined>) {
    const loggedIn = isLoggedIn()
    this.setData({
      loggedIn,
      orderNo: options.orderNo ?? '',
      orderItemId: Number(options.orderItemId ?? 0),
      productName: decodeURIComponent(options.productName ?? ''),
    })
  },

  selectRating(event: WechatMiniprogram.TouchEvent) {
    this.setData({ rating: Number(event.currentTarget.dataset.rating) })
  },

  onContentInput(event: WechatMiniprogram.Input) {
    this.setData({ content: event.detail.value })
  },

  onAnonymousChange(event: WechatMiniprogram.CustomEvent<{ value: boolean }>) {
    this.setData({ anonymous: event.detail.value })
  },

  async submit() {
    if (this.data.submitting) return
    if (!this.data.orderNo || !this.data.orderItemId) {
      wx.showToast({ title: '缺少订单明细', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      await createReview(this.data.orderNo, {
        orderItemId: this.data.orderItemId,
        rating: this.data.rating,
        content: this.data.content.trim() || undefined,
        anonymous: this.data.anonymous,
      })
      wx.showToast({ title: '评价已提交，等待审核', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 500)
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '提交失败',
        icon: 'none',
      })
    } finally {
      this.setData({ submitting: false })
    }
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },
})
