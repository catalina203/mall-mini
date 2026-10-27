import { listProducts } from '../../services/catalog'
import { listHotKeywords } from '../../services/search'
import type { ProductSummary } from '../../types/api'
import { formatPrice } from '../../utils/format'

const RECENT_KEY = 'mallmini_recent_searches'
const MAX_RECENT = 10

interface ProductCardItem extends ProductSummary {
  priceText: string
  stockText: string
}

Page({
  data: {
    keyword: '',
    searchedKeyword: '',
    recent: [] as string[],
    hot: [] as string[],
    products: [] as ProductCardItem[],
    page: 1,
    pageSize: 10,
    total: 0,
    loading: false,
    loadingMore: false,
    error: '',
    hasMore: false,
    searched: false,
  },

  onLoad() {
    this.setData({ recent: this.readRecent() })
    void this.loadHot()
  },

  async loadHot() {
    try {
      this.setData({ hot: await listHotKeywords(10) })
    } catch {
      this.setData({ hot: [] })
    }
  },

  onKeywordInput(event: WechatMiniprogram.Input) {
    this.setData({ keyword: event.detail.value })
  },

  clearKeyword() {
    this.setData({
      keyword: '',
      searchedKeyword: '',
      products: [],
      total: 0,
      hasMore: false,
      error: '',
      searched: false,
    })
  },

  search() {
    const keyword = this.data.keyword.trim()
    if (!keyword) {
      wx.showToast({ title: '请输入搜索关键词', icon: 'none' })
      return
    }
    this.saveRecent(keyword)
    this.setData({ keyword, searchedKeyword: keyword, searched: true })
    void this.loadProducts(true)
  },

  useRecent(event: WechatMiniprogram.TouchEvent) {
    const keyword = String(event.currentTarget.dataset.keyword ?? '')
    if (!keyword) {
      return
    }
    this.setData({ keyword }, () => this.search())
  },

  clearRecent() {
    wx.showModal({
      title: '清空最近搜索',
      content: '仅清除本机搜索记录，不影响服务端数据。',
      success: (result) => {
        if (result.confirm) {
          wx.removeStorageSync(RECENT_KEY)
          this.setData({ recent: [] })
        }
      },
    })
  },

  async loadProducts(reset: boolean) {
    const keyword = this.data.searchedKeyword
    if (!keyword) {
      return
    }
    const page = reset ? 1 : this.data.page + 1
    this.setData(
      reset
        ? { loading: true, error: '', products: [], page: 1, hasMore: true }
        : { loadingMore: true, error: '' },
    )
    try {
      const result = await listProducts({
        page,
        pageSize: this.data.pageSize,
        keyword,
      })
      const items = result.items.map((product) => this.toCard(product))
      this.setData({
        products: reset ? items : [...this.data.products, ...items],
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
        error: error instanceof Error ? error.message : '搜索失败',
      })
    }
  },

  onReachBottom() {
    if (!this.data.searched || this.data.loading || this.data.loadingMore || !this.data.hasMore) {
      return
    }
    void this.loadProducts(false)
  },

  async onPullDownRefresh() {
    if (this.data.searched) {
      await this.loadProducts(true)
    }
    wx.stopPullDownRefresh()
  },

  retry() {
    void this.loadProducts(true)
  },

  openProduct(event: WechatMiniprogram.CustomEvent<{ id: number }>) {
    const id = Number(event.detail.id)
    if (id) {
      wx.navigateTo({ url: `/pages/product-detail/index?id=${id}` })
    }
  },

  readRecent(): string[] {
    const stored = wx.getStorageSync(RECENT_KEY)
    if (!Array.isArray(stored)) {
      return []
    }
    return stored.filter((item): item is string => typeof item === 'string').slice(0, MAX_RECENT)
  },

  saveRecent(keyword: string) {
    const recent = [keyword, ...this.data.recent.filter((item) => item !== keyword)].slice(0, MAX_RECENT)
    this.setData({ recent })
    wx.setStorageSync(RECENT_KEY, recent)
  },

  toCard(product: ProductSummary): ProductCardItem {
    return {
      ...product,
      priceText:
        product.minPrice === product.maxPrice
          ? formatPrice(product.minPrice)
          : `${formatPrice(product.minPrice)}起`,
      stockText: product.totalStock > 0 ? `库存 ${product.totalStock}` : '暂时缺货',
    }
  },
})
