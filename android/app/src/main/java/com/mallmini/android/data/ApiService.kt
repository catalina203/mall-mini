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
}
