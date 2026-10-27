import { deleteAddress, listAddresses, setDefaultAddress } from '../../services/address'
import { changeOrderAddress } from '../../services/order'
import { isLoggedIn } from '../../store/auth'
import type { Address } from '../../types/api'

Page({
  data: {
    loggedIn: false,
    loading: false,
    error: '',
    addresses: [] as Address[],
    acting: false,
    selectForOrder: '',
  },

  onLoad(options: Record<string, string | undefined>) {
    const selectForOrder = options.selectForOrder ?? ''
    this.setData({ selectForOrder })
    if (selectForOrder) {
      wx.setNavigationBarTitle({ title: '选择收货地址' })
    }
  },

  onShow() {
    const loggedIn = isLoggedIn()
    this.setData({ loggedIn })
    if (loggedIn) {
      void this.load()
    } else {
      this.setData({ addresses: [], loading: false, error: '' })
    }
  },

  async load() {
    this.setData({ loading: true, error: '' })
    try {
      const addresses = await listAddresses()
      this.setData({ addresses, loading: false })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '地址加载失败',
      })
    }
  },

  async selectAddressForOrder(event: WechatMiniprogram.TouchEvent) {
    if (!this.data.selectForOrder || this.data.acting) {
      return
    }
    const id = Number(event.currentTarget.dataset.id)
    if (!id) {
      return
    }
    this.setData({ acting: true })
    try {
      await changeOrderAddress(this.data.selectForOrder, id)
      wx.showToast({ title: '地址已更新', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 300)
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '修改失败',
        icon: 'none',
      })
    } finally {
      this.setData({ acting: false })
    }
  },

  addAddress() {
    wx.navigateTo({ url: '/pages/address-edit/index' })
  },

  editAddress(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (id) {
      wx.navigateTo({ url: `/pages/address-edit/index?id=${id}` })
    }
  },

  async makeDefault(event: WechatMiniprogram.TouchEvent) {
    if (this.data.acting) {
      return
    }
    const id = Number(event.currentTarget.dataset.id)
    if (!id) {
      return
    }
    this.setData({ acting: true })
    try {
      await setDefaultAddress(id)
      await this.load()
      wx.showToast({ title: '已设为默认', icon: 'success' })
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '设置失败',
        icon: 'none',
      })
    } finally {
      this.setData({ acting: false })
    }
  },

  removeAddress(event: WechatMiniprogram.TouchEvent) {
    const id = Number(event.currentTarget.dataset.id)
    if (!id || this.data.acting) {
      return
    }
    wx.showModal({
      title: '删除地址',
      content: '确定删除该收货地址？历史订单中的地址快照不受影响。',
      success: (result) => {
        if (result.confirm) {
          void this.doRemove(id)
        }
      },
    })
  },

  async doRemove(id: number) {
    this.setData({ acting: true })
    try {
      await deleteAddress(id)
      await this.load()
      wx.showToast({ title: '已删除', icon: 'none' })
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '删除失败',
        icon: 'none',
      })
    } finally {
      this.setData({ acting: false })
    }
  },

  goLogin() {
    wx.switchTab({ url: '/pages/profile/index' })
  },
})
