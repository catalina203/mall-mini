import { getProduct } from '../../services/catalog'
import { addCartItem } from '../../services/cart'
import { isLoggedIn } from '../../store/auth'
import type { ProductDetail, Sku } from '../../types/api'
import { formatPrice } from '../../utils/format'

interface SkuView extends Sku {
  specText: string
  priceText: string
  availability: string
  available: boolean
}

interface ProductView {
  id: number
  name: string
  subtitle: string
  description: string
  priceText: string
  stockText: string
  imageUrls: string[]
  skus: SkuView[]
}

Page({
  data: {
    id: '',
    loading: true,
    error: '',
    product: null as ProductView | null,
    selectedSkuId: 0,
    selectionText: '',
    currentPriceText: '',
    currentStockText: '',
    subtotalText: '',
    quantity: 1,
    maxQuantity: 1,
    canBuy: false,
    adding: false,
  },

  onLoad(options: Record<string, string | undefined>) {
    const id = options.id ?? ''
    this.setData({ id })
    if (!id) {
      this.setData({ loading: false, error: '缺少商品参数' })
      return
    }
    void this.load()
  },

  async load() {
    const id = this.data.id
    if (!id) {
      return
    }
    this.setData({ loading: true, error: '' })
    try {
      const detail: ProductDetail = await getProduct(id)
      const product = this.toView(detail)
      const availableSkus = product.skus.filter((sku) => sku.available)
      const autoSkuId = availableSkus.length === 1 ? availableSkus[0].id : 0

      this.setData({ loading: false, product }, () => {
        this.applySelection(autoSkuId)
      })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '商品加载失败',
      })
    }
  },

  selectSku(event: WechatMiniprogram.TouchEvent) {
    const skuId = Number(event.currentTarget.dataset.id)
    const sku = this.data.product?.skus.find((item) => item.id === skuId)
    if (!sku || !sku.available) {
      return
    }
    this.applySelection(skuId)
  },

  applySelection(skuId: number) {
    const product = this.data.product
    if (!product) {
      return
    }
    const sku = product.skus.find((item) => item.id === skuId && item.available)
    if (!sku) {
      this.setData({
        selectedSkuId: 0,
        selectionText: '请选择规格',
        currentPriceText: product.priceText,
        currentStockText: '',
        subtotalText: '',
        quantity: 1,
        maxQuantity: 1,
        canBuy: false,
      })
      return
    }

    const maxQuantity = Math.min(Math.max(sku.stock, 1), 99)
    this.setData(
      {
        selectedSkuId: sku.id,
        selectionText: sku.specText,
        currentPriceText: sku.priceText,
        currentStockText: `库存 ${sku.stock}`,
        quantity: Math.min(this.data.quantity, maxQuantity),
        maxQuantity,
        canBuy: true,
      },
      () => this.refreshSubtotal(),
    )
  },

  decrease() {
    if (!this.data.canBuy || this.data.quantity <= 1) {
      return
    }
    this.setData({ quantity: this.data.quantity - 1 }, () => this.refreshSubtotal())
  },

  increase() {
    if (!this.data.canBuy || this.data.quantity >= this.data.maxQuantity) {
      return
    }
    this.setData({ quantity: this.data.quantity + 1 }, () => this.refreshSubtotal())
  },

  async addToCart() {
    if (!this.data.canBuy || !this.data.selectedSkuId) {
      wx.showToast({ title: '请先选择规格', icon: 'none' })
      return
    }
    if (!this.requireLogin()) {
      return
    }
    if (this.data.adding) {
      return
    }

    this.setData({ adding: true })
    try {
      await addCartItem(this.data.selectedSkuId, this.data.quantity)
      wx.showToast({ title: '已加入购物车', icon: 'success' })
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '加入购物车失败',
        icon: 'none',
      })
    } finally {
      this.setData({ adding: false })
    }
  },

  buyNow() {
    if (!this.data.canBuy || !this.data.selectedSkuId) {
      wx.showToast({ title: '请先选择规格', icon: 'none' })
      return
    }
    if (!this.requireLogin()) {
      return
    }
    wx.navigateTo({
      url: `/pages/checkout/index?source=BUY_NOW&skuId=${this.data.selectedSkuId}&quantity=${this.data.quantity}`,
    })
  },

  requireLogin(): boolean {
    if (isLoggedIn()) {
      return true
    }
    wx.showModal({
      title: '需要登录',
      content: '登录后才能购买商品。',
      confirmText: '去登录',
      success: (result) => {
        if (result.confirm) {
          wx.switchTab({ url: '/pages/profile/index' })
        }
      },
    })
    return false
  },

  refreshSubtotal() {
    const sku = this.data.product?.skus.find((item) => item.id === this.data.selectedSkuId)
    if (!sku) {
      this.setData({ subtotalText: '' })
      return
    }
    const subtotal = Number(sku.price) * this.data.quantity
    this.setData({ subtotalText: formatPrice(subtotal) })
  },

  toView(detail: ProductDetail): ProductView {
    const enabledSkus = detail.skus.filter((sku) => sku.status === 'ENABLED')
    const prices = enabledSkus.map((sku) => Number(sku.price)).filter((price) => Number.isFinite(price))
    const minPrice = prices.length ? Math.min(...prices) : 0
    const maxPrice = prices.length ? Math.max(...prices) : 0
    const totalStock = enabledSkus.reduce((sum, sku) => sum + Math.max(sku.stock, 0), 0)
    const imageUrls = [detail.mainImage, ...detail.images.map((image) => image.url)]
      .filter((url, index, list) => Boolean(url) && list.indexOf(url) === index)

    return {
      id: detail.id,
      name: detail.name,
      subtitle: detail.subtitle,
      description: detail.description || '暂无描述',
      priceText:
        minPrice === maxPrice
          ? formatPrice(minPrice)
          : `${formatPrice(minPrice)} ~ ${formatPrice(maxPrice)}`,
      stockText: totalStock > 0 ? `可用库存 ${totalStock}` : '暂时缺货',
      imageUrls,
      skus: detail.skus.map((sku) => {
        const available = sku.status === 'ENABLED' && sku.stock > 0
        return {
          ...sku,
          specText: Object.values(sku.spec).join(' / ') || '默认规格',
          priceText: formatPrice(sku.price),
          available,
          availability:
            sku.status !== 'ENABLED' ? '不可用' : sku.stock > 0 ? `库存 ${sku.stock}` : '已售罄',
        }
      }),
    }
  },
})
