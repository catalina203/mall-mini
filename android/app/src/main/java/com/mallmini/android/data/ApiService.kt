package com.mallmini.android.data

import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

interface ApiService {

    @POST("auth/dev-login")
    suspend fun devLogin(@Body body: DevLoginRequest): ApiResponse<AuthSession>

    @GET("me")
    suspend fun me(): ApiResponse<User>

    @GET("home")
    suspend fun home(): ApiResponse<HomeData>

    @GET("categories")
    suspend fun categories(): ApiResponse<List<Category>>

    @GET("products")
    suspend fun products(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
        @Query("categoryId") categoryId: Long? = null,
        @Query("keyword") keyword: String? = null,
        @Query("sort") sort: String? = null,
    ): ApiResponse<PageResult<ProductSummary>>

    @GET("products/{id}")
    suspend fun product(@Path("id") id: Long): ApiResponse<ProductDetail>

    @GET("me/cart")
    suspend fun cart(): ApiResponse<Cart>

    @POST("me/cart")
    suspend fun addCart(@Body body: AddCartRequest): ApiResponse<Cart>

    @PUT("me/cart/{id}")
    suspend fun updateCartItem(
        @Path("id") id: Long,
        @Body body: UpdateCartRequest,
    ): ApiResponse<Cart>

    @PUT("me/cart/selection")
    suspend fun selectCart(@Body body: CartSelectionRequest): ApiResponse<Cart>

    @DELETE("me/cart/{id}")
    suspend fun deleteCartItem(@Path("id") id: Long): ApiResponse<Cart>

    @DELETE("me/cart")
    suspend fun clearCart(): ApiResponse<Cart>

    @GET("me/addresses")
    suspend fun addresses(): ApiResponse<List<Address>>

    @POST("me/addresses")
    suspend fun createAddress(@Body body: AddressPayload): ApiResponse<Address>

    @PUT("me/addresses/{id}")
    suspend fun updateAddress(
        @Path("id") id: Long,
        @Body body: AddressPayload,
    ): ApiResponse<Address>

    @DELETE("me/addresses/{id}")
    suspend fun deleteAddress(@Path("id") id: Long): ApiResponse<Any?>

    @PUT("me/addresses/{id}/default")
    suspend fun setDefaultAddress(@Path("id") id: Long): ApiResponse<Any?>

    @POST("me/checkout/preview")
    suspend fun checkoutPreview(@Body body: CheckoutRequest): ApiResponse<CheckoutPreview>

    @POST("me/orders")
    suspend fun createOrder(@Body body: CreateOrderRequest): ApiResponse<OrderDetail>

    @GET("me/orders")
    suspend fun orders(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): ApiResponse<PageResult<OrderSummary>>

    @GET("me/orders/{orderNo}")
    suspend fun order(@Path("orderNo") orderNo: String): ApiResponse<OrderDetail>

    @POST("me/orders/{orderNo}/cancel")
    suspend fun cancelOrder(
        @Path("orderNo") orderNo: String,
        @Body body: CancelOrderRequest,
    ): ApiResponse<OrderDetail>

    @POST("me/orders/{orderNo}/confirm-receipt")
    suspend fun confirmReceipt(@Path("orderNo") orderNo: String): ApiResponse<OrderDetail>

    @POST("me/orders/{orderNo}/pay")
    suspend fun payOrder(@Path("orderNo") orderNo: String): ApiResponse<PaymentResult>

    @GET("payment/config")
    suspend fun paymentConfig(): ApiResponse<PaymentConfig>

    @GET("search/hot")
    suspend fun hotKeywords(@Query("limit") limit: Int = 10): ApiResponse<List<String>>

    @POST("me/orders/{orderNo}/address")
    suspend fun changeOrderAddress(
        @Path("orderNo") orderNo: String,
        @Body body: ChangeAddressRequest,
    ): ApiResponse<OrderDetail>

    @GET("me/after-sales")
    suspend fun afterSales(
        @Query("status") status: String? = null,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): ApiResponse<PageResult<AfterSaleSummary>>

    @GET("me/after-sales/{afterSaleNo}")
    suspend fun afterSale(@Path("afterSaleNo") afterSaleNo: String): ApiResponse<AfterSaleDetail>

    @POST("me/orders/{orderNo}/after-sales")
    suspend fun createAfterSale(
        @Path("orderNo") orderNo: String,
        @Body body: CreateAfterSaleRequest,
    ): ApiResponse<AfterSaleDetail>

    @POST("me/after-sales/{afterSaleNo}/cancel")
    suspend fun cancelAfterSale(@Path("afterSaleNo") afterSaleNo: String): ApiResponse<AfterSaleDetail>

    @POST("me/after-sales/{afterSaleNo}/return-shipment")
    suspend fun submitReturnShipment(
        @Path("afterSaleNo") afterSaleNo: String,
        @Body body: ReturnShipmentRequest,
    ): ApiResponse<AfterSaleDetail>

    @GET("me/coupons")
    suspend fun myCoupons(@Query("status") status: String? = null): ApiResponse<List<UserCoupon>>

    @GET("me/coupons/claimable")
    suspend fun claimableCoupons(): ApiResponse<List<CouponTemplate>>

    @POST("me/coupons/{templateId}/claim")
    suspend fun claimCoupon(@Path("templateId") templateId: Long): ApiResponse<UserCoupon>

    @GET("me/member")
    suspend fun member(): ApiResponse<MemberInfo>

    @GET("me/points/logs")
    suspend fun pointsLogs(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30,
    ): ApiResponse<PageResult<PointsLog>>

    @GET("products/{id}/reviews")
    suspend fun productReviews(
        @Path("id") productId: Long,
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): ApiResponse<PageResult<Review>>

    @GET("products/{id}/reviews/summary")
    suspend fun reviewSummary(@Path("id") productId: Long): ApiResponse<ReviewSummary>

    @POST("me/orders/{orderNo}/reviews")
    suspend fun createReview(
        @Path("orderNo") orderNo: String,
        @Body body: CreateReviewRequest,
    ): ApiResponse<Review>

    @GET("me/favorites")
    suspend fun favorites(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
    ): ApiResponse<PageResult<FavoriteProduct>>

    @GET("me/favorites/{productId}/status")
    suspend fun favoriteStatus(@Path("productId") productId: Long): ApiResponse<Boolean>

    @POST("me/favorites/{productId}")
    suspend fun addFavorite(@Path("productId") productId: Long): ApiResponse<Any?>

    @DELETE("me/favorites/{productId}")
    suspend fun removeFavorite(@Path("productId") productId: Long): ApiResponse<Any?>

    @GET("me/browse-history")
    suspend fun history(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 50,
    ): ApiResponse<PageResult<FavoriteProduct>>

    @POST("me/browse-history/{productId}")
    suspend fun recordHistory(@Path("productId") productId: Long): ApiResponse<Any?>

    @DELETE("me/browse-history")
    suspend fun clearHistory(): ApiResponse<Any?>

    @GET("me/support-tickets")
    suspend fun supportTickets(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 20,
    ): ApiResponse<PageResult<SupportTicket>>

    @POST("me/support-tickets")
    suspend fun createTicket(@Body body: CreateTicketRequest): ApiResponse<SupportTicket>

    @POST("me/support-tickets/{ticketNo}/close")
    suspend fun closeTicket(@Path("ticketNo") ticketNo: String): ApiResponse<Any?>

    @GET("me/notifications")
    suspend fun notifications(
        @Query("page") page: Int = 1,
        @Query("pageSize") pageSize: Int = 30,
    ): ApiResponse<PageResult<UserNotification>>

    @POST("me/notifications/{id}/read")
    suspend fun markNotificationRead(@Path("id") id: Long): ApiResponse<Any?>
}
