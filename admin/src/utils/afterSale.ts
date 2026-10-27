import type { AfterSaleStatus, AfterSaleType } from '@/types/admin'

export const AFTER_SALE_STATUS_OPTIONS: Array<{ label: string; value: AfterSaleStatus }> = [
  { label: '待审核', value: 'APPLIED' },
  { label: '待退款', value: 'APPROVED' },
  { label: '待退货', value: 'WAIT_RETURN' },
  { label: '退货运输中', value: 'RETURNED' },
  { label: '已收到退货', value: 'RETURN_RECEIVED' },
  { label: '退款中', value: 'REFUNDING' },
  { label: '已退款', value: 'REFUNDED' },
  { label: '退款失败', value: 'REFUND_FAILED' },
  { label: '已拒绝', value: 'REJECTED' },
  { label: '已取消', value: 'CANCELLED' },
]

const STATUS_TEXT: Record<AfterSaleStatus, string> = {
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

export function afterSaleStatusText(status: AfterSaleStatus): string {
  return STATUS_TEXT[status] ?? status
}

export function afterSaleStatusTagType(
  status: AfterSaleStatus,
): 'primary' | 'success' | 'warning' | 'danger' | 'info' {
  switch (status) {
    case 'APPLIED':
    case 'APPROVED':
      return 'warning'
    case 'WAIT_RETURN':
    case 'RETURNED':
    case 'RETURN_RECEIVED':
    case 'REFUNDING':
      return 'primary'
    case 'REFUNDED':
      return 'success'
    case 'REFUND_FAILED':
      return 'danger'
    default:
      return 'info'
  }
}

export function afterSaleTypeText(type: AfterSaleType): string {
  return type === 'RETURN_REFUND' ? '退货退款' : '仅退款'
}
