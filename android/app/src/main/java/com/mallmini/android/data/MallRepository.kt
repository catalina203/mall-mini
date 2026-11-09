package com.mallmini.android.data

import com.google.gson.Gson
import com.google.gson.JsonParser
import com.mallmini.android.BuildConfig
import okhttp3.OkHttpClient
import okhttp3.logging.HttpLoggingInterceptor
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import java.io.IOException
import java.util.concurrent.TimeUnit

class MallRepository(private val prefs: Preferences) {

    private val gson = Gson()

    init {
        ApiConfig.baseUrl = prefs.baseUrl
    }
    private val client = OkHttpClient.Builder()
        .connectTimeout(12, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(25, TimeUnit.SECONDS)
        .addInterceptor { chain ->
            val builder = chain.request().newBuilder()
                .header("Accept", "application/json")
            if (prefs.token.isNotBlank()) {
                builder.header("Authorization", "Bearer ${prefs.token}")
            }
            chain.proceed(builder.build())
        }
        .apply {
            if (BuildConfig.DEBUG) {
                addInterceptor(HttpLoggingInterceptor().apply {
                    level = HttpLoggingInterceptor.Level.BASIC
                })
            }
        }
        .build()

    @Volatile
    private var api: ApiService = buildApi(prefs.baseUrl)

    fun updateBaseUrl(baseUrl: String) {
        ApiConfig.baseUrl = baseUrl
        api = buildApi(baseUrl)
    }

    suspend fun devLogin(openId: String, nickname: String): Result<AuthSession> =
        call { api.devLogin(DevLoginRequest(openId, nickname)) }
            .onSuccess { prefs.saveSession(it.token, it.user) }

    suspend fun me(): Result<User> = call { api.me() }

    suspend fun home(): Result<HomeData> = call { api.home() }

    suspend fun categories(): Result<List<Category>> = call { api.categories() }

    suspend fun products(
        categoryId: Long? = null,
        keyword: String? = null,
        sort: String? = null,
    ): Result<PageResult<ProductSummary>> =
        call {
            api.products(
                categoryId = categoryId,
                keyword = keyword?.takeIf { it.isNotBlank() },
                sort = sort,
            )
        }

    suspend fun product(id: Long): Result<ProductDetail> = call { api.product(id) }

    suspend fun cart(): Result<Cart> = call { api.cart() }

    suspend fun addCart(skuId: Long, quantity: Int): Result<Cart> =
        call { api.addCart(AddCartRequest(skuId, quantity)) }

    suspend fun updateCart(id: Long, quantity: Int? = null, selected: Boolean? = null): Result<Cart> =
        call { api.updateCartItem(id, UpdateCartRequest(quantity, selected)) }

    suspend fun selectCart(selected: Boolean, ids: List<Long>? = null): Result<Cart> =
        call { api.selectCart(CartSelectionRequest(selected, ids)) }

    suspend fun deleteCartItem(id: Long): Result<Cart> = call { api.deleteCartItem(id) }

    suspend fun clearCart(): Result<Cart> = call { api.clearCart() }

    suspend fun addresses(): Result<List<Address>> = call { api.addresses() }

    suspend fun createAddress(payload: AddressPayload): Result<Address> =
        call { api.createAddress(payload) }

    suspend fun updateAddress(id: Long, payload: AddressPayload): Result<Address> =
        call { api.updateAddress(id, payload) }

    suspend fun deleteAddress(id: Long): Result<Unit> = action { api.deleteAddress(id) }

    suspend fun setDefaultAddress(id: Long): Result<Unit> = action { api.setDefaultAddress(id) }

    suspend fun checkoutPreview(request: CheckoutRequest): Result<CheckoutPreview> =
        call { api.checkoutPreview(request) }

    suspend fun createOrder(request: CreateOrderRequest): Result<OrderDetail> =
        call { api.createOrder(request) }

    suspend fun orders(status: String? = null): Result<PageResult<OrderSummary>> =
        call { api.orders(status = status?.takeIf { it.isNotBlank() }) }

    suspend fun order(orderNo: String): Result<OrderDetail> = call { api.order(orderNo) }

    suspend fun cancelOrder(orderNo: String, reason: String): Result<OrderDetail> =
        call { api.cancelOrder(orderNo, CancelOrderRequest(reason)) }

    suspend fun confirmReceipt(orderNo: String): Result<OrderDetail> =
        call { api.confirmReceipt(orderNo) }

    suspend fun payOrder(orderNo: String): Result<PaymentResult> = call { api.payOrder(orderNo) }

    suspend fun paymentConfig(): Result<PaymentConfig> = call { api.paymentConfig() }

    suspend fun hotKeywords(): Result<List<String>> = call { api.hotKeywords() }

    suspend fun changeOrderAddress(orderNo: String, addressId: Long): Result<OrderDetail> =
        call { api.changeOrderAddress(orderNo, ChangeAddressRequest(addressId)) }

    suspend fun afterSales(status: String? = null): Result<PageResult<AfterSaleSummary>> =
        call { api.afterSales(status = status?.takeIf { it.isNotBlank() }) }

    suspend fun afterSale(afterSaleNo: String): Result<AfterSaleDetail> =
        call { api.afterSale(afterSaleNo) }

    suspend fun createAfterSale(orderNo: String, request: CreateAfterSaleRequest): Result<AfterSaleDetail> =
        call { api.createAfterSale(orderNo, request) }

    suspend fun cancelAfterSale(afterSaleNo: String): Result<AfterSaleDetail> =
        call { api.cancelAfterSale(afterSaleNo) }

    suspend fun submitReturnShipment(
        afterSaleNo: String,
        company: String,
        trackingNo: String,
    ): Result<AfterSaleDetail> =
        call { api.submitReturnShipment(afterSaleNo, ReturnShipmentRequest(company, trackingNo)) }

    suspend fun myCoupons(status: String? = null): Result<List<UserCoupon>> =
        call { api.myCoupons(status = status?.takeIf { it.isNotBlank() }) }

    suspend fun claimableCoupons(): Result<List<CouponTemplate>> = call { api.claimableCoupons() }

    suspend fun claimCoupon(templateId: Long): Result<UserCoupon> = call { api.claimCoupon(templateId) }

    suspend fun member(): Result<MemberInfo> = call { api.member() }

    suspend fun pointsLogs(): Result<PageResult<PointsLog>> = call { api.pointsLogs() }

    suspend fun productReviews(productId: Long): Result<PageResult<Review>> =
        call { api.productReviews(productId) }

    suspend fun reviewSummary(productId: Long): Result<ReviewSummary> =
        call { api.reviewSummary(productId) }

    suspend fun createReview(orderNo: String, request: CreateReviewRequest): Result<Review> =
        call { api.createReview(orderNo, request) }

    suspend fun favorites(): Result<PageResult<FavoriteProduct>> = call { api.favorites() }

    suspend fun favoriteStatus(productId: Long): Result<Boolean> = call { api.favoriteStatus(productId) }

    suspend fun addFavorite(productId: Long): Result<Unit> = action { api.addFavorite(productId) }

    suspend fun removeFavorite(productId: Long): Result<Unit> = action { api.removeFavorite(productId) }

    suspend fun history(): Result<PageResult<FavoriteProduct>> = call { api.history() }

    suspend fun recordHistory(productId: Long): Result<Unit> = action { api.recordHistory(productId) }

    suspend fun clearHistory(): Result<Unit> = action { api.clearHistory() }

    suspend fun supportTickets(): Result<PageResult<SupportTicket>> = call { api.supportTickets() }

    suspend fun createTicket(request: CreateTicketRequest): Result<SupportTicket> =
        call { api.createTicket(request) }

    suspend fun closeTicket(ticketNo: String): Result<Unit> = action { api.closeTicket(ticketNo) }

    suspend fun notifications(): Result<PageResult<UserNotification>> = call { api.notifications() }

    suspend fun markNotificationRead(id: Long): Result<Unit> = action { api.markNotificationRead(id) }

    private fun buildApi(baseUrl: String): ApiService {
        val normalized = if (baseUrl.endsWith("/")) baseUrl else "$baseUrl/"
        return Retrofit.Builder()
            .baseUrl(normalized)
            .client(client)
            .addConverterFactory(GsonConverterFactory.create(gson))
            .build()
            .create(ApiService::class.java)
    }

    private suspend fun <T : Any> call(block: suspend () -> ApiResponse<T>): Result<T> {
        return try {
            val response = block()
            if (response.code == "OK" && response.data != null) {
                Result.success(response.data)
            } else {
                Result.failure(ApiException(0, response.message.ifBlank { "请求失败" }))
            }
        } catch (error: HttpException) {
            if (error.code() == 401) {
                prefs.clearSession()
            }
            Result.failure(ApiException(error.code(), parseError(error)))
        } catch (error: IOException) {
            Result.failure(ApiException(-1, "无法连接服务器，请检查网络与服务器地址"))
        } catch (error: Exception) {
            Result.failure(ApiException(0, error.message ?: "请求失败"))
        }
    }

    private suspend fun action(block: suspend () -> ApiResponse<*>): Result<Unit> {
        return try {
            val response = block()
            if (response.code == "OK") {
                Result.success(Unit)
            } else {
                Result.failure(ApiException(0, response.message.ifBlank { "操作失败" }))
            }
        } catch (error: HttpException) {
            if (error.code() == 401) {
                prefs.clearSession()
            }
            Result.failure(ApiException(error.code(), parseError(error)))
        } catch (error: IOException) {
            Result.failure(ApiException(-1, "无法连接服务器，请检查网络与服务器地址"))
        } catch (error: Exception) {
            Result.failure(ApiException(0, error.message ?: "操作失败"))
        }
    }

    private fun parseError(error: HttpException): String {
        val raw = runCatching { error.response()?.errorBody()?.string() }.getOrNull()
        val message = runCatching {
            JsonParser.parseString(raw).asJsonObject.get("message")?.asString
        }.getOrNull()
        if (!message.isNullOrBlank()) {
            return message
        }
        return when (error.code()) {
            400 -> "请求参数不正确"
            401 -> "登录已过期，请重新登录"
            403 -> "没有操作权限"
            404 -> "请求的内容不存在"
            409 -> "操作冲突，请刷新后重试"
            429 -> "操作过于频繁，请稍后再试"
            else -> "服务器错误（${error.code()}）"
        }
    }
}
