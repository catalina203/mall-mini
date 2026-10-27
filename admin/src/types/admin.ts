export interface AdminProfile {
  id: number
  username: string
  displayName: string
  role: string
  status: string
  permissions: string[]
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
  shippingTemplateId: number | null
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
    orderItemId: number | null
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
  afterSaleNo: string | null
  afterSaleStatus: string | null
  invoiceRequired: boolean
  invoiceTitle: string
  invoiceTaxNo: string
  invoiceEmail: string
  adminRemark: string
}

export interface AdminUser {
  id: number
  nickname: string
  avatarUrl: string
  phone: string
  status: 'ACTIVE' | 'DISABLED'
  points: number
  memberLevel: string
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

export interface DashboardTrend {
  date: string
  orderCount: number
  paidAmount: number
}

export interface DashboardSummary {
  days: number
  rangeStart: string
  rangeEnd: string
  generatedAt: string
  orderCount: number
  paidOrderCount: number
  paidAmount: number
  refundAmount: number
  netAmount: number
  averageOrderValue: number
  refundRate: number
  pendingShipmentCount: number
  pendingReceiptCount: number
  pendingAfterSaleCount: number
  lowStockCount: number
  todayOrderCount: number
  trend: DashboardTrend[]
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

export interface AdminAfterSaleSummary {
  afterSaleNo: string
  orderNo: string
  type: AfterSaleType
  status: AfterSaleStatus
  refundAmount: number
  firstProductName: string
  firstProductImage: string
  totalQuantity: number
  createdAt: string
}

export interface AdminAfterSaleDetail {
  afterSaleNo: string
  orderNo: string
  type: AfterSaleType
  status: AfterSaleStatus
  reason: string
  description: string
  refundAmount: number
  returnCompany: string | null
  returnTrackingNo: string | null
  reviewRemark: string | null
  reviewedAt: string | null
  returnedAt: string | null
  refundedAt: string | null
  createdAt: string
  items: Array<{
    orderItemId: number
    productId: number
    skuId: number
    productName: string
    skuSpec: string
    productImage: string
    unitPrice: number
    quantity: number
    refundAmount: number
  }>
  logs: Array<{
    fromStatus: AfterSaleStatus | null
    toStatus: AfterSaleStatus
    operatorType: string
    remark: string
    createdAt: string
  }>
  refund: {
    refundNo: string
    channel: string
    amount: number
    status: 'PENDING' | 'SUCCESS' | 'FAILED'
    transactionNo: string | null
    successAt: string | null
    failedReason: string | null
  } | null
}

export interface AdminStockLog {
  id: number
  skuId: number
  productId: number
  skuCode: string
  productName: string
  changeType: string
  quantityDelta: number
  stockBefore: number
  stockAfter: number
  bizType: string
  bizNo: string
  operatorType: string
  operatorId: number | null
  remark: string
  createdAt: string
}

export interface AdminStockAlert {
  skuId: number
  productId: number
  skuCode: string
  productName: string
  specText: string
  stock: number
  threshold: number
  price: number
}

export interface AdminNotification {
  id: number
  channel: string
  recipientType: string
  recipientId: number
  templateCode: string
  title: string
  content: string
  bizType: string
  bizNo: string
  status: string
  retryCount: number
  errorMessage: string | null
  sentAt: string | null
  createdAt: string
}

export interface AdminOperationLog {
  id: number
  adminId: number
  adminUsername: string
  action: string
  targetType: string
  targetId: string
  detail: string
  requestId: string
  createdAt: string
}

export interface AdminCouponTemplate {
  id: number
  name: string
  type: 'FIXED' | 'PERCENT'
  value: number
  minAmount: number
  scope: 'ALL' | 'CATEGORY' | 'PRODUCT'
  scopeId: number | null
  totalCount: number
  claimedCount: number
  perUserLimit: number
  validFrom: string
  validTo: string
  status: 'ENABLED' | 'DISABLED'
  createdAt: string
}

export interface AdminShippingRule {
  id?: number
  regionCode: string
  firstUnit: number
  firstFee: number
  additionalUnit: number
  additionalFee: number
  freeThreshold: number
  status: 'ENABLED' | 'DISABLED'
  sortOrder: number
}

export interface AdminShippingTemplate {
  id: number
  name: string
  defaultFee: number
  freeThreshold: number
  status: 'ENABLED' | 'DISABLED'
  rules: AdminShippingRule[]
}

export interface AdminReview {
  id: number
  productId: number
  skuId: number
  userNickname: string
  rating: number
  content: string
  anonymous: boolean
  status: 'PENDING' | 'APPROVED' | 'REJECTED'
  reply: string
  createdAt: string
}

export interface AdminSupportTicket {
  id: number
  ticketNo: string
  userId: number
  userNickname: string
  orderNo: string | null
  type: string
  subject: string
  content: string
  status: 'OPEN' | 'PROCESSING' | 'RESOLVED' | 'CLOSED'
  reply: string
  repliedAt: string | null
  createdAt: string
}
