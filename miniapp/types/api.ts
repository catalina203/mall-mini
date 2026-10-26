export interface ApiResponse<T> {
  code: string
  message: string
  data: T
  requestId?: string
}

export interface PageResult<T> {
  items: T[]
  page: number
  pageSize: number
  total: number
}

export interface Banner {
  id: number
  title: string
  imageUrl: string
  linkType: 'NONE' | 'PRODUCT' | 'CATEGORY'
  linkTarget: string
}

export interface Category {
  id: number
  name: string
  iconUrl: string
  sortOrder: number
  children: Category[]
}

export interface ProductSummary {
  id: number
  name: string
  subtitle: string
  mainImage: string
  minPrice: number | string
  maxPrice: number | string
  totalStock: number
}

export interface Sku {
  id: number
  skuCode: string
  spec: Record<string, string>
  price: number | string
  stock: number
  status: 'ENABLED' | 'DISABLED'
}

export interface ProductImage {
  id: number
  url: string
  sortOrder: number
}

export interface ProductDetail {
  id: number
  name: string
  subtitle: string
  description: string
  mainImage: string
  categoryId: number
  status: 'ON_SALE' | 'OFF_SALE'
  images: ProductImage[]
  skus: Sku[]
}

export interface HomeData {
  banners: Banner[]
  categories: Category[]
  recommendedProducts: ProductSummary[]
}

export interface User {
  id: number
  nickname: string
  avatarUrl: string
  phone: string
  status: string
}

export interface AuthToken {
  token: string
  devMode: boolean
  user: User
}

export interface Address {
  id: number
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
}

export interface AddressPayload {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
  isDefault: boolean
}

export interface CartItem {
  id: number
  skuId: number
  productId: number | null
  productName: string
  productImage: string
  spec: Record<string, string>
  price: number | string
  stock: number
  quantity: number
  selected: boolean
  available: boolean
  unavailableReason: string
  subtotal: number | string
}

export interface CartSummary {
  selectedItemCount: number
  selectedQuantity: number
  selectedAmount: number | string
}

export interface Cart {
  items: CartItem[]
  summary: CartSummary
}

export type OrderStatus =
  | 'PENDING_PAYMENT'
  | 'PENDING_SHIPMENT'
  | 'PENDING_RECEIPT'
  | 'COMPLETED'
  | 'CANCELLED'

export interface AddressSnapshot {
  receiverName: string
  phone: string
  province: string
  city: string
  district: string
  detail: string
}

export interface OrderItem {
  productId: number
  skuId: number
  productName: string
  skuSpec: string
  productImage: string
  unitPrice: number | string
  quantity: number
  subtotal: number | string
}

export interface OrderStatusLog {
  fromStatus: OrderStatus | null
  toStatus: OrderStatus
  operatorType: string
  remark: string
  createdAt: string
}

export interface CheckoutPreview {
  address: AddressSnapshot
  items: OrderItem[]
  goodsAmount: number | string
  discountAmount: number | string
  shippingFee: number | string
  payableAmount: number | string
  freeShippingThreshold: number | string
}

export interface CheckoutPayload {
  addressId: number
  source: 'CART' | 'BUY_NOW'
  cartItemIds?: number[]
  skuId?: number
  quantity?: number
}

export interface CreateOrderPayload extends CheckoutPayload {
  clientRequestNo: string
  remark?: string
}

export interface OrderSummary {
  orderNo: string
  status: OrderStatus
  paymentStatus: string
  payableAmount: number | string
  totalQuantity: number
  firstProductName: string
  firstProductImage: string
  createdAt: string
}

export interface OrderDetail {
  orderNo: string
  status: OrderStatus
  paymentStatus: string
  goodsAmount: number | string
  discountAmount: number | string
  shippingFee: number | string
  payableAmount: number | string
  address: AddressSnapshot
  items: OrderItem[]
  remark: string
  logisticsCompany: string | null
  logisticsNo: string | null
  createdAt: string
  paidAt: string | null
  shippedAt: string | null
  completedAt: string | null
  cancelledAt: string | null
  cancelReason: string | null
  statusLogs: OrderStatusLog[]
}

export interface PaymentConfig {
  mockEnabled: boolean
  channel: string
}

export interface PaymentResult {
  mock: boolean
  channel: string
  order: OrderDetail
}
