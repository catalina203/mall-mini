import { getHome } from '../../services/catalog'
import type { Banner, Category, HomeData, ProductSummary } from '../../types/api'
import { appConfig } from '../../utils/config'
import { setPendingCategory } from '../../utils/category-nav'
import { formatPrice } from '../../utils/format'
import { syncTabBar } from '../../utils/tab-bar'

interface ProductCard extends ProductSummary {
  priceText: string
  stockText: string
}

interface HomeView {
  banners: Banner[]
  categories: Category[]
  recommendedProducts: ProductCard[]
}

Page({
  data: {
    brandName: appConfig.brandName,
    loading: true,
    error: '',
    home: null as HomeView | null,
  },

  onShow() {
    syncTabBar(this, 0)
    if (!this.data.home) {
      void this.load()
    }
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const home: HomeData = await getHome()
      const recommendedProducts = home.recommendedProducts.map((product) => ({
        ...product,
        priceText:
          product.minPrice === product.maxPrice
            ? formatPrice(product.minPrice)
            : `${formatPrice(product.minPrice)}起`,
        stockText: product.totalStock > 0 ? `库存 ${product.totalStock}` : '暂时缺货',
      }))
      this.setData({
        loading: false,
        home: {
          banners: home.banners,
          categories: home.categories.slice(0, 8),
          recommendedProducts,
        },
      })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '首页加载失败',
      })
    }
  },

  openProduct(event: WechatMiniprogram.CustomEvent<{ id: number }>) {
    const id = Number(event.detail.id)
    if (!id) {
      return
    }
    wx.navigateTo({ url: `/pages/product-detail/index?id=${id}` })
  },

  openSearch() {
    wx.navigateTo({ url: '/pages/search/index' })
  },

  openCategory(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (!id) {
      return
    }
    setPendingCategory(id)
    wx.switchTab({ url: '/pages/category/index' })
  },

  openBanner(event: WechatMiniprogram.TouchEvent) {
    const index = Number(event.currentTarget.dataset.index)
    const banner = this.data.home?.banners[index]
    if (!banner) {
      return
    }
    if (banner.linkType === 'PRODUCT' && banner.linkTarget) {
      wx.navigateTo({ url: `/pages/product-detail/index?id=${banner.linkTarget}` })
      return
    }
    if (banner.linkType === 'CATEGORY') {
      setPendingCategory(Number(banner.linkTarget))
      wx.switchTab({ url: '/pages/category/index' })
    }
  },
})
