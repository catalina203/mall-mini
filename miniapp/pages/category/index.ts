import { getCategories, listProducts } from '../../services/catalog'
import type { Category, ProductSummary } from '../../types/api'
import { takePendingCategory } from '../../utils/category-nav'
import { formatPrice } from '../../utils/format'
import { syncTabBar } from '../../utils/tab-bar'

interface ProductCardItem extends ProductSummary {
  priceText: string
  stockText: string
}

Page({
  data: {
    categories: [] as Category[],
    children: [] as Category[],
    activeRootId: 0,
    activeCategoryId: 0,
    products: [] as ProductCardItem[],
    page: 1,
    pageSize: 10,
    total: 0,
    loading: true,
    loadingMore: false,
    error: '',
    hasMore: true,
    pendingCategoryId: 0,
  },

  onLoad() {
    void this.loadCategories()
  },

  onShow() {
    syncTabBar(this, 1)
    const pendingCategoryId = takePendingCategory()
    if (!pendingCategoryId) {
      return
    }
    if (this.data.categories.length) {
      this.applyCategory(pendingCategoryId)
    } else {
      this.setData({ pendingCategoryId })
    }
  },

  async loadCategories() {
    this.setData({ loading: true, error: '' })
    try {
      const categories = await getCategories()
      if (!categories.length) {
        this.setData({ categories: [], children: [], products: [], loading: false, hasMore: false })
        return
      }
      this.setData({ categories }, () => {
        const targetId = this.data.pendingCategoryId || categories[0].id
        this.applyCategory(targetId)
      })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '分类加载失败',
      })
    }
  },

  applyCategory(categoryId: number) {
    const categories = this.data.categories
    if (!categories.length) {
      this.setData({ pendingCategoryId: categoryId })
      return
    }
    const root =
      categories.find((category) => category.id === categoryId) ??
      categories.find((category) =>
        (category.children ?? []).some((child) => child.id === categoryId),
      ) ??
      categories[0]
    const activeCategoryId = root.id === categoryId ? root.id : categoryId

    this.setData({
      activeRootId: root.id,
      activeCategoryId,
      children: root.children ?? [],
      pendingCategoryId: 0,
    })
    void this.loadProducts(true)
  },

  selectRoot(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (!id) {
      return
    }
    this.applyCategory(id)
  },

  selectChild(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (!id || id === this.data.activeCategoryId) {
      return
    }
    this.setData({ activeCategoryId: id })
    void this.loadProducts(true)
  },

  async loadProducts(reset: boolean) {
    if (!this.data.activeCategoryId) {
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
        categoryId: this.data.activeCategoryId,
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
        error: error instanceof Error ? error.message : '商品加载失败',
      })
    }
  },

  onReachBottom() {
    if (this.data.loading || this.data.loadingMore || !this.data.hasMore) {
      return
    }
    void this.loadProducts(false)
  },

  async onPullDownRefresh() {
    if (!this.data.categories.length) {
      await this.loadCategories()
    } else {
      await this.loadProducts(true)
    }
    wx.stopPullDownRefresh()
  },

  retry() {
    if (!this.data.categories.length) {
      void this.loadCategories()
      return
    }
    void this.loadProducts(true)
  },

  openProduct(event: WechatMiniprogram.CustomEvent<{ id: number }>) {
    const id = Number(event.detail.id)
    if (id) {
      wx.navigateTo({ url: `/pages/product-detail/index?id=${id}` })
    }
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
