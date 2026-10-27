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
  salesCount: number
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
  newProducts: ProductSummary[]
  hotProducts: ProductSummary[]
}

export interface User {
  id: number
  nickname: string
  avatarUrl: string
  phone: string
  status: string
  points: number
  memberLevel: string
  memberLevelLabel: string
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
  orderItemId: number | null
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
  couponId: number | null
  couponName: string
}

export interface CheckoutPayload {
  addressId: number
  source: 'CART' | 'BUY_NOW'
  cartItemIds?: number[]
  skuId?: number
  quantity?: number
  couponId?: number
}

export interface CreateOrderPayload extends CheckoutPayload {
  clientRequestNo: string
  remark?: string
  invoiceRequired?: boolean
  invoiceTitle?: string
  invoiceTaxNo?: string
  invoiceEmail?: string
  couponId?: number
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
  afterSaleNo: string | null
  afterSaleStatus: AfterSaleStatus | null
  invoiceRequired: boolean
  invoiceTitle: string
  invoiceTaxNo: string
  invoiceEmail: string
  adminRemark: string
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

export type AfterSaleType = 'REFUND_ONLY' | 'RETURN_REFUND'
export type AfterSaleStatus =
  | 'APPLIED'
  | 'APPROVED'
  | 'WAIT_RETURN'
  | 'RETURNED'
  | 'RETURN_RECEIVED'
  | 'REFUNDING'
  | 'REFUNDED'
  | 'REFUND_FAILED'
  | 'REJECTED'
  | 'CANCELLED'

export interface AfterSaleItem {
  orderItemId: number
  productId: number
  skuId: number
  productName: string
  skuSpec: string
  productImage: string
  unitPrice: number | string
  quantity: number
  refundAmount: number | string
}

export interface AfterSaleLog {
  fromStatus: AfterSaleStatus | null
  toStatus: AfterSaleStatus
  operatorType: string
  remark: string
  createdAt: string
}

export interface AfterSaleRefund {
  refundNo: string
  channel: string
  amount: number | string
  status: 'PENDING' | 'SUCCESS' | 'FAILED'
  transactionNo: string | null
  successAt: string | null
  failedReason: string | null
}

export interface AfterSaleSummary {
  afterSaleNo: string
  orderNo: string
  type: AfterSaleType
  status: AfterSaleStatus
  refundAmount: number | string
  firstProductName: string
  firstProductImage: string
  totalQuantity: number
  createdAt: string
}

export interface AfterSaleDetail {
  afterSaleNo: string
  orderNo: string
  type: AfterSaleType
  status: AfterSaleStatus
  reason: string
  description: string
  refundAmount: number | string
  returnCompany: string | null
  returnTrackingNo: string | null
  reviewRemark: string | null
  reviewedAt: string | null
  returnedAt: string | null
  refundedAt: string | null
  createdAt: string
  items: AfterSaleItem[]
  logs: AfterSaleLog[]
  refund: AfterSaleRefund | null
}

export interface CreateAfterSalePayload {
  type: AfterSaleType
  reason: string
  description?: string
  items: Array<{ orderItemId: number; quantity: number }>
}

export interface UserNotification {
  id: number
  templateCode: string
  title: string
  content: string
  bizType: string
  bizNo: string
  read: boolean
  readAt: string | null
  createdAt: string
}

export type CouponType = 'FIXED' | 'PERCENT'
export type CouponScope = 'ALL' | 'CATEGORY' | 'PRODUCT'
export type CouponStatus = 'UNUSED' | 'USED' | 'EXPIRED'

export interface CouponTemplate {
  id: number
  name: string
  type: CouponType
  value: number | string
  minAmount: number | string
  scope: CouponScope
  scopeId: number | null
  totalCount: number
  claimedCount: number
  perUserLimit: number
  validFrom: string
  validTo: string
  status: string
  createdAt: string
}

export interface UserCoupon {
  id: number
  couponNo: string
  templateId: number
  name: string
  type: CouponType
  value: number | string
  minAmount: number | string
  scope: CouponScope
  scopeId: number | null
  status: CouponStatus
  orderNo: string | null
  validFrom: string
  validTo: string
}

export interface Review {
  id: number
  productId: number
  skuId: number
  userNickname: string
  rating: number
  content: string
  anonymous: boolean
  status: string
  reply: string
  createdAt: string
}

export interface ReviewSummary {
  productId: number
  averageRating: number | string
  reviewCount: number
}

export interface FavoriteProduct {
  productId: number
  name: string
  subtitle: string
  mainImage: string
  minPrice: number | string
  status: string
  at: string
}


export interface MemberInfo {
  points: number
  totalPoints: number
  level: string
  levelLabel: string
  nextLevelPoints: number | null
  nextLevelLabel: string | null
}

export interface PointsLog {
  id: number
  changeType: string
  pointsDelta: number
  balanceAfter: number
  remark: string
  createdAt: string
}

export type SupportStatus = 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'

export interface SupportTicket {
  id: number
  ticketNo: string
  userId: number
  userNickname: string
  orderNo: string | null
  type: string
  subject: string
  content: string
  status: SupportStatus
  reply: string
  repliedAt: string | null
  createdAt: string
}
