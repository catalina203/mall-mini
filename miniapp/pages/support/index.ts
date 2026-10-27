import { closeTicket, createTicket, listTickets } from '../../services/support'
import { isLoggedIn } from '../../store/auth'
import type { SupportTicket } from '../../types/api'
import { formatDateTime } from '../../utils/format'

const TYPES = [
  { value: 'ORDER', label: '订单问题' },
  { value: 'PRODUCT', label: '商品问题' },
  { value: 'REFUND', label: '退款/售后' },
  { value: 'OTHER', label: '其他问题' },
]

const STATUS_TEXT: Record<string, string> = {
  OPEN: '待处理',
  PROCESSING: '处理中',
  RESOLVED: '已回复',
  CLOSED: '已关闭',
}

interface TicketView extends SupportTicket {
  statusText: string
  typeText: string
  createdText: string
  repliedText: string
}

Page({
  data: {
    loggedIn: false,
    loading: false,
    submitting: false,
    error: '',
    types: TYPES,
    typeIndex: 0,
    orderNo: '',
    subject: '',
    content: '',
    tickets: [] as TicketView[],
    faqs: [
      { q: '如何申请退款？', a: '进入订单详情，点击“申请售后”，选择仅退款或退货退款并提交。' },
      { q: '积分怎么获得？', a: '订单完成后按实付金额 1 元 = 1 积分，整单退款会扣回。' },
      { q: '运费怎么计算？', a: '结算页会按商品绑定的运费模板和收货省份计算，最终以服务端金额为准。' },
    ],
  },

  onLoad(options: Record<string, string | undefined>) {
    this.setData({ orderNo: options.orderNo ?? '' })
  },

  onShow() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (loggedIn) {
      void this.load()
    } else {
      this.setData({ tickets: [], loading: false })
    }
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const result = await listTickets(1, 20)
      this.setData({
        tickets: result.items.map((item) => this.toView(item)),
        loading: false,
      })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '工单加载失败',
      })
    }
  },

  onTypeChange(event: WechatMiniprogram.CustomEvent<{ value: string }>) {
    this.setData({ typeIndex: Number(event.detail.value) })
  },

  onOrderInput(event: WechatMiniprogram.Input) {
    this.setData({ orderNo: event.detail.value })
  },

  onSubjectInput(event: WechatMiniprogram.Input) {
    this.setData({ subject: event.detail.value })
  },

  onContentInput(event: WechatMiniprogram.Input) {
    this.setData({ content: event.detail.value })
  },

  async submit() {
    if (this.data.submitting) return
    if (!this.data.subject.trim() || !this.data.content.trim()) {
      wx.showToast({ title: '请填写标题和问题描述', icon: 'none' })
      return
    }
    this.setData({ submitting: true })
    try {
      await createTicket({
        orderNo: this.data.orderNo.trim() || undefined,
        type: TYPES[this.data.typeIndex].value,
        subject: this.data.subject.trim(),
        content: this.data.content.trim(),
      })
      wx.showToast({ title: '工单已提交', icon: 'success' })
      this.setData({ subject: '', content: '', orderNo: '' })
      await this.load()
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '提交失败',
        icon: 'none',
      })
    } finally {
      this.setData({ submitting: false })
    }
  },

  close(event: WechatMiniprogram.TouchEvent) {
    const ticketNo = String(event.currentTarget.dataset.ticketNo ?? '')
    if (!ticketNo) return
    wx.showModal({
      title: '关闭工单',
      content: '确定关闭该工单？',
      success: (result) => {
        if (result.confirm) {
          void this.doClose(ticketNo)
        }
      },
    })
  },

  async doClose(ticketNo: string) {
    try {
      await closeTicket(ticketNo)
      wx.showToast({ title: '已关闭', icon: 'none' })
      await this.load()
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '操作失败',
        icon: 'none',
      })
    }
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },

  toView(item: SupportTicket): TicketView {
    return {
      ...item,
      statusText: STATUS_TEXT[item.status] ?? item.status,
      typeText: TYPES.find((type) => type.value === item.type)?.label ?? item.type,
      createdText: formatDateTime(item.createdAt),
      repliedText: formatDateTime(item.repliedAt),
    }
  },
})
