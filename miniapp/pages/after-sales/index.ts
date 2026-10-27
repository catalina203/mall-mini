import { listAfterSales } from '../../services/after-sales'
import { isLoggedIn } from '../../store/auth'
import type { AfterSaleStatus, AfterSaleSummary } from '../../types/api'
import { formatDateTime, formatPrice } from '../../utils/format'
import {
  AFTER_SALE_STATUS_TABS,
  afterSaleStatusClass,
  afterSaleStatusText,
  afterSaleTypeText,
} from '../../utils/after-sale'

interface AfterSaleCard extends AfterSaleSummary {
  statusText: string
  statusClass: string
  typeText: string
  amountText: string
  createdAtText: string
}

Page({
  data: {
    loggedIn: false,
    tabs: AFTER_SALE_STATUS_TABS,
    activeStatus: '' as '' | AfterSaleStatus,
    records: [] as AfterSaleCard[],
    page: 1,
    pageSize: 10,
    total: 0,
    loading: false,
    loadingMore: false,
    error: '',
    hasMore: false,
  },

  onShow() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (!loggedIn) {
      this.setData({ records: [], loading: false, error: '', hasMore: false })
      return
    }
    void this.load(true)
  },

  switchStatus(event: WechatMiniprogram.TouchEvent) {
    const status = String(event.currentTarget.dataset.status ?? '') as '' | AfterSaleStatus
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
        ? { loading: true, error: '', records: [], page: 1, hasMore: true }
        : { loadingMore: true, error: '' },
    )
    try {
      const result = await listAfterSales({
        status: this.data.activeStatus || undefined,
        page,
        pageSize: this.data.pageSize,
      })
      const records = result.items.map((item) => this.toCard(item))
      this.setData({
        records: reset ? records : [...this.data.records, ...records],
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
        error: error instanceof Error ? error.message : '售后记录加载失败',
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
    const afterSaleNo = String(event.currentTarget.dataset.afterSaleNo ?? '')
    if (afterSaleNo) {
      wx.navigateTo({ url: `/pages/after-sale-detail/index?afterSaleNo=${afterSaleNo}` })
    }
  },

  goOrders() {
    wx.navigateTo({ url: '/pages/orders/index' })
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },

  toCard(item: AfterSaleSummary): AfterSaleCard {
    return {
      ...item,
      statusText: afterSaleStatusText(item.status),
      statusClass: afterSaleStatusClass(item.status),
      typeText: afterSaleTypeText(item.type),
      amountText: formatPrice(item.refundAmount),
      createdAtText: formatDateTime(item.createdAt),
    }
  },
})
