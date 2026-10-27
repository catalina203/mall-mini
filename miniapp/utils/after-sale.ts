import type { AfterSaleStatus, AfterSaleType } from '../types/api'

export const AFTER_SALE_STATUS_TEXT: Record<AfterSaleStatus, string> = {
  APPLIED: '待审核',
  APPROVED: '待退款',
  WAIT_RETURN: '待退货',
  RETURNED: '退货运输中',
  RETURN_RECEIVED: '已收到退货',
  REFUNDING: '退款中',
  REFUNDED: '已退款',
  REFUND_FAILED: '退款失败',
  REJECTED: '已拒绝',
  CANCELLED: '已取消',
}

export const AFTER_SALE_STATUS_TABS: Array<{ label: string; value: '' | AfterSaleStatus }> = [
  { label: '全部', value: '' },
  { label: '待审核', value: 'APPLIED' },
  { label: '待退货', value: 'WAIT_RETURN' },
  { label: '退款中', value: 'REFUNDING' },
  { label: '已退款', value: 'REFUNDED' },
]

export function afterSaleStatusText(status: AfterSaleStatus | null): string {
  return status ? AFTER_SALE_STATUS_TEXT[status] ?? status : ''
}

export function afterSaleStatusClass(status: AfterSaleStatus): string {
  return `status-${status.toLowerCase().replace(/_/g, '-')}`
}

export function afterSaleTypeText(type: AfterSaleType): string {
  return type === 'RETURN_REFUND' ? '退货退款' : '仅退款'
}
