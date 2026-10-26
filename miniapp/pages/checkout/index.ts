import { listAddresses } from '../../services/address'
import { previewCheckout } from '../../services/checkout'
import { createOrder } from '../../services/order'
import { isLoggedIn } from '../../store/auth'
import type {
  Address,
  AddressSnapshot,
  CheckoutPayload,
  CheckoutPreview,
  CreateOrderPayload,
  OrderItem,
} from '../../types/api'
import { formatPrice } from '../../utils/format'

interface CheckoutItemView extends OrderItem {
  priceText: string
  subtotalText: string
}

interface PreviewView {
  address: AddressSnapshot
  addressText: string
  items: CheckoutItemView[]
  goodsAmountText: string
  discountAmountText: string
  shippingText: string
  payableText: string
  freeShippingText: string
  hasDiscount: boolean
}

function newRequestNo(): string {
  return `c${Date.now().toString(36)}${Math.random().toString(36).slice(2, 10)}`
}

Page({
  data: {
    loggedIn: false,
    loading: true,
    submitting: false,
    error: '',
    addresses: [] as Address[],
    selectedAddressId: 0,
    preview: null as PreviewView | null,
    pickerVisible: false,
    source: 'CART' as 'CART' | 'BUY_NOW',
    cartItemIds: [] as number[],
    skuId: 0,
    quantity: 1,
    requestNo: '',
  },

  onLoad(options: Record<string, string | undefined>) {
    const source = options.source === 'BUY_NOW' ? 'BUY_NOW' : 'CART'
    const cartItemIds = (options.cartItemIds ?? '')
      .split(',')
      .map((value) => Number(value))
      .filter((value) => Number.isFinite(value) && value > 0)
    const skuId = Number(options.skuId ?? 0)
    const quantity = Number(options.quantity ?? 1)
    this.setData({
      loggedIn: isLoggedIn(),
      source,
      cartItemIds,
      skuId: Number.isFinite(skuId) ? skuId : 0,
      quantity: Number.isFinite(quantity) && quantity > 0 ? Math.min(quantity, 99) : 1,
    })
  },

  onShow() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (!loggedIn) {
      this.setData({ loading: false })
      return
    }
    if (!this.data.requestNo) {
      this.setData({ requestNo: newRequestNo() })
    }
    void this.loadAddresses()
  },

  async loadAddresses() {
    this.setData({ loading: true, error: '' })
    try {
      const addresses = await listAddresses()
      if (!addresses.length) {
        this.setData({ addresses: [], preview: null, loading: false })
        return
      }
      const selected =
        addresses.find((address) => address.id === this.data.selectedAddressId) ??
        addresses.find((address) => address.isDefault) ??
        addresses[0]
      this.setData({ addresses })
      await this.loadPreview(selected.id)
    } catch (error) {
      this.setData({
        loading: false,
        preview: null,
        error: error instanceof Error ? error.message : '结算信息加载失败',
      })
    }
  },

  async loadPreview(addressId: number) {
    if (!addressId) {
      return
    }
    const payload = this.buildPayload(addressId)
    if (!payload) {
      this.setData({ preview: null, loading: false, error: '请选择结算商品' })
      return
    }
    this.setData({
      loading: true,
      error: '',
      selectedAddressId: addressId,
      pickerVisible: false,
    })
    try {
      const preview = await previewCheckout(payload)
      this.setData({ preview: this.toPreview(preview), loading: false })
    } catch (error) {
      this.setData({
        preview: null,
        loading: false,
        error: error instanceof Error ? error.message : '结算信息加载失败',
      })
    }
  },

  buildPayload(addressId: number): CheckoutPayload | null {
    if (this.data.source === 'CART') {
      if (!this.data.cartItemIds.length) {
        return null
      }
      return {
        addressId,
        source: 'CART',
        cartItemIds: this.data.cartItemIds,
      }
    }
    if (!this.data.skuId) {
      return null
    }
    return {
      addressId,
      source: 'BUY_NOW',
      skuId: this.data.skuId,
      quantity: this.data.quantity,
    }
  },

  toPreview(preview: CheckoutPreview): PreviewView {
    const shipping = Number(preview.shippingFee)
    const discount = Number(preview.discountAmount)
    const threshold = Number(preview.freeShippingThreshold)
    return {
      address: preview.address,
      addressText: `${preview.address.province}${preview.address.city}${preview.address.district}${preview.address.detail}`,
      items: preview.items.map((item) => ({
        ...item,
        priceText: formatPrice(item.unitPrice),
        subtotalText: formatPrice(item.subtotal),
      })),
      goodsAmountText: formatPrice(preview.goodsAmount),
      discountAmountText: formatPrice(preview.discountAmount),
      shippingText: shipping <= 0 ? '免运费' : formatPrice(preview.shippingFee),
      payableText: formatPrice(preview.payableAmount),
      freeShippingText:
        threshold > 0 ? `满 ${formatPrice(preview.freeShippingThreshold)} 包邮` : '全场包邮',
      hasDiscount: discount > 0,
    }
  },

  chooseAddress() {
    if (!this.data.addresses.length) {
      this.addAddress()
      return
    }
    this.setData({ pickerVisible: true })
  },

  closePicker() {
    this.setData({ pickerVisible: false })
  },

  noop() {
  },

  selectAddress(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (id && id !== this.data.selectedAddressId) {
      void this.loadPreview(id)
    } else {
      this.setData({ pickerVisible: false })
    }
  },

  addAddress() {
    this.setData({ pickerVisible: false })
    wx.navigateTo({ url: '/pages/address-edit/index' })
  },

  async submit() {
    if (this.data.submitting) {
      return
    }
    if (!this.data.preview) {
      wx.showToast({ title: '结算信息未就绪', icon: 'none' })
      return
    }
    if (!isLoggedIn()) {
      this.setData({ loggedIn: false })
      return
    }
    const payload = this.buildPayload(this.data.selectedAddressId)
    if (!payload) {
      wx.showToast({ title: '请选择结算商品', icon: 'none' })
      return
    }

    const orderPayload: CreateOrderPayload = {
      ...payload,
      clientRequestNo: this.data.requestNo,
    }
    this.setData({ submitting: true })
    try {
      const order = await createOrder(orderPayload)
      wx.showToast({ title: '下单成功', icon: 'success' })
      setTimeout(() => {
        wx.redirectTo({ url: `/pages/order-detail/index?orderNo=${order.orderNo}` })
      }, 300)
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '下单失败',
        icon: 'none',
      })
      await this.loadPreview(this.data.selectedAddressId)
    } finally {
      this.setData({ submitting: false })
    }
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },
})
