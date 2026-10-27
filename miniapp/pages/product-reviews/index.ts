import { getReviewSummary, listProductReviews } from '../../services/review'
import type { Review, ReviewSummary } from '../../types/api'
import { formatDateTime } from '../../utils/format'

interface ReviewView extends Review {
  createdText: string
  stars: string
}

Page({
  data: {
    productId: 0,
    loading: true,
    loadingMore: false,
    error: '',
    summary: null as ReviewSummary | null,
    averageText: '0.0',
    items: [] as ReviewView[],
    page: 1,
    pageSize: 10,
    total: 0,
    hasMore: false,
  },

  onLoad(options: Record<string, string | undefined>) {
    const productId = Number(options.productId ?? 0)
    this.setData({ productId })
    if (!productId) {
      this.setData({ loading: false, error: '缺少商品参数' })
      return
    }
    void this.load()
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const [summary, result] = await Promise.all([
        getReviewSummary(this.data.productId),
        listProductReviews(this.data.productId, 1, this.data.pageSize),
      ])
      this.setData({
        summary,
        averageText: Number(summary.averageRating).toFixed(1),
        items: result.items.map((item) => this.toView(item)),
        page: 1,
        total: result.total,
        hasMore: result.total > this.data.pageSize,
        loading: false,
      })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '评价加载失败',
      })
    }
  },

  async loadMore() {
    if (!this.data.hasMore || this.data.loadingMore) return
    const page = this.data.page + 1
    this.setData({ loadingMore: true })
    try {
      const result = await listProductReviews(this.data.productId, page, this.data.pageSize)
      this.setData({
        items: [...this.data.items, ...result.items.map((item) => this.toView(item))],
        page,
        hasMore: page * this.data.pageSize < result.total,
        loadingMore: false,
      })
    } catch (error) {
      this.setData({ loadingMore: false })
      wx.showToast({
        title: error instanceof Error ? error.message : '加载失败',
        icon: 'none',
      })
    }
  },

  onReachBottom() {
    void this.loadMore()
  },

  toView(item: Review): ReviewView {
    return {
      ...item,
      createdText: formatDateTime(item.createdAt),
      stars: '★★★★★'.slice(0, item.rating) + '☆☆☆☆☆'.slice(0, 5 - item.rating),
    }
  },
})
