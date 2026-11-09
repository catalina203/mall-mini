package com.mallmini.android.ui

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.mallmini.android.data.Address
import com.mallmini.android.data.AddressPayload
import com.mallmini.android.data.AfterSaleDetail
import com.mallmini.android.data.AfterSaleSummary
import com.mallmini.android.data.ApiException
import com.mallmini.android.data.AfterSaleItemRequest
import com.mallmini.android.data.CouponTemplate
import com.mallmini.android.data.CreateAfterSaleRequest
import com.mallmini.android.data.CreateReviewRequest
import com.mallmini.android.data.CreateTicketRequest
import com.mallmini.android.data.FavoriteProduct
import com.mallmini.android.data.MemberInfo
import com.mallmini.android.data.PointsLog
import com.mallmini.android.data.ProductDetail
import com.mallmini.android.data.Review
import com.mallmini.android.data.ReviewSummary
import com.mallmini.android.data.SupportTicket
import com.mallmini.android.data.UserCoupon
import com.mallmini.android.data.UserNotification
import com.mallmini.android.data.Cart
import com.mallmini.android.data.Category
import com.mallmini.android.data.CheckoutPreview
import com.mallmini.android.data.CheckoutRequest
import com.mallmini.android.data.CreateOrderRequest
import com.mallmini.android.data.HomeData
import com.mallmini.android.data.MallRepository
import com.mallmini.android.data.OrderDetail
import com.mallmini.android.data.OrderSummary
import com.mallmini.android.data.PageResult
import com.mallmini.android.data.PaymentConfig
import com.mallmini.android.data.Preferences
import com.mallmini.android.data.ProductSummary
import com.mallmini.android.data.User
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

class MallViewModel(application: Application) : AndroidViewModel(application) {

    private val prefs = Preferences(application)
    private val repository = MallRepository(prefs)

    private val _baseUrl = MutableStateFlow(prefs.baseUrl)
    val baseUrl: StateFlow<String> = _baseUrl

    private val _loggedIn = MutableStateFlow(prefs.token.isNotBlank())
    val loggedIn: StateFlow<Boolean> = _loggedIn

    private val _user = MutableStateFlow(prefs.user())
    val user: StateFlow<User?> = _user

    private val _loginBusy = MutableStateFlow(false)
    val loginBusy: StateFlow<Boolean> = _loginBusy

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError

    private val _busy = MutableStateFlow(false)
    val busy: StateFlow<Boolean> = _busy

    private val _messages = MutableSharedFlow<String>(extraBufferCapacity = 20)
    val messages = _messages.asSharedFlow()

    private val _home = MutableStateFlow(UiState<HomeData>())
    val home: StateFlow<UiState<HomeData>> = _home

    private val _products = MutableStateFlow(UiState<PageResult<ProductSummary>>())
    val products: StateFlow<UiState<PageResult<ProductSummary>>> = _products

    private val _product = MutableStateFlow(UiState<ProductDetail>())
    val product: StateFlow<UiState<ProductDetail>> = _product

    private val _cart = MutableStateFlow(UiState<Cart>())
    val cart: StateFlow<UiState<Cart>> = _cart

    private val _addresses = MutableStateFlow(UiState<List<Address>>())
    val addresses: StateFlow<UiState<List<Address>>> = _addresses

    private val _checkout = MutableStateFlow(UiState<CheckoutPreview>())
    val checkout: StateFlow<UiState<CheckoutPreview>> = _checkout

    private val _orders = MutableStateFlow(UiState<PageResult<OrderSummary>>())
    val orders: StateFlow<UiState<PageResult<OrderSummary>>> = _orders

    private val _orderDetail = MutableStateFlow(UiState<OrderDetail>())
    val orderDetail: StateFlow<UiState<OrderDetail>> = _orderDetail

    private val _paymentConfig = MutableStateFlow(PaymentConfig())
    val paymentConfig: StateFlow<PaymentConfig> = _paymentConfig

    private val _hotKeywords = MutableStateFlow<List<String>>(emptyList())
    val hotKeywords: StateFlow<List<String>> = _hotKeywords

    private val _afterSales = MutableStateFlow(UiState<PageResult<AfterSaleSummary>>())
    val afterSales: StateFlow<UiState<PageResult<AfterSaleSummary>>> = _afterSales

    private val _afterSaleDetail = MutableStateFlow(UiState<AfterSaleDetail>())
    val afterSaleDetail: StateFlow<UiState<AfterSaleDetail>> = _afterSaleDetail

    private val _claimableCoupons = MutableStateFlow(UiState<List<CouponTemplate>>())
    val claimableCoupons: StateFlow<UiState<List<CouponTemplate>>> = _claimableCoupons

    private val _myCoupons = MutableStateFlow(UiState<List<UserCoupon>>())
    val myCoupons: StateFlow<UiState<List<UserCoupon>>> = _myCoupons

    private val _member = MutableStateFlow(UiState<MemberInfo>())
    val member: StateFlow<UiState<MemberInfo>> = _member

    private val _pointsLogs = MutableStateFlow(UiState<PageResult<PointsLog>>())
    val pointsLogs: StateFlow<UiState<PageResult<PointsLog>>> = _pointsLogs

    private val _productReviews = MutableStateFlow(UiState<PageResult<Review>>())
    val productReviews: StateFlow<UiState<PageResult<Review>>> = _productReviews

    private val _reviewSummary = MutableStateFlow<ReviewSummary?>(null)
    val reviewSummary: StateFlow<ReviewSummary?> = _reviewSummary

    private val _favorites = MutableStateFlow(UiState<PageResult<FavoriteProduct>>())
    val favorites: StateFlow<UiState<PageResult<FavoriteProduct>>> = _favorites

    private val _history = MutableStateFlow(UiState<PageResult<FavoriteProduct>>())
    val history: StateFlow<UiState<PageResult<FavoriteProduct>>> = _history

    private val _favoriteStatus = MutableStateFlow<Map<Long, Boolean>>(emptyMap())
    val favoriteStatus: StateFlow<Map<Long, Boolean>> = _favoriteStatus

    private val _supportTickets = MutableStateFlow(UiState<PageResult<SupportTicket>>())
    val supportTickets: StateFlow<UiState<PageResult<SupportTicket>>> = _supportTickets

    private val _notifications = MutableStateFlow(UiState<PageResult<UserNotification>>())
    val notifications: StateFlow<UiState<PageResult<UserNotification>>> = _notifications

    val categories: StateFlow<List<Category>>
        get() = _categories
    private val _categories = MutableStateFlow<List<Category>>(emptyList())

    private val _categoriesLoading = MutableStateFlow(false)
    val categoriesLoading: StateFlow<Boolean> = _categoriesLoading

    private val _categoriesError = MutableStateFlow<String?>(null)
    val categoriesError: StateFlow<String?> = _categoriesError

    val cartBadge: StateFlow<Int> = _cart
        .map { state -> state.data?.items?.sumOf { it.quantity } ?: 0 }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 0)

    init {
        loadHome()
        loadPaymentConfig()
        loadHotKeywords()
        loadCategories()
        if (_loggedIn.value) {
            loadMe()
            loadCart()
        }
    }

    fun loadHome(force: Boolean = false) {
        if (!force && _home.value.data != null) {
            return
        }
        _home.update { it.withLoading() }
        viewModelScope.launch {
            repository.home()
                .onSuccess { data -> _home.value = UiState(data = data) }
                .onFailure { error -> _home.value = _home.value.withError(error.message ?: "首页加载失败") }
        }
    }

    fun loadCategories() {
        if (_categoriesLoading.value) {
            return
        }
        _categoriesLoading.value = true
        _categoriesError.value = null
        viewModelScope.launch {
            repository.categories()
                .onSuccess { data -> _categories.value = data }
                .onFailure { error -> _categoriesError.value = error.message ?: "分类加载失败" }
            _categoriesLoading.value = false
        }
    }

    fun loadProducts(categoryId: Long? = null, keyword: String? = null, refresh: Boolean = false) {
        if (!refresh && _products.value.loading) {
            return
        }
        _products.value = UiState(loading = true)
        viewModelScope.launch {
            repository.products(categoryId, keyword)
                .onSuccess { data -> _products.value = UiState(data = data) }
                .onFailure { error -> _products.value = _products.value.withError(error.message ?: "商品加载失败") }
        }
    }

    fun loadProduct(id: Long, force: Boolean = false) {
        if (!force && _product.value.data?.id == id && !_product.value.loading) {
            return
        }
        _product.value = UiState(loading = true)
        viewModelScope.launch {
            repository.product(id)
                .onSuccess { data -> _product.value = UiState(data = data) }
                .onFailure { error -> _product.value = _product.value.withError(error.message ?: "商品加载失败") }
        }
    }

    fun devLogin(openId: String, nickname: String) {
        val normalizedOpenId = openId.trim()
        if (normalizedOpenId.isBlank()) {
            _loginError.value = "请输入开发用户标识"
            return
        }
        _loginBusy.value = true
        _loginError.value = null
        viewModelScope.launch {
            repository.devLogin(normalizedOpenId, nickname.trim().ifBlank { "Android 用户" })
                .onSuccess { session ->
                    prefs.openId = normalizedOpenId
                    prefs.nickname = nickname.trim().ifBlank { "Android 用户" }
                    _loggedIn.value = true
                    _user.value = session.user
                    emit("开发登录成功")
                    loadCart()
                }
                .onFailure { error ->
                    _loginError.value = error.message ?: "登录失败"
                }
            _loginBusy.value = false
        }
    }

    fun logout() {
        prefs.clearSession()
        _loggedIn.value = false
        _user.value = null
        _cart.value = UiState()
        _orders.value = UiState()
        _addresses.value = UiState()
        emit("已退出登录")
    }

    fun updateBaseUrl(input: String) {
        val normalized = normalizeBaseUrl(input)
        if (normalized == null) {
            emit("服务器地址格式不正确")
            return
        }
        prefs.baseUrl = normalized
        prefs.clearSession()
        repository.updateBaseUrl(normalized)
        _baseUrl.value = normalized
        _loggedIn.value = false
        _user.value = null
        _cart.value = UiState()
        _addresses.value = UiState()
        _home.value = UiState()
        emit("服务器地址已更新，请重新登录")
        loadHome(force = true)
    }

    fun loadMe() {
        if (prefs.token.isBlank()) {
            return
        }
        viewModelScope.launch {
            repository.me()
                .onSuccess { data -> _user.value = data; _loggedIn.value = true }
                .onFailure { error ->
                    if (error is ApiException && error.statusCode == 401) {
                        handleUnauthorized()
                    }
                }
        }
    }

    fun loadCart() {
        if (!_loggedIn.value) {
            _cart.value = UiState()
            return
        }
        _cart.value = _cart.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.cart()
                .onSuccess { data -> _cart.value = UiState(data = data) }
                .onFailure { error ->
                    if (error is ApiException && error.statusCode == 401) {
                        handleUnauthorized()
                    } else {
                        _cart.value = _cart.value.withError(error.message ?: "购物车加载失败")
                    }
                }
        }
    }

    fun addToCart(skuId: Long, quantity: Int) {
        if (!_loggedIn.value) {
            emit("请先登录")
            return
        }
        _busy.value = true
        viewModelScope.launch {
            repository.addCart(skuId, quantity)
                .onSuccess { data ->
                    _cart.value = UiState(data = data)
                    emit("已加入购物车")
                }
                .onFailure { error -> emit(error.message ?: "加入购物车失败") }
            _busy.value = false
        }
    }

    fun updateCartItem(id: Long, quantity: Int? = null, selected: Boolean? = null) {
        _busy.value = true
        viewModelScope.launch {
            repository.updateCart(id, quantity, selected)
                .onSuccess { data -> _cart.value = UiState(data = data) }
                .onFailure { error -> emit(error.message ?: "购物车更新失败") }
            _busy.value = false
        }
    }

    fun selectAllCart(selected: Boolean) {
        _busy.value = true
        viewModelScope.launch {
            repository.selectCart(selected)
                .onSuccess { data -> _cart.value = UiState(data = data) }
                .onFailure { error -> emit(error.message ?: "操作失败") }
            _busy.value = false
        }
    }

    fun deleteCartItem(id: Long) {
        _busy.value = true
        viewModelScope.launch {
            repository.deleteCartItem(id)
                .onSuccess { data -> _cart.value = UiState(data = data); emit("已删除") }
                .onFailure { error -> emit(error.message ?: "删除失败") }
            _busy.value = false
        }
    }

    fun clearCart() {
        _busy.value = true
        viewModelScope.launch {
            repository.clearCart()
                .onSuccess { data -> _cart.value = UiState(data = data); emit("购物车已清空") }
                .onFailure { error -> emit(error.message ?: "清空失败") }
            _busy.value = false
        }
    }

    fun loadAddresses() {
        if (!_loggedIn.value) {
            _addresses.value = UiState()
            return
        }
        _addresses.value = _addresses.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.addresses()
                .onSuccess { data -> _addresses.value = UiState(data = data) }
                .onFailure { error ->
                    if (error is ApiException && error.statusCode == 401) {
                        handleUnauthorized()
                    } else {
                        _addresses.value = _addresses.value.withError(error.message ?: "地址加载失败")
                    }
                }
        }
    }

    fun saveAddress(
        id: Long?,
        payload: AddressPayload,
        onSuccess: () -> Unit,
    ) {
        _busy.value = true
        viewModelScope.launch {
            val result = if (id == null || id == 0L) {
                repository.createAddress(payload)
            } else {
                repository.updateAddress(id, payload)
            }
            result.onSuccess {
                emit("地址已保存")
                loadAddresses()
                onSuccess()
            }.onFailure { error -> emit(error.message ?: "保存地址失败") }
            _busy.value = false
        }
    }

    fun deleteAddress(id: Long) {
        _busy.value = true
        viewModelScope.launch {
            repository.deleteAddress(id)
                .onSuccess { loadAddresses(); emit("地址已删除") }
                .onFailure { error -> emit(error.message ?: "删除失败") }
            _busy.value = false
        }
    }

    fun setDefaultAddress(id: Long) {
        _busy.value = true
        viewModelScope.launch {
            repository.setDefaultAddress(id)
                .onSuccess { loadAddresses(); emit("已设为默认地址") }
                .onFailure { error -> emit(error.message ?: "设置失败") }
            _busy.value = false
        }
    }

    fun previewCheckout(
        addressId: Long,
        source: String,
        cartItemIds: List<Long>,
        skuId: Long?,
        quantity: Int?,
        couponId: Long? = null,
    ) {
        if (addressId <= 0) {
            _checkout.value = UiState(error = "请选择收货地址")
            return
        }
        _checkout.value = UiState(loading = true)
        viewModelScope.launch {
            repository.checkoutPreview(
                CheckoutRequest(
                    addressId = addressId,
                    source = source,
                    cartItemIds = cartItemIds.takeIf { it.isNotEmpty() },
                    skuId = skuId,
                    quantity = quantity,
                    couponId = couponId,
                ),
            ).onSuccess { data -> _checkout.value = UiState(data = data) }
                .onFailure { error -> _checkout.value = _checkout.value.withError(error.message ?: "结算信息加载失败") }
        }
    }

    fun createOrder(
        addressId: Long,
        source: String,
        cartItemIds: List<Long>,
        skuId: Long?,
        quantity: Int?,
        clientRequestNo: String,
        couponId: Long? = null,
        invoiceRequired: Boolean = false,
        invoiceTitle: String = "",
        invoiceTaxNo: String = "",
        invoiceEmail: String = "",
        onSuccess: (OrderDetail) -> Unit,
    ) {
        if (_busy.value) {
            return
        }
        _busy.value = true
        viewModelScope.launch {
            repository.createOrder(
                CreateOrderRequest(
                    addressId = addressId,
                    source = source,
                    cartItemIds = cartItemIds.takeIf { it.isNotEmpty() },
                    skuId = skuId,
                    quantity = quantity,
                    clientRequestNo = clientRequestNo,
                    couponId = couponId,
                    invoiceRequired = invoiceRequired,
                    invoiceTitle = invoiceTitle,
                    invoiceTaxNo = invoiceTaxNo,
                    invoiceEmail = invoiceEmail,
                ),
            ).onSuccess { order ->
                emit("下单成功")
                loadCart()
                onSuccess(order)
            }.onFailure { error -> emit(error.message ?: "下单失败") }
            _busy.value = false
        }
    }

    fun loadOrders(status: String? = null) {
        if (!_loggedIn.value) {
            _orders.value = UiState()
            return
        }
        _orders.value = _orders.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.orders(status)
                .onSuccess { data -> _orders.value = UiState(data = data) }
                .onFailure { error ->
                    if (error is ApiException && error.statusCode == 401) {
                        handleUnauthorized()
                    } else {
                        _orders.value = _orders.value.withError(error.message ?: "订单加载失败")
                    }
                }
        }
    }

    fun loadOrderDetail(orderNo: String) {
        _orderDetail.value = UiState(loading = true)
        viewModelScope.launch {
            repository.order(orderNo)
                .onSuccess { data -> _orderDetail.value = UiState(data = data) }
                .onFailure { error -> _orderDetail.value = _orderDetail.value.withError(error.message ?: "订单加载失败") }
        }
    }

    fun payOrder(orderNo: String) {
        _busy.value = true
        viewModelScope.launch {
            repository.payOrder(orderNo)
                .onSuccess { result ->
                    _orderDetail.value = UiState(data = result.order)
                    emit(if (result.mock) "模拟支付成功" else "支付成功")
                    loadOrders()
                }
                .onFailure { error -> emit(error.message ?: "支付失败") }
            _busy.value = false
        }
    }

    fun cancelOrder(orderNo: String) {
        _busy.value = true
        viewModelScope.launch {
            repository.cancelOrder(orderNo, "用户取消")
                .onSuccess { data ->
                    _orderDetail.value = UiState(data = data)
                    emit("订单已取消")
                    loadOrders()
                }
                .onFailure { error -> emit(error.message ?: "取消失败") }
            _busy.value = false
        }
    }

    fun confirmReceipt(orderNo: String) {
        _busy.value = true
        viewModelScope.launch {
            repository.confirmReceipt(orderNo)
                .onSuccess { data ->
                    _orderDetail.value = UiState(data = data)
                    emit("已确认收货")
                    loadOrders()
                }
                .onFailure { error -> emit(error.message ?: "操作失败") }
            _busy.value = false
        }
    }

    fun loadPaymentConfig() {
        viewModelScope.launch {
            repository.paymentConfig()
                .onSuccess { data -> _paymentConfig.value = data }
                .onFailure { /* 保持默认关闭 */ }
        }
    }

    fun addressById(id: Long): Address? = _addresses.value.data?.firstOrNull { it.id == id }

    val savedOpenId: String get() = prefs.openId
    val savedNickname: String get() = prefs.nickname

    fun loadHotKeywords() {
        viewModelScope.launch {
            repository.hotKeywords().onSuccess { _hotKeywords.value = it }
        }
    }

    fun loadAfterSales() {
        if (!_loggedIn.value) return
        _afterSales.value = _afterSales.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.afterSales()
                .onSuccess { _afterSales.value = UiState(data = it) }
                .onFailure { _afterSales.value = _afterSales.value.withError(it.message ?: "售后加载失败") }
        }
    }

    fun loadAfterSaleDetail(afterSaleNo: String) {
        _afterSaleDetail.value = UiState(loading = true)
        viewModelScope.launch {
            repository.afterSale(afterSaleNo)
                .onSuccess { _afterSaleDetail.value = UiState(data = it) }
                .onFailure { _afterSaleDetail.value = _afterSaleDetail.value.withError(it.message ?: "售后详情加载失败") }
        }
    }

    fun createAfterSale(
        orderNo: String,
        type: String,
        reason: String,
        description: String,
        items: List<AfterSaleItemRequest>,
        onSuccess: (AfterSaleDetail) -> Unit,
    ) {
        _busy.value = true
        viewModelScope.launch {
            repository.createAfterSale(orderNo, CreateAfterSaleRequest(type, reason, description, items))
                .onSuccess { detail ->
                    emit("售后申请已提交")
                    onSuccess(detail)
                }
                .onFailure { emit(it.message ?: "提交失败") }
            _busy.value = false
        }
    }

    fun cancelAfterSale(afterSaleNo: String) {
        _busy.value = true
        viewModelScope.launch {
            repository.cancelAfterSale(afterSaleNo)
                .onSuccess { _afterSaleDetail.value = UiState(data = it); emit("已取消售后") }
                .onFailure { emit(it.message ?: "取消失败") }
            _busy.value = false
        }
    }

    fun submitReturnShipment(afterSaleNo: String, company: String, trackingNo: String) {
        _busy.value = true
        viewModelScope.launch {
            repository.submitReturnShipment(afterSaleNo, company, trackingNo)
                .onSuccess { _afterSaleDetail.value = UiState(data = it); emit("退货物流已提交") }
                .onFailure { emit(it.message ?: "提交失败") }
            _busy.value = false
        }
    }

    fun loadCoupons() {
        if (!_loggedIn.value) return
        _claimableCoupons.value = _claimableCoupons.value.copy(loading = true, error = null)
        _myCoupons.value = _myCoupons.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.claimableCoupons()
                .onSuccess { _claimableCoupons.value = UiState(data = it) }
                .onFailure { _claimableCoupons.value = _claimableCoupons.value.withError(it.message ?: "优惠券加载失败") }
            repository.myCoupons()
                .onSuccess { _myCoupons.value = UiState(data = it) }
                .onFailure { _myCoupons.value = _myCoupons.value.withError(it.message ?: "优惠券加载失败") }
        }
    }

    fun claimCoupon(templateId: Long) {
        _busy.value = true
        viewModelScope.launch {
            repository.claimCoupon(templateId)
                .onSuccess { emit("领取成功"); loadCoupons() }
                .onFailure { emit(it.message ?: "领取失败") }
            _busy.value = false
        }
    }

    fun loadMember() {
        if (!_loggedIn.value) return
        _member.value = _member.value.copy(loading = true, error = null)
        _pointsLogs.value = _pointsLogs.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.member()
                .onSuccess { _member.value = UiState(data = it) }
                .onFailure { _member.value = _member.value.withError(it.message ?: "会员信息加载失败") }
            repository.pointsLogs()
                .onSuccess { _pointsLogs.value = UiState(data = it) }
                .onFailure { _pointsLogs.value = _pointsLogs.value.withError(it.message ?: "积分记录加载失败") }
        }
    }

    fun loadProductReviews(productId: Long) {
        _productReviews.value = _productReviews.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.productReviews(productId)
                .onSuccess { _productReviews.value = UiState(data = it) }
                .onFailure { _productReviews.value = _productReviews.value.withError(it.message ?: "评价加载失败") }
            repository.reviewSummary(productId).onSuccess { _reviewSummary.value = it }
        }
    }

    fun submitReview(
        orderNo: String,
        orderItemId: Long,
        rating: Int,
        content: String,
        anonymous: Boolean,
        onSuccess: () -> Unit,
    ) {
        _busy.value = true
        viewModelScope.launch {
            repository.createReview(orderNo, CreateReviewRequest(orderItemId, rating, content, anonymous))
                .onSuccess { emit("评价已提交，等待审核"); onSuccess() }
                .onFailure { emit(it.message ?: "评价提交失败") }
            _busy.value = false
        }
    }

    fun loadFavorites() {
        if (!_loggedIn.value) return
        _favorites.value = _favorites.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.favorites()
                .onSuccess { _favorites.value = UiState(data = it) }
                .onFailure { _favorites.value = _favorites.value.withError(it.message ?: "收藏加载失败") }
        }
    }

    fun loadHistory() {
        if (!_loggedIn.value) return
        _history.value = _history.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.history()
                .onSuccess { _history.value = UiState(data = it) }
                .onFailure { _history.value = _history.value.withError(it.message ?: "足迹加载失败") }
        }
    }

    fun loadFavoriteStatus(productId: Long) {
        if (!_loggedIn.value) return
        viewModelScope.launch {
            repository.favoriteStatus(productId).onSuccess { value ->
                _favoriteStatus.update { it + (productId to value) }
            }
        }
    }

    fun toggleFavorite(productId: Long) {
        if (!_loggedIn.value) {
            emit("请先登录")
            return
        }
        val current = _favoriteStatus.value[productId] == true
        _busy.value = true
        viewModelScope.launch {
            val result = if (current) repository.removeFavorite(productId) else repository.addFavorite(productId)
            result.onSuccess {
                _favoriteStatus.update { it + (productId to !current) }
                emit(if (current) "已取消收藏" else "已收藏")
            }.onFailure { emit(it.message ?: "操作失败") }
            _busy.value = false
        }
    }

    fun recordProductHistory(productId: Long) {
        if (!_loggedIn.value) return
        viewModelScope.launch { repository.recordHistory(productId) }
    }

    fun clearHistory() {
        _busy.value = true
        viewModelScope.launch {
            repository.clearHistory()
                .onSuccess { _history.value = UiState(data = PageResult()); emit("已清空足迹") }
                .onFailure { emit(it.message ?: "清空失败") }
            _busy.value = false
        }
    }

    fun loadSupportTickets() {
        if (!_loggedIn.value) return
        _supportTickets.value = _supportTickets.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.supportTickets()
                .onSuccess { _supportTickets.value = UiState(data = it) }
                .onFailure { _supportTickets.value = _supportTickets.value.withError(it.message ?: "工单加载失败") }
        }
    }

    fun createTicket(orderNo: String, type: String, subject: String, content: String) {
        _busy.value = true
        viewModelScope.launch {
            repository.createTicket(CreateTicketRequest(orderNo.takeIf { it.isNotBlank() }, type, subject, content))
                .onSuccess { emit("工单已提交"); loadSupportTickets() }
                .onFailure { emit(it.message ?: "提交失败") }
            _busy.value = false
        }
    }

    fun closeTicket(ticketNo: String) {
        _busy.value = true
        viewModelScope.launch {
            repository.closeTicket(ticketNo)
                .onSuccess { emit("工单已关闭"); loadSupportTickets() }
                .onFailure { emit(it.message ?: "操作失败") }
            _busy.value = false
        }
    }

    fun loadNotifications() {
        if (!_loggedIn.value) return
        _notifications.value = _notifications.value.copy(loading = true, error = null)
        viewModelScope.launch {
            repository.notifications()
                .onSuccess { _notifications.value = UiState(data = it) }
                .onFailure { _notifications.value = _notifications.value.withError(it.message ?: "通知加载失败") }
        }
    }

    fun markNotificationRead(id: Long) {
        viewModelScope.launch {
            repository.markNotificationRead(id).onSuccess { loadNotifications() }
        }
    }

    fun changeOrderAddress(orderNo: String, addressId: Long) {
        _busy.value = true
        viewModelScope.launch {
            repository.changeOrderAddress(orderNo, addressId)
                .onSuccess { _orderDetail.value = UiState(data = it); emit("地址已更新") }
                .onFailure { emit(it.message ?: "修改地址失败") }
            _busy.value = false
        }
    }

    private fun handleUnauthorized() {
        prefs.clearSession()
        _loggedIn.value = false
        _user.value = null
        _cart.value = UiState()
        _orders.value = UiState()
        _addresses.value = UiState()
        emit("登录已过期，请重新登录")
    }

    private fun emit(message: String) {
        _messages.tryEmit(message)
    }

    private fun normalizeBaseUrl(input: String): String? {
        var value = input.trim()
        if (value.isBlank()) {
            return null
        }
        if (!value.startsWith("http://") && !value.startsWith("https://")) {
            value = "http://$value"
        }
        value = value.trimEnd('/')
        if (!value.contains("/api/v1")) {
            value = "$value/api/v1"
        }
        return value
    }
}
