package com.mallmini.android.data

data class ApiResponse<T>(
    val code: String = "",
    val message: String = "",
    val data: T? = null,
    val requestId: String? = null,
)

data class PageResult<T>(
    val items: List<T> = emptyList(),
    val page: Long = 1,
    val pageSize: Long = 10,
    val total: Long = 0,
)

data class Banner(
    val id: Long = 0,
    val title: String = "",
    val imageUrl: String = "",
    val linkType: String = "",
    val linkTarget: String = "",
)

data class Category(
    val id: Long = 0,
    val name: String = "",
    val iconUrl: String = "",
    val sortOrder: Int = 0,
    val children: List<Category> = emptyList(),
)

data class ProductSummary(
    val id: Long = 0,
    val name: String = "",
    val subtitle: String = "",
    val mainImage: String = "",
    val minPrice: Double = 0.0,
    val maxPrice: Double = 0.0,
    val totalStock: Int = 0,
    val salesCount: Int = 0,
)

data class Sku(
    val id: Long = 0,
    val skuCode: String = "",
    val spec: Map<String, String> = emptyMap(),
    val price: Double = 0.0,
    val stock: Int = 0,
    val status: String = "",
)

data class ProductImage(
    val id: Long = 0,
    val url: String = "",
    val sortOrder: Int = 0,
)

data class ProductDetail(
    val id: Long = 0,
    val name: String = "",
    val subtitle: String = "",
    val description: String = "",
    val mainImage: String = "",
    val categoryId: Long = 0,
    val status: String = "",
    val images: List<ProductImage> = emptyList(),
    val skus: List<Sku> = emptyList(),
)

data class HomeData(
    val banners: List<Banner> = emptyList(),
    val categories: List<Category> = emptyList(),
    val recommendedProducts: List<ProductSummary> = emptyList(),
    val newProducts: List<ProductSummary> = emptyList(),
    val hotProducts: List<ProductSummary> = emptyList(),
)

data class User(
    val id: Long = 0,
    val nickname: String = "",
    val avatarUrl: String = "",
    val phone: String = "",
    val status: String = "",
    val points: Int = 0,
    val memberLevel: String = "NORMAL",
    val memberLevelLabel: String = "普通会员",
)

data class AuthSession(
    val token: String = "",
    val devMode: Boolean = true,
    val user: User = User(),
)

data class CartItem(
    val id: Long = 0,
    val skuId: Long = 0,
    val productId: Long? = null,
    val productName: String = "",
    val productImage: String = "",
    val spec: Map<String, String> = emptyMap(),
    val price: Double = 0.0,
    val stock: Int = 0,
    val quantity: Int = 0,
    val selected: Boolean = false,
    val available: Boolean = false,
    val unavailableReason: String = "",
    val subtotal: Double = 0.0,
)

data class CartSummary(
    val selectedItemCount: Int = 0,
    val selectedQuantity: Int = 0,
    val selectedAmount: Double = 0.0,
)

data class Cart(
    val items: List<CartItem> = emptyList(),
    val summary: CartSummary = CartSummary(),
)

data class Address(
    val id: Long = 0,
    val receiverName: String = "",
    val phone: String = "",
    val province: String = "",
    val city: String = "",
    val district: String = "",
    val detail: String = "",
    val isDefault: Boolean = false,
)

data class AddressSnapshot(
    val receiverName: String = "",
    val phone: String = "",
    val province: String = "",
    val city: String = "",
    val district: String = "",
    val detail: String = "",
)

data class OrderItem(
    val productId: Long = 0,
    val skuId: Long = 0,
    val productName: String = "",
    val skuSpec: String = "",
    val productImage: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 0,
    val subtotal: Double = 0.0,
    val orderItemId: Long? = null,
)

data class CheckoutPreview(
    val address: AddressSnapshot = AddressSnapshot(),
    val items: List<OrderItem> = emptyList(),
    val goodsAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val shippingFee: Double = 0.0,
    val payableAmount: Double = 0.0,
    val freeShippingThreshold: Double = 0.0,
    val couponId: Long? = null,
    val couponName: String = "",
)

data class OrderStatusLog(
    val fromStatus: String? = null,
    val toStatus: String = "",
    val operatorType: String = "",
    val remark: String = "",
    val createdAt: String = "",
)

data class OrderSummary(
    val orderNo: String = "",
    val status: String = "",
    val paymentStatus: String = "",
    val payableAmount: Double = 0.0,
    val totalQuantity: Int = 0,
    val firstProductName: String = "",
    val firstProductImage: String = "",
    val createdAt: String = "",
)

data class OrderDetail(
    val orderNo: String = "",
    val status: String = "",
    val paymentStatus: String = "",
    val goodsAmount: Double = 0.0,
    val discountAmount: Double = 0.0,
    val shippingFee: Double = 0.0,
    val payableAmount: Double = 0.0,
    val address: AddressSnapshot = AddressSnapshot(),
    val items: List<OrderItem> = emptyList(),
    val remark: String = "",
    val logisticsCompany: String? = null,
    val logisticsNo: String? = null,
    val createdAt: String = "",
    val paidAt: String? = null,
    val shippedAt: String? = null,
    val completedAt: String? = null,
    val cancelledAt: String? = null,
    val cancelReason: String? = null,
    val statusLogs: List<OrderStatusLog> = emptyList(),
    val afterSaleNo: String? = null,
    val afterSaleStatus: String? = null,
    val invoiceRequired: Boolean = false,
    val invoiceTitle: String = "",
    val invoiceTaxNo: String = "",
    val invoiceEmail: String = "",
    val couponId: Long? = null,
    val couponName: String = "",
    val adminRemark: String = "",
)

data class PaymentConfig(
    val mockEnabled: Boolean = false,
    val channel: String = "",
)

data class PaymentResult(
    val mock: Boolean = false,
    val channel: String = "",
    val order: OrderDetail = OrderDetail(),
)

data class DevLoginRequest(
    val openId: String,
    val nickname: String,
)

data class AddCartRequest(
    val skuId: Long,
    val quantity: Int,
)

data class UpdateCartRequest(
    val quantity: Int? = null,
    val selected: Boolean? = null,
)

data class CartSelectionRequest(
    val selected: Boolean,
    val ids: List<Long>? = null,
)

data class AddressPayload(
    val receiverName: String,
    val phone: String,
    val province: String,
    val city: String,
    val district: String,
    val detail: String,
    val isDefault: Boolean,
)

data class CheckoutRequest(
    val addressId: Long,
    val source: String,
    val cartItemIds: List<Long>? = null,
    val skuId: Long? = null,
    val quantity: Int? = null,
    val couponId: Long? = null,
)

data class CreateOrderRequest(
    val addressId: Long,
    val source: String,
    val cartItemIds: List<Long>? = null,
    val skuId: Long? = null,
    val quantity: Int? = null,
    val clientRequestNo: String,
    val remark: String = "",
    val couponId: Long? = null,
    val invoiceRequired: Boolean = false,
    val invoiceTitle: String = "",
    val invoiceTaxNo: String = "",
    val invoiceEmail: String = "",
)

data class CancelOrderRequest(
    val reason: String,
)

class ApiException(
    val statusCode: Int,
    override val message: String,
) : Exception(message)


data class ChangeAddressRequest(val addressId: Long)

data class AfterSaleItem(
    val orderItemId: Long = 0,
    val productId: Long = 0,
    val skuId: Long = 0,
    val productName: String = "",
    val skuSpec: String = "",
    val productImage: String = "",
    val unitPrice: Double = 0.0,
    val quantity: Int = 0,
    val refundAmount: Double = 0.0,
)

data class AfterSaleLog(
    val fromStatus: String? = null,
    val toStatus: String = "",
    val operatorType: String = "",
    val remark: String = "",
    val createdAt: String = "",
)

data class RefundInfo(
    val refundNo: String = "",
    val channel: String = "",
    val amount: Double = 0.0,
    val status: String = "",
    val transactionNo: String? = null,
    val successAt: String? = null,
    val failedReason: String? = null,
)

data class AfterSaleSummary(
    val afterSaleNo: String = "",
    val orderNo: String = "",
    val type: String = "",
    val status: String = "",
    val refundAmount: Double = 0.0,
    val firstProductName: String = "",
    val firstProductImage: String = "",
    val totalQuantity: Int = 0,
    val createdAt: String = "",
)

data class AfterSaleDetail(
    val afterSaleNo: String = "",
    val orderNo: String = "",
    val type: String = "",
    val status: String = "",
    val reason: String = "",
    val description: String = "",
    val refundAmount: Double = 0.0,
    val returnCompany: String? = null,
    val returnTrackingNo: String? = null,
    val reviewRemark: String? = null,
    val reviewedAt: String? = null,
    val returnedAt: String? = null,
    val refundedAt: String? = null,
    val createdAt: String = "",
    val items: List<AfterSaleItem> = emptyList(),
    val logs: List<AfterSaleLog> = emptyList(),
    val refund: RefundInfo? = null,
)

data class CreateAfterSaleRequest(
    val type: String,
    val reason: String,
    val description: String = "",
    val items: List<AfterSaleItemRequest>,
)

data class AfterSaleItemRequest(val orderItemId: Long, val quantity: Int)

data class ReturnShipmentRequest(val company: String, val trackingNo: String)

data class CouponTemplate(
    val id: Long = 0,
    val name: String = "",
    val type: String = "",
    val value: Double = 0.0,
    val minAmount: Double = 0.0,
    val scope: String = "",
    val scopeId: Long? = null,
    val totalCount: Int = 0,
    val claimedCount: Int = 0,
    val perUserLimit: Int = 1,
    val validFrom: String = "",
    val validTo: String = "",
    val status: String = "",
    val createdAt: String = "",
)

data class UserCoupon(
    val id: Long = 0,
    val couponNo: String = "",
    val templateId: Long = 0,
    val name: String = "",
    val type: String = "",
    val value: Double = 0.0,
    val minAmount: Double = 0.0,
    val scope: String = "",
    val scopeId: Long? = null,
    val status: String = "",
    val orderNo: String? = null,
    val validFrom: String = "",
    val validTo: String = "",
)

data class MemberInfo(
    val points: Int = 0,
    val totalPoints: Int = 0,
    val level: String = "",
    val levelLabel: String = "",
    val nextLevelPoints: Int? = null,
    val nextLevelLabel: String? = null,
)

data class PointsLog(
    val id: Long = 0,
    val changeType: String = "",
    val pointsDelta: Int = 0,
    val balanceAfter: Int = 0,
    val remark: String = "",
    val createdAt: String = "",
)

data class Review(
    val id: Long = 0,
    val productId: Long = 0,
    val skuId: Long = 0,
    val userNickname: String = "",
    val rating: Int = 0,
    val content: String = "",
    val anonymous: Boolean = false,
    val status: String = "",
    val reply: String = "",
    val createdAt: String = "",
)

data class ReviewSummary(
    val productId: Long = 0,
    val averageRating: Double = 0.0,
    val reviewCount: Long = 0,
)

data class CreateReviewRequest(
    val orderItemId: Long,
    val rating: Int,
    val content: String = "",
    val anonymous: Boolean = false,
)

data class FavoriteProduct(
    val productId: Long = 0,
    val name: String = "",
    val subtitle: String = "",
    val mainImage: String = "",
    val minPrice: Double = 0.0,
    val status: String = "",
    val at: String = "",
)

data class SupportTicket(
    val id: Long = 0,
    val ticketNo: String = "",
    val userId: Long = 0,
    val userNickname: String = "",
    val orderNo: String? = null,
    val type: String = "",
    val subject: String = "",
    val content: String = "",
    val status: String = "",
    val reply: String = "",
    val repliedAt: String? = null,
    val createdAt: String = "",
)

data class CreateTicketRequest(
    val orderNo: String? = null,
    val type: String,
    val subject: String,
    val content: String,
)

data class UserNotification(
    val id: Long = 0,
    val templateCode: String = "",
    val title: String = "",
    val content: String = "",
    val bizType: String = "",
    val bizNo: String = "",
    val read: Boolean = false,
    val readAt: String? = null,
    val createdAt: String = "",
)
