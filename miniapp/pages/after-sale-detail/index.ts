import {
  cancelAfterSale,
  getAfterSale,
  submitReturnShipment,
} from '../../services/after-sales'
import { isLoggedIn } from '../../store/auth'
import type {
  AfterSaleDetail,
  AfterSaleItem,
  AfterSaleLog,
  AfterSaleRefund,
  AfterSaleStatus,
} from '../../types/api'
import { formatDateTime, formatPrice } from '../../utils/format'
import {
  afterSaleStatusClass,
  afterSaleStatusText,
  afterSaleTypeText,
} from '../../utils/after-sale'

interface ItemView extends AfterSaleItem {
  priceText: string
  refundText: string
}

interface LogView extends AfterSaleLog {
  fromText: string
  toText: string
  createdText: string
}

interface RefundView extends AfterSaleRefund {
  amountText: string
  successText: string
  statusText: string
}

interface DetailView {
  afterSaleNo: string
  orderNo: string
  typeText: string
  status: AfterSaleStatus
  statusText: string
  statusClass: string
  statusNote: string
  reason: string
  description: string
  refundText: string
  returnCompany: string
  returnTrackingNo: string
  reviewRemark: string
  createdAtText: string
  returnedAtText: string
  refundedAtText: string
  items: ItemView[]
  logs: LogView[]
  refund: RefundView | null
  canCancel: boolean
  canReturn: boolean
}

Page({
  data: {
    afterSaleNo: '',
    loggedIn: true,
    loading: true,
    acting: false,
    submitting: false,
    error: '',
    detail: null as DetailView | null,
    returnCompany: '',
    returnTrackingNo: '',
  },

  onLoad(options: Record<string, string | undefined>) {
    const afterSaleNo = options.afterSaleNo ?? ''
    const loggedIn = isLoggedIn()
    this.setData({ afterSaleNo, loggedIn })
    if (!afterSaleNo) {
      this.setData({ loading: false, error: '缺少售后单号' })
      return
    }
    if (!loggedIn) {
      this.setData({ loading: false })
      return
    }
    void this.load()
  },

  async load() {
    if (!this.data.afterSaleNo) {
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const detail = await getAfterSale(this.data.afterSaleNo)
      this.setData({ detail: this.toView(detail), loading: false })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '售后详情加载失败',
      })
    }
  },

  cancel() {
    if (this.data.acting) {
      return
    }
    wx.showModal({
      title: '取消售后',
      content: '确定取消本次售后申请？取消后如需售后需重新申请。',
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
      const detail = await cancelAfterSale(this.data.afterSaleNo)
      this.setData({ detail: this.toView(detail) })
      wx.showToast({ title: '已取消售后', icon: 'success' })
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '取消失败',
        icon: 'none',
      })
    } finally {
      this.setData({ acting: false })
    }
  },

  onCompanyInput(event: WechatMiniprogram.Input) {
    this.setData({ returnCompany: event.detail.value })
  },

  onTrackingInput(event: WechatMiniprogram.Input) {
    this.setData({ returnTrackingNo: event.detail.value })
  },

  async submitReturn() {
    if (this.data.submitting) {
      return
    }
    const company = this.data.returnCompany.trim()
    const trackingNo = this.data.returnTrackingNo.trim()
    if (!company || !trackingNo) {
      wx.showToast({ title: '请填写物流公司与单号', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      const detail = await submitReturnShipment(this.data.afterSaleNo, company, trackingNo)
      this.setData({
        detail: this.toView(detail),
        returnCompany: '',
        returnTrackingNo: '',
      })
      wx.showToast({ title: '退货物流已提交', icon: 'success' })
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '提交失败',
        icon: 'none',
      })
    } finally {
      this.setData({ submitting: false })
    }
  },

  openOrder() {
    if (this.data.detail) {
      wx.navigateTo({ url: `/pages/order-detail/index?orderNo=${this.data.detail.orderNo}` })
    }
  },

  toView(detail: AfterSaleDetail): DetailView {
    const status = detail.status
    const canCancel = status === 'APPLIED' || status === 'APPROVED' || status === 'WAIT_RETURN'
    return {
      afterSaleNo: detail.afterSaleNo,
      orderNo: detail.orderNo,
      typeText: afterSaleTypeText(detail.type),
      status,
      statusText: afterSaleStatusText(status),
      statusClass: afterSaleStatusClass(status),
      statusNote: this.statusNote(detail),
      reason: detail.reason,
      description: detail.description || '无补充说明',
      refundText: formatPrice(detail.refundAmount),
      returnCompany: detail.returnCompany || '',
      returnTrackingNo: detail.returnTrackingNo || '',
      reviewRemark: detail.reviewRemark || '',
      createdAtText: formatDateTime(detail.createdAt),
      returnedAtText: formatDateTime(detail.returnedAt),
      refundedAtText: formatDateTime(detail.refundedAt),
      items: detail.items.map((item) => ({
        ...item,
        priceText: formatPrice(item.unitPrice),
        refundText: formatPrice(item.refundAmount),
      })),
      logs: detail.logs.map((log) => ({
        ...log,
        fromText: afterSaleStatusText(log.fromStatus) || '创建',
        toText: afterSaleStatusText(log.toStatus),
        createdText: formatDateTime(log.createdAt),
      })),
      refund: detail.refund
        ? {
            ...detail.refund,
            amountText: formatPrice(detail.refund.amount),
            successText: formatDateTime(detail.refund.successAt),
            statusText: this.refundStatusText(detail.refund.status),
          }
        : null,
      canCancel,
      canReturn: status === 'WAIT_RETURN',
    }
  },

  statusNote(detail: AfterSaleDetail): string {
    switch (detail.status) {
      case 'APPLIED':
        return '售后申请已提交，等待商家审核。'
      case 'APPROVED':
        return '商家已审核通过，等待退款处理。'
      case 'WAIT_RETURN':
        return '请将商品寄回并填写退货物流，商家确认收货后退款。'
      case 'RETURNED':
        return '退货物流已提交，等待商家确认收货。'
      case 'RETURN_RECEIVED':
        return '商家已收到退货，等待退款。'
      case 'REFUNDING':
        return '退款处理中，请耐心等待。'
      case 'REFUNDED':
        return '退款已完成，请留意到账通知。'
      case 'REFUND_FAILED':
        return detail.refund?.failedReason || '退款失败，请联系商家处理。'
      case 'REJECTED':
        return detail.reviewRemark ? `售后未通过：${detail.reviewRemark}` : '售后申请未通过。'
      case 'CANCELLED':
        return '售后申请已取消。'
      default:
        return ''
    }
  },

  refundStatusText(status: 'PENDING' | 'SUCCESS' | 'FAILED'): string {
    if (status === 'SUCCESS') {
      return '已退款'
    }
    if (status === 'FAILED') {
      return '退款失败'
    }
    return '退款中'
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },
})
