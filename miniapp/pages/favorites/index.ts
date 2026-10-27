import { listFavorites, removeFavorite } from '../../services/favorite'
import { isLoggedIn } from '../../store/auth'
import type { FavoriteProduct } from '../../types/api'
import { formatDateTime, formatPrice } from '../../utils/format'

interface ProductView extends FavoriteProduct {
  priceText: string
  atText: string
}

Page({
  data: {
    loggedIn: false,
    loading: false,
    loadingMore: false,
    error: '',
    items: [] as ProductView[],
    page: 1,
    pageSize: 20,
    total: 0,
    hasMore: false,
    acting: false,
  },

  onShow() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (loggedIn) {
      void this.load(true)
    } else {
      this.setData({ items: [], loading: false, error: '' })
    }
  },

  async load(reset: boolean) {
    const page = reset ? 1 : this.data.page + 1
    this.setData(reset
      ? { loading: true, error: '', page: 1, hasMore: true }
      : { loadingMore: true, error: '' })
    try {
      const result = await listFavorites(page, this.data.pageSize)
      const items = result.items.map((item) => this.toView(item))
      this.setData({
        items: reset ? items : [...this.data.items, ...items],
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
        error: error instanceof Error ? error.message : '收藏加载失败',
      })
    }
  },

  onReachBottom() {
    if (this.data.hasMore && !this.data.loading && !this.data.loadingMore) {
      void this.load(false)
    }
  },

  async onPullDownRefresh() {
    if (this.data.loggedIn) {
      await this.load(true)
    }
    wx.stopPullDownRefresh()
  },

  openProduct(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (id) {
      wx.navigateTo({ url: `/pages/product-detail/index?id=${id}` })
    }
  },

  async remove(event: WechatMiniprogram.TouchEvent) {
    if (this.data.acting) return
    const id = Number(event.currentTarget.dataset.id)
    if (!id) return
    this.setData({ acting: true })
    try {
      await removeFavorite(id)
      wx.showToast({ title: '已取消收藏', icon: 'none' })
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

  toView(item: FavoriteProduct): ProductView {
    return {
      ...item,
      priceText: formatPrice(item.minPrice),
      atText: formatDateTime(item.at),
    }
  },
})
