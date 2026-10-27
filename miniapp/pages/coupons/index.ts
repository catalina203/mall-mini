import { claimCoupon, listClaimableCoupons, listMyCoupons } from '../../services/coupon'
import { isLoggedIn } from '../../store/auth'
import type { CouponTemplate, UserCoupon } from '../../types/api'
import { formatDateTime, formatPrice } from '../../utils/format'

interface TemplateView extends CouponTemplate {
  valueText: string
  conditionText: string
  validText: string
  remaining: number
  remainingText: string
}

interface CouponView extends UserCoupon {
  valueText: string
  conditionText: string
  validText: string
  statusText: string
  statusLine: string
}

Page({
  data: {
    loggedIn: false,
    tab: 'claimable' as 'claimable' | 'mine',
    claimable: [] as TemplateView[],
    mine: [] as CouponView[],
    loading: false,
    acting: false,
    error: '',
  },

  onShow() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (!loggedIn) {
      this.setData({ claimable: [], mine: [], loading: false })
      return
    }
    void this.load()
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [claimable, mine] = await Promise.all([
        listClaimableCoupons(),
        listMyCoupons(),
      ])
      this.setData({
        claimable: claimable.map((item) => this.toTemplateView(item)),
        mine: mine.map((item) => this.toCouponView(item)),
        loading: false,
      })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '优惠券加载失败',
      })
    }
  },

  switchTab(event: WechatMiniprogram.TouchEvent) {
    this.setData({ tab: event.currentTarget.dataset.tab as 'claimable' | 'mine' })
  },

  async claim(event: WechatMiniprogram.TouchEvent) {
    if (this.data.acting) {
      return
    }
    const templateId = Number(event.currentTarget.dataset.id)
    if (!templateId) {
      return
    }
    this.setData({ acting: true })
    try {
      await claimCoupon(templateId)
      wx.showToast({ title: '领取成功', icon: 'success' })
      await this.load()
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '领取失败',
        icon: 'none',
      })
    } finally {
      this.setData({ acting: false })
    }
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },

  toTemplateView(item: CouponTemplate): TemplateView {
    const total = item.totalCount || 0
    const claimed = item.claimedCount || 0
    return {
      ...item,
      valueText: item.type === 'FIXED'
        ? `¥${Number(item.value).toFixed(0)}`
        : `${Number(item.value).toFixed(0)}%`,
      conditionText: Number(item.minAmount) > 0
        ? `满 ${formatPrice(item.minAmount)} 可用`
        : '无门槛',
      validText: `${formatDateTime(item.validFrom)} 至 ${formatDateTime(item.validTo)}`,
      remaining: total === 0 ? -1 : Math.max(total - claimed, 0),
      remainingText: total === 0 ? '不限量' : `剩余 ${Math.max(total - claimed, 0)} 张`,
    }
  },

  toCouponView(item: UserCoupon): CouponView {
    return {
      ...item,
      valueText: item.type === 'FIXED'
        ? `¥${Number(item.value).toFixed(0)}`
        : `${Number(item.value).toFixed(0)}%`,
      conditionText: Number(item.minAmount) > 0
        ? `满 ${formatPrice(item.minAmount)} 可用`
        : '无门槛',
      validText: `${formatDateTime(item.validFrom)} 至 ${formatDateTime(item.validTo)}`,
      statusText:
        item.status === 'UNUSED' ? '未使用' : item.status === 'USED' ? '已使用' : '已过期',
      statusLine: item.orderNo
        ? `${item.status === 'UNUSED' ? '未使用' : item.status === 'USED' ? '已使用' : '已过期'} · 订单 ${item.orderNo}`
        : `${item.status === 'UNUSED' ? '未使用' : item.status === 'USED' ? '已使用' : '已过期'}`,
    }
  },
})
