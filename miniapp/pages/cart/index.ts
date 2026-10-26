import {
  clearCart,
  deleteCartItem,
  getCart,
  selectCartItems,
  updateCartItem,
} from '../../services/cart'
import { isLoggedIn } from '../../store/auth'
import type { Cart, CartItem } from '../../types/api'
import { formatPrice } from '../../utils/format'
import { syncTabBar } from '../../utils/tab-bar'

interface CartItemView extends CartItem {
  priceText: string
  subtotalText: string
  specText: string
}

interface CartView {
  items: CartItemView[]
  summary: {
    selectedItemCount: number
    selectedQuantity: number
    selectedAmountText: string
  }
}

Page({
  data: {
    loggedIn: false,
    loading: false,
    updating: false,
    error: '',
    cart: null as CartView | null,
    allSelected: false,
    hasItems: false,
  },

  onShow() {
    syncTabBar(this, 2)
    void this.refresh()
  },

  async refresh() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (!loggedIn) {
      this.setData({ cart: null, loading: false, error: '', hasItems: false })
      return
    }
    await this.load()
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const cart = await getCart()
      this.applyCart(cart)
      this.setData({ loading: false })
    } catch (error) {
      this.setData({
        loading: false,
        cart: null,
        hasItems: false,
        error: error instanceof Error ? error.message : '购物车加载失败',
      })
    }
  },

  applyCart(cart: Cart) {
    const items: CartItemView[] = cart.items.map((item) => ({
      ...item,
      priceText: formatPrice(item.price),
      subtotalText: formatPrice(item.subtotal),
      specText: Object.values(item.spec).join(' / ') || '默认规格',
    }))
    this.setData({
      cart: {
        items,
        summary: {
          selectedItemCount: cart.summary.selectedItemCount,
          selectedQuantity: cart.summary.selectedQuantity,
          selectedAmountText: formatPrice(cart.summary.selectedAmount),
        },
      },
      hasItems: items.length > 0,
      allSelected: items.length > 0 && items.every((item) => item.selected),
    })
  },

  async toggleSelect(event: WechatMiniprogram.TouchEvent) {
    if (this.data.updating) {
      return
    }
    const id = Number(event.currentTarget.dataset.id)
    const item = this.data.cart?.items.find((entry) => entry.id === id)
    if (!item) {
      return
    }
    if (!item.available) {
      wx.showToast({ title: item.unavailableReason || '商品不可选', icon: 'none' })
      return
    }
    await this.mutate(() => updateCartItem(id, { selected: !item.selected }))
  },

  async toggleAll() {
    if (this.data.updating || !this.data.hasItems) {
      return
    }
    const nextSelected = !this.data.allSelected
    await this.mutate(() => selectCartItems(nextSelected))
  },

  async changeQuantity(event: WechatMiniprogram.TouchEvent) {
    if (this.data.updating) {
      return
    }
    const id = Number(event.currentTarget.dataset.id)
    const delta = Number(event.currentTarget.dataset.delta)
    const item = this.data.cart?.items.find((entry) => entry.id === id)
    if (!item) {
      return
    }
    if (!item.available) {
      wx.showToast({ title: item.unavailableReason || '商品不可选', icon: 'none' })
      return
    }
    const nextQuantity = item.quantity + delta
    if (nextQuantity < 1) {
      return
    }
    if (nextQuantity > 99) {
      wx.showToast({ title: '单项最多 99 件', icon: 'none' })
      return
    }
    if (nextQuantity > item.stock) {
      wx.showToast({ title: `库存仅剩 ${item.stock} 件`, icon: 'none' })
      return
    }
    await this.mutate(() => updateCartItem(id, { quantity: nextQuantity }))
  },

  removeItem(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (!id || this.data.updating) {
      return
    }
    wx.showModal({
      title: '删除商品',
      content: '确定从购物车删除该商品？',
      success: (result) => {
        if (result.confirm) {
          void this.mutate(() => deleteCartItem(id))
        }
      },
    })
  },

  clearAll() {
    if (this.data.updating || !this.data.hasItems) {
      return
    }
    wx.showModal({
      title: '清空购物车',
      content: '确定清空购物车中的全部商品？',
      success: (result) => {
        if (result.confirm) {
          void this.mutate(() => clearCart())
        }
      },
    })
  },

  async mutate(action: () => Promise<Cart>) {
    this.setData({ updating: true })
    try {
      const cart = await action()
      this.applyCart(cart)
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '操作失败',
        icon: 'none',
      })
    } finally {
      this.setData({ updating: false })
    }
  },

  goShopping() {
    wx.switchTab({ url: '/pages/home/index' })
  },

  goCheckout() {
    const items = this.data.cart?.items ?? []
    const ids = items.filter((item) => item.selected && item.available).map((item) => item.id)
    if (!ids.length) {
      wx.showToast({ title: '请选择可购买的商品', icon: 'none' })
      return
    }
    wx.navigateTo({
      url: `/pages/checkout/index?source=CART&cartItemIds=${ids.join(',')}`,
    })
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },
})
