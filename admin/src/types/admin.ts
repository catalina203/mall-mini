export interface AdminProfile {
  id: number
  username: string
  displayName: string
  role: string
  status: string
}

export interface AdminToken {
  token: string
  admin: AdminProfile
}

export interface AdminCategory {
  id: number
  parentId: number
  parentName: string
  name: string
  iconUrl: string
  sortOrder: number
  status: 'ENABLED' | 'DISABLED'
}

export interface AdminSku {
  id: number
  skuCode: string
  spec: Record<string, string>
  price: number
  stock: number
  status: 'ENABLED' | 'DISABLED'
}

export interface AdminProductSummary {
  id: number
  categoryId: number
  categoryName: string
  name: string
  subtitle: string
  mainImage: string
  status: 'ON_SALE' | 'OFF_SALE'
  sortOrder: number
  minPrice: number
  maxPrice: number
  totalStock: number
  skuCount: number
  updatedAt: string
}

export interface AdminProductDetail {
  id: number
  categoryId: number
  categoryName: string
  name: string
  subtitle: string
  description: string
  mainImage: string
  status: 'ON_SALE' | 'OFF_SALE'
  sortOrder: number
  images: Array<{ id: number; url: string; sortOrder: number }>
  skus: AdminSku[]
}

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PENDING_SHIPMENT'
  | 'PENDING_RECEIPT'
  | 'COMPLETED'
  | 'CANCELLED'

export interface AdminOrderSummary {
  orderNo: string
  userId: number
  status: OrderStatus
  paymentStatus: string
  payableAmount: number
  totalQuantity: number
  receiverName: string
  receiverPhone: string
  createdAt: string
}

export interface AdminOrderDetail {
  orderNo: string
  status: OrderStatus
  paymentStatus: string
  goodsAmount: number
  discountAmount: number
  shippingFee: number
  payableAmount: number
  address: {
    receiverName: string
    phone: string
    province: string
    city: string
    district: string
    detail: string
  }
  items: Array<{
    productId: number
    skuId: number
    productName: string
    skuSpec: string
    productImage: string
    unitPrice: number
    quantity: number
    subtotal: number
  }>
  remark: string
  logisticsCompany: string | null
  logisticsNo: string | null
  createdAt: string
  paidAt: string | null
  shippedAt: string | null
  completedAt: string | null
  cancelledAt: string | null
  cancelReason: string | null
  statusLogs: Array<{
    fromStatus: OrderStatus | null
    toStatus: OrderStatus
    operatorType: string
    remark: string
    createdAt: string
  }>
}

export interface AdminUser {
  id: number
  nickname: string
  avatarUrl: string
  phone: string
  status: 'ACTIVE' | 'DISABLED'
  createdAt: string
}

export interface AdminBanner {
  id: number
  title: string
  imageUrl: string
  linkType: 'NONE' | 'PRODUCT' | 'CATEGORY'
  linkTarget: string
  sortOrder: number
  status: 'ENABLED' | 'DISABLED'
  createdAt: string
}

export interface DashboardSummary {
  days: number
  rangeStart: string
  rangeEnd: string
  generatedAt: string
  orderCount: number
  paidAmount: number
  pendingShipmentCount: number
  pendingReceiptCount: number
  todayOrderCount: number
}
