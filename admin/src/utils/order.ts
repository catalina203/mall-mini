import type { OrderStatus } from '@/types/admin'

export const ORDER_STATUS_OPTIONS: Array<{ label: string; value: OrderStatus }> = [
  { label: '待付款', value: 'PENDING_PAYMENT' },
  { label: '待发货', value: 'PENDING_SHIPMENT' },
  { label: '待收货', value: 'PENDING_RECEIPT' },
  { label: '已完成', value: 'COMPLETED' },
  { label: '已取消', value: 'CANCELLED' },
]

const STATUS_TEXT: Record<OrderStatus, string> = {
  PENDING_PAYMENT: '待付款',
  PENDING_SHIPMENT: '待发货',
  PENDING_RECEIPT: '待收货',
  COMPLETED: '已完成',
  CANCELLED: '已取消',
}

export function orderStatusText(status: OrderStatus): string {
  return STATUS_TEXT[status] ?? status
}

export function orderStatusTagType(status: OrderStatus): 'warning' | 'primary' | 'success' | 'info' {
  switch (status) {
    case 'PENDING_PAYMENT':
      return 'warning'
    case 'PENDING_SHIPMENT':
      return 'primary'
    case 'PENDING_RECEIPT':
      return 'primary'
    case 'COMPLETED':
      return 'success'
    default:
      return 'info'
  }
}
