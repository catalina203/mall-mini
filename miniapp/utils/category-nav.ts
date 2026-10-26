const PENDING_CATEGORY_KEY = 'mallmini_pending_category'

export function setPendingCategory(categoryId: number): void {
  if (categoryId > 0) {
    wx.setStorageSync(PENDING_CATEGORY_KEY, categoryId)
  }
}

export function takePendingCategory(): number {
  const stored = Number(wx.getStorageSync(PENDING_CATEGORY_KEY))
  wx.removeStorageSync(PENDING_CATEGORY_KEY)
  return Number.isFinite(stored) && stored > 0 ? stored : 0
}
