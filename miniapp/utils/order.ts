import type { OrderStatus } from '../types/api'

export const ORDER_STATUS_TABS: Array<{ label: string; value: '' | OrderStatus }> = [
  { label: '全部', value: '' },
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

export function orderStatusClass(status: OrderStatus): string {
  return `status-${status.toLowerCase().replace(/_/g, '-')}`
}
