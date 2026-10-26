import { createAddress, listAddresses, updateAddress } from '../../services/address'
import { isLoggedIn } from '../../store/auth'
import type { Address, AddressPayload } from '../../types/api'

interface AddressForm {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
}

function emptyForm(): AddressForm {
  return {
    receiverName: '',
    phone: '',
    province: '',
    city: '',
    district: '',
    detail: '',
    isDefault: false,
  }
}

Page({
  data: {
    id: 0,
    loading: false,
    saving: false,
    error: '',
    region: [] as string[],
    regionText: '',
    form: emptyForm(),
  },

  onLoad(options: Record<string, string | undefined>) {
    if (!isLoggedIn()) {
      wx.showToast({ title: '请先登录', icon: 'none' })
      wx.switchTab({ url: '/pages/profile/index' })
      return
    }
    const id = Number(options.id ?? 0)
    this.setData({ id })
    wx.setNavigationBarTitle({ title: id ? '编辑地址' : '新增地址' })
    if (id) {
      void this.load(id)
    }
  },

  async load(id: number) {
    this.setData({ loading: true, error: '' })
    try {
      const addresses = await listAddresses()
      const address = addresses.find((item) => item.id === id)
      if (!address) {
        throw new Error('地址不存在')
      }
      this.fillForm(address)
      this.setData({ loading: false })
    } catch (error) {
      this.setData({
        loading: false,
        error: error instanceof Error ? error.message : '地址加载失败',
      })
    }
  },

  retry() {
    if (this.data.id) {
      void this.load(this.data.id)
    }
  },

  fillForm(address: Address) {    const region = [address.province, address.city, address.district]
    this.setData({
      form: {
        receiverName: address.receiverName,
        phone: address.phone,
        province: address.province,
        city: address.city,
        district: address.district,
        detail: address.detail,
        isDefault: address.isDefault,
      },
      region,
      regionText: region.join(' '),
    })
  },

  onNameInput(event: WechatMiniprogram.Input) {
    this.setData({ 'form.receiverName': event.detail.value })
  },

  onPhoneInput(event: WechatMiniprogram.Input) {
    this.setData({ 'form.phone': event.detail.value })
  },

  onDetailInput(event: WechatMiniprogram.Input) {
    this.setData({ 'form.detail': event.detail.value })
  },

  onRegionChange(event: WechatMiniprogram.CustomEvent<{ value: string[] }>) {
    const region = event.detail.value
    const [province = '', city = '', district = ''] = region
    this.setData({
      region,
      regionText: region.join(' '),
      'form.province': province,
      'form.city': city,
      'form.district': district,
    })
  },

  onDefaultChange(event: WechatMiniprogram.CustomEvent<{ value: boolean }>) {
    this.setData({ 'form.isDefault': event.detail.value })
  },

  async save() {
    if (this.data.saving) {
      return
    }
    const form = this.data.form
    const receiverName = form.receiverName.trim()
    const phone = form.phone.trim()
    const detail = form.detail.trim()

    if (!receiverName) {
      wx.showToast({ title: '请输入收货人', icon: 'none' })
      return
    }
    if (!/^1[3-9]\d{9}$/.test(phone)) {
      wx.showToast({ title: '请输入正确的手机号', icon: 'none' })
      return
    }
    if (!form.province || !form.city || !form.district) {
      wx.showToast({ title: '请选择省市区', icon: 'none' })
      return
    }
    if (!detail) {
      wx.showToast({ title: '请输入详细地址', icon: 'none' })
      return
    }

    const payload: AddressPayload = {
      receiverName,
      phone,
      province: form.province,
      city: form.city,
      district: form.district,
      detail,
      isDefault: form.isDefault,
    }

    this.setData({ saving: true })
    try {
      if (this.data.id) {
        await updateAddress(this.data.id, payload)
      } else {
        await createAddress(payload)
      }
      wx.showToast({ title: '已保存', icon: 'success' })
      setTimeout(() => wx.navigateBack(), 400)
    } catch (error) {
      wx.showToast({
        title: error instanceof Error ? error.message : '保存失败',
        icon: 'none',
      })
    } finally {
      this.setData({ saving: false })
    }
  },
})
