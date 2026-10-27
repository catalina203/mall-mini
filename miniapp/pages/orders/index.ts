import { cancelOrder, confirmReceipt, listOrders } from '../../services/order'
import { isLoggedIn } from '../../store/auth'
import type { OrderStatus, OrderSummary } from '../../types/api'
import { formatDateTime, formatPrice } from '../../utils/format'
import { ORDER_STATUS_TABS, orderStatusClass, orderStatusText } from '../../utils/order'

interface OrderCard extends OrderSummary {
  statusText: string
  statusClass: string
  amountText: string
  createdAtText: string
}

Page({
  data: {
    loggedIn: false,
    tabs: ORDER_STATUS_TABS,
    activeStatus: '' as '' | OrderStatus,
    orders: [] as OrderCard[],
    page: 1,
    pageSize: 10,
    total: 0,
    loading: false,
    loadingMore: false,
    acting: false,
    error: '',
    hasMore: false,
  },

  onShow() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (!loggedIn) {
      this.setData({ orders: [], loading: false, error: '', hasMore: false })
      return
    }
    void this.load(true)
  },

  switchStatus(event: WechatMiniprogram.TouchEvent) {
    const status = String(event.currentTarget.dataset.status ?? '') as '' | OrderStatus
    if (status === this.data.activeStatus) {
      return
    }
    this.setData({ activeStatus: status })
    void this.load(true)
  },

  async load(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1
    this.setData(
      reset
        ? { loading: true, error: '', orders: [], page: 1, hasMore: true }
        : { loadingMore: true, error: '' },
    )
    try {
      const result = await listOrders({
        status: this.data.activeStatus || undefined,
        page,
        pageSize: this.data.pageSize,
      })
      const orders = result.items.map((order) => this.toCard(order))
      this.setData({
        orders: reset ? orders : [...this.data.orders, ...orders],
        page,
        total: result.total,
        hasMore: page * this.data.pageSize < result.total,
        loading: false,
        loadingMore: false,
      })
    } catch (error) {
      this.setData({
        loading: false,
        loadingMore: false,
        error: error instanceof Error ? error.message : '订单加载失败',
      })
    }
  },

  onReachBottom() {
    if (this.data.loading || this.data.loadingMore || !this.data.hasMore) {
      return
    }
    void this.load(false)
  },

  async onPullDownRefresh() {
    if (this.data.loggedIn) {
      await this.load(true)
    }
    wx.stopPullDownRefresh()
  },

  retry() {
    void this.load(true)
  },

  openDetail(event: WechatMiniprogram.TouchEvent) {
    const orderNo = String(event.currentTarget.dataset.orderNo ?? '')
    if (orderNo) {
      wx.navigateTo({ url: `/pages/order-detail/index?orderNo=${orderNo}` })
    }
  },

  cancelOrder(event: WechatMiniprogram.TouchEvent) {
    const orderNo = String(event.currentTarget.dataset.orderNo ?? '')
    if (!orderNo || this.data.acting) {
      return
    }
    wx.showModal({
      title: '取消订单',
      content: '确定取消该订单？库存将自动回补。',
      success: (result) => {
        if (result.confirm) {
          void this.doCancel(orderNo)
        }
      },
    })
  },

  async doCancel(orderNo: string) {
    this.setData({ acting: true })
    try {
      await cancelOrder(orderNo, '用户取消')
      wx.showToast({ title: '订单已取消', icon: 'success' })
      await this.load(true)
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '取消失败',
        icon: 'none',
      })
    } finally {
      this.setData({ acting: false })
    }
  },

  confirmReceipt(event: WechatMiniprogram.TouchEvent) {
    const orderNo = String(event.currentTarget.dataset.orderNo ?? '')
    if (!orderNo || this.data.acting) {
      return
    }
    wx.showModal({
      title: '确认收货',
      content: '确认已收到商品？确认后订单将完成。',
      success: (result) => {
        if (result.confirm) {
          void this.doConfirm(orderNo)
        }
      },
    })
  },

  async doConfirm(orderNo: string) {
    this.setData({ acting: true })
    try {
      await confirmReceipt(orderNo)
      wx.showToast({ title: '已确认收货', icon: 'success' })
      await this.load(true)
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '操作失败',
        icon: 'none',
      })
    } finally {
      this.setData({ acting: false })
    }
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },

  toCard(order: OrderSummary): OrderCard {
    const refunded = order.paymentStatus === 'REFUNDED'
    const partialRefunded = order.paymentStatus === 'PARTIALLY_REFUNDED'
    return {
      ...order,
      statusText: refunded ? '已退款' : partialRefunded ? '部分退款' : orderStatusText(order.status),
      statusClass: refunded ? 'status-refunded' : orderStatusClass(order.status),
      amountText: formatPrice(order.payableAmount),
      createdAtText: formatDateTime(order.createdAt),
    }
  },
})
