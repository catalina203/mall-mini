import { createAfterSale } from '../../services/after-sales'
import { getOrder } from '../../services/order'
import { isLoggedIn } from '../../store/auth'
import type { AfterSaleType, OrderDetail } from '../../types/api'
import { formatPrice } from '../../utils/format'

interface ApplyItem {
  orderItemId: number
  productName: string
  skuSpec: string
  productImage: string
  unitPrice: number
  maxQuantity: number
  quantity: number
  selected: boolean
  priceText: string
  refundText: string
}

const REASONS = ['不想要了', '商品质量问题', '收到商品与描述不符', '商家发错货', '其他原因']

Page({
  data: {
    orderNo: '',
    loggedIn: true,
    loading: true,
    submitting: false,
    error: '',
    orderStatus: '',
    canReturnRefund: false,
    type: 'REFUND_ONLY' as AfterSaleType,
    items: [] as ApplyItem[],
    reasons: REASONS,
    reasonIndex: 0,
    description: '',
    estimatedRefund: '¥0.00',
    selectedCount: 0,
  },

  onLoad(options: Record<string, string | undefined>) {
    const orderNo = options.orderNo ?? ''
    const loggedIn = isLoggedIn()
    this.setData({ orderNo, loggedIn })
    if (!orderNo) {
      this.setData({ loading: false, error: '缺少订单号' })
      return
    }
    if (!loggedIn) {
      this.setData({ loading: false })
      return
    }
    void this.load()
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const order = await getOrder(this.data.orderNo)
      if (order.afterSaleNo) {
        wx.redirectTo({
          url: `/pages/after-sale-detail/index?afterSaleNo=${order.afterSaleNo}`,
        })
        return
      }
      const items = this.toItems(order)
      this.setData({
        orderStatus: order.status,
        canReturnRefund:
          order.status === 'PENDING_RECEIPT' || order.status === 'COMPLETED',
        items,
        loading: false,
      })
      this.recalculate()
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '订单加载失败',
      })
    }
  },

  toItems(order: OrderDetail): ApplyItem[] {
    return order.items
      .filter((item) => item.orderItemId !== null)
      .map((item) => ({
        orderItemId: item.orderItemId as number,
        productName: item.productName,
        skuSpec: item.skuSpec,
        productImage: item.productImage,
        unitPrice: Number(item.unitPrice),
        maxQuantity: item.quantity,
        quantity: item.quantity,
        selected: true,
        priceText: formatPrice(item.unitPrice),
        refundText: formatPrice(item.unitPrice),
      }))
  },

  toggleItem(event: WechatMiniprogram.TouchEvent) {
    const index = Number(event.currentTarget.dataset.index)
    const items = this.data.items.map((item, current) =>
      current === index ? { ...item, selected: !item.selected } : item,
    )
    this.setData({ items })
    this.recalculate()
  },

  changeQuantity(event: WechatMiniprogram.TouchEvent) {
    const index = Number(event.currentTarget.dataset.index)
    const delta = Number(event.currentTarget.dataset.delta)
    const items = this.data.items.map((item, current) => {
      if (current !== index) {
        return item
      }
      const quantity = Math.min(Math.max(item.quantity + delta, 1), item.maxQuantity)
      return {
        ...item,
        quantity,
        refundText: formatPrice(item.unitPrice * quantity),
      }
    })
    this.setData({ items })
    this.recalculate()
  },

  recalculate() {
    const selected = this.data.items.filter((item) => item.selected)
    const selectedCount = selected.reduce((sum, item) => sum + item.quantity, 0)
    const total = selected.reduce((sum, item) => sum + item.unitPrice * item.quantity, 0)
    this.setData({
      selectedCount,
      estimatedRefund: formatPrice(total),
    })
  },

  selectType(event: WechatMiniprogram.TouchEvent) {
    const type = String(event.currentTarget.dataset.type) as AfterSaleType
    if (type === 'RETURN_REFUND' && !this.data.canReturnRefund) {
      wx.showToast({ title: '未发货订单只能申请仅退款', icon: 'none' })
      return
    }
    this.setData({ type })
  },

  onReasonChange(event: WechatMiniprogram.CustomEvent<{ value: string }>) {
    this.setData({ reasonIndex: Number(event.detail.value) })
  },

  onDescriptionInput(event: WechatMiniprogram.Input) {
    this.setData({ description: event.detail.value })
  },

  async submit() {
    if (this.data.submitting) {
      return
    }
    const selected = this.data.items.filter((item) => item.selected)
    if (!selected.length) {
      wx.showToast({ title: '请选择售后商品', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const detail = await createAfterSale(this.data.orderNo, {
        type: this.data.type,
        reason: this.data.reasons[this.data.reasonIndex],
        description: this.data.description.trim() || undefined,
        items: selected.map((item) => ({
          orderItemId: item.orderItemId,
          quantity: item.quantity,
        })),
      })
      wx.redirectTo({
        url: `/pages/after-sale-detail/index?afterSaleNo=${detail.afterSaleNo}`,
      })
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
