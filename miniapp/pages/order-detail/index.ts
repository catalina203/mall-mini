import { cancelOrder, confirmReceipt, getOrder } from '../../services/order'
import { getPaymentConfig, payOrder } from '../../services/payment'
import { isLoggedIn } from '../../store/auth'
import type { OrderDetail, OrderItem, OrderStatus, OrderStatusLog, PaymentConfig } from '../../types/api'
import { formatDateTime, formatPrice } from '../../utils/format'
import { orderStatusClass, orderStatusText } from '../../utils/order'
import { afterSaleStatusText } from '../../utils/after-sale'

interface ItemView extends OrderItem {
  priceText: string
  subtotalText: string
}

interface LogView extends OrderStatusLog {
  fromText: string
  toText: string
  createdText: string
}

interface OrderView {
  orderNo: string
  status: OrderStatus
  statusText: string
  statusClass: string
  statusNote: string
  paymentText: string
  goodsText: string
  shippingText: string
  discountText: string
  payableText: string
  addressText: string
  items: ItemView[]
  remark: string
  logisticsCompany: string
  logisticsNo: string
  createdAtText: string
  paidAtText: string
  shippedAtText: string
  completedAtText: string
  cancelledAtText: string
  cancelReason: string
  logs: LogView[]
  afterSaleNo: string
  afterSaleText: string
  canApplyAfterSale: boolean
  canChangeAddress: boolean
  invoiceRequired: boolean
  invoiceTitle: string
  invoiceTaxNo: string
  invoiceEmail: string
}

Page({
  data: {
    orderNo: '',
    loggedIn: true,
    loading: true,
    acting: false,
    paying: false,
    error: '',
    detail: null as OrderView | null,
    paymentConfig: null as PaymentConfig | null,
  },

  onShow() {
    if (this.data.loggedIn && this.data.orderNo && this.data.detail) {
      void this.load()
    }
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
    void this.loadPaymentConfig()
    void this.load()
  },

  async loadPaymentConfig() {
    try {
      this.setData({ paymentConfig: await getPaymentConfig() })
    } catch {
      this.setData({ paymentConfig: null })
    }
  },

  async load() {
    if (!this.data.orderNo) {
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const detail = await getOrder(this.data.orderNo)
      this.setData({ detail: this.toView(detail), loading: false })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '订单加载失败',
      })
    }
  },

  cancel() {
    if (this.data.acting) {
      return
    }
    wx.showModal({
      title: '取消订单',
      content: '确定取消该订单？库存将自动回补。',
      success: (result) => {
        if (result.confirm) {
          void this.doCancel()
        }
      },
    })
  },

  async doCancel() {
    this.setData({ acting: true })
    try {
      const detail = await cancelOrder(this.data.orderNo, '用户取消')
      this.setData({ detail: this.toView(detail) })
      wx.showToast({ title: '订单已取消', icon: 'success' })
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '取消失败',
        icon: 'none',
      })
    } finally {
      this.setData({ acting: false })
    }
  },

  confirmReceipt() {
    if (this.data.acting) {
      return
    }
    wx.showModal({
      title: '确认收货',
      content: '确认已收到商品？确认后订单将完成。',
      success: (result) => {
        if (result.confirm) {
          void this.doConfirm()
        }
      },
    })
  },

  async doConfirm() {
    this.setData({ acting: true })
    try {
      const detail = await confirmReceipt(this.data.orderNo)
      this.setData({ detail: this.toView(detail) })
      wx.showToast({ title: '已确认收货', icon: 'success' })
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '操作失败',
        icon: 'none',
      })
    } finally {
      this.setData({ acting: false })
    }
  },

  pay() {
    if (this.data.paying) {
      return
    }
    wx.showModal({
      title: '模拟支付',
      content: '当前为开发环境模拟支付，不会产生真实扣款，是否继续？',
      confirmText: '确认支付',
      success: (result) => {
        if (result.confirm) {
          void this.doPay()
        }
      },
    })
  },

  async doPay() {
    this.setData({ paying: true })
    try {
      const result = await payOrder(this.data.orderNo)
      this.setData({ detail: this.toView(result.order) })
      wx.showToast({
        title: result.mock ? '模拟支付成功' : '支付成功',
        icon: 'success',
      })
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '支付失败',
        icon: 'none',
      })
    } finally {
      this.setData({ paying: false })
    }
  },

  toView(order: OrderDetail): OrderView {
    const shipping = Number(order.shippingFee)
    const refunded = order.paymentStatus === 'REFUNDED'
    const partialRefunded = order.paymentStatus === 'PARTIALLY_REFUNDED'
    return {
      orderNo: order.orderNo,
      status: order.status,
      statusText: refunded ? '已退款' : partialRefunded ? '部分退款' : orderStatusText(order.status),
      statusClass: refunded ? 'status-refunded' : orderStatusClass(order.status),
      statusNote: this.statusNote(order),
      paymentText: this.paymentText(order.paymentStatus),
      goodsText: formatPrice(order.goodsAmount),
      shippingText: shipping <= 0 ? '免运费' : formatPrice(order.shippingFee),
      discountText: formatPrice(order.discountAmount),
      payableText: formatPrice(order.payableAmount),
      addressText: `${order.address.province}${order.address.city}${order.address.district}${order.address.detail}`,
      items: order.items.map((item) => ({
        ...item,
        priceText: formatPrice(item.unitPrice),
        subtotalText: formatPrice(item.subtotal),
      })),
      remark: order.remark || '',
      logisticsCompany: order.logisticsCompany || '',
      logisticsNo: order.logisticsNo || '',
      createdAtText: formatDateTime(order.createdAt),
      paidAtText: formatDateTime(order.paidAt),
      shippedAtText: formatDateTime(order.shippedAt),
      completedAtText: formatDateTime(order.completedAt),
      cancelledAtText: formatDateTime(order.cancelledAt),
      cancelReason: order.cancelReason || '',
      logs: order.statusLogs.map((log) => ({
        ...log,
        fromText: log.fromStatus ? orderStatusText(log.fromStatus) : '创建',
        toText: orderStatusText(log.toStatus),
        createdText: formatDateTime(log.createdAt),
      })),
      afterSaleNo: order.afterSaleNo || '',
      afterSaleText: afterSaleStatusText(order.afterSaleStatus),
      canApplyAfterSale:
        order.paymentStatus === 'PAID' &&
        !order.afterSaleNo &&
        (order.status === 'PENDING_SHIPMENT' ||
          order.status === 'PENDING_RECEIPT' ||
          order.status === 'COMPLETED'),
      canChangeAddress:
        order.status === 'PENDING_SHIPMENT' &&
        !order.afterSaleNo &&
        (order.paymentStatus === 'PAID' || order.paymentStatus === 'PARTIALLY_REFUNDED'),
      invoiceRequired: order.invoiceRequired,
      invoiceTitle: order.invoiceTitle,
      invoiceTaxNo: order.invoiceTaxNo,
      invoiceEmail: order.invoiceEmail,
    }
  },

  paymentText(paymentStatus: string): string {
    switch (paymentStatus) {
      case 'PAID':
        return '已支付'
      case 'PARTIALLY_REFUNDED':
        return '部分退款'
      case 'REFUNDED':
        return '已退款'
      default:
        return '未支付'
    }
  },

  applyAfterSale() {
    if (!this.data.orderNo) {
      return
    }
    wx.navigateTo({ url: `/pages/after-sale-apply/index?orderNo=${this.data.orderNo}` })
  },

  reviewItem(event: WechatMiniprogram.TouchEvent) {
    const orderItemId = Number(event.currentTarget.dataset.orderItemId)
    const productName = String(event.currentTarget.dataset.productName ?? '')
    if (!orderItemId) {
      return
    }
    wx.navigateTo({
      url: `/pages/review-edit/index?orderNo=${this.data.orderNo}&orderItemId=${orderItemId}&productName=${encodeURIComponent(productName)}`,
    })
  },

  changeAddress() {
    if (!this.data.orderNo) {
      return
    }
    wx.navigateTo({
      url: `/pages/address-list/index?selectForOrder=${this.data.orderNo}`,
    })
  },

  openAfterSale() {
    if (!this.data.detail?.afterSaleNo) {
      return
    }
    wx.navigateTo({
      url: `/pages/after-sale-detail/index?afterSaleNo=${this.data.detail.afterSaleNo}`,
    })
  },

  statusNote(order: OrderDetail): string {
    if (order.paymentStatus === 'REFUNDED') {
      return '订单已退款，如有疑问可在售后记录中查看。'
    }
    if (order.paymentStatus === 'PARTIALLY_REFUNDED') {
      return '订单已部分退款，剩余商品按原流程履约。'
    }
    switch (order.status) {
      case 'PENDING_PAYMENT':
        return '订单待付款。'
      case 'PENDING_SHIPMENT':
        return '商家正在备货，请耐心等待发货。'
      case 'PENDING_RECEIPT':
        return '商品已发出，请确认收货。'
      case 'COMPLETED':
        return '订单已完成，感谢购买。'
      case 'CANCELLED':
        return order.cancelReason ? `订单已取消：${order.cancelReason}` : '订单已取消。'
      default:
        return ''
    }
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },
})
