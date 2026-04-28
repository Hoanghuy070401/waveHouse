package com.wavehouse.presentation.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.*
import com.wavehouse.domain.repository.AuthRepository
import com.wavehouse.domain.repository.ProductRepository
import com.wavehouse.domain.repository.WarehouseRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.pos.CancelOrderUseCase
import com.wavehouse.domain.usecase.pos.CheckoutUseCase
import com.wavehouse.domain.usecase.pos.ConfirmPaymentUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PosUiState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val cartItems: List<CartItem> = emptyList(),
    val isLoading: Boolean = true,
    val isCheckingOut: Boolean = false,
    val checkoutSuccess: Boolean = false,
    val checkoutOrderId: String? = null,
    val selectedPaymentMethod: PaymentMethod = PaymentMethod.CASH,
    val error: String? = null,
    val qrEnabled: Boolean = false,
    val qrImageUrl: String? = null,
    val showCashConfirmDialog: Boolean = false,
    val showQrSheet: Boolean = false,
    val pendingOrderId: String? = null,
    val pendingOrderAmount: Double = 0.0,
    val pendingOrderItemCount: Int = 0,
    val isConfirmingPayment: Boolean = false,
    val paymentFlowError: String? = null,
    val lastCheckoutMethod: PaymentMethod? = null,
    // ── Debt form state ────────────────────────────────────────────────────────
    val enableDebt: Boolean = false,
    val debtCustomerName: String = "",
    val debtCustomerPhone: String = "",
    val debtNote: String = "",
    val debtDueDateMs: Long? = null,
) {
    val cartTotal: Double get() = cartItems.sumOf { it.lineTotal }
    val cartCount: Int get() = cartItems.sumOf { it.quantity }.toInt()
    val isCartEmpty: Boolean get() = cartItems.isEmpty()
}

@HiltViewModel
class PosViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val productRepository: ProductRepository,
    private val checkoutUseCase: CheckoutUseCase,
    private val confirmPaymentUseCase: ConfirmPaymentUseCase,
    private val cancelOrderUseCase: CancelOrderUseCase,
    private val warehouseRepository: WarehouseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _uiState = MutableStateFlow(PosUiState())
    val uiState = _uiState.asStateFlow()

    /** One-shot events cho "stock limit reached" toasts. */
    private val _limitEvents = Channel<String>(Channel.BUFFERED)
    val limitEvents: Flow<String> = _limitEvents.receiveAsFlow()

    private var lastLimitEventAt = 0L
    private val limitEventCooldownMs = 1500L

    private var currentUser: User? = null

    private val stockLimitMsg =
        "Số lượng hàng đã đạt giới hạn kho, vui lòng nhập thêm để tiếp tục đơn hàng"

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            if (user == null) {
                _uiState.update {
                    it.copy(isLoading = false, error = "Chưa đăng nhập hoặc không xác định được người dùng")
                }
                return@launch
            }
            currentUser = user
            loadQrConfig(user)
            productRepository.getProducts(user.warehouseId).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        _allProducts.value = result.data.filter { it.currentStock > 0.0 }
                        filterProducts(_uiState.value.searchQuery)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(error = result.message, isLoading = false)
                    }
                    ApiResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun loadQrConfig(user: User) {
        viewModelScope.launch {
            val verified = runCatching { authRepository.reloadAndCheckVerified() }.getOrDefault(false)
            warehouseRepository.observeWarehouse(user.warehouseId).collect { result ->
                if (result is ApiResult.Success) {
                    val qrUrl = result.data.qrImageUrl
                    _uiState.update {
                        it.copy(
                            qrImageUrl = qrUrl,
                            qrEnabled = verified && !qrUrl.isNullOrBlank()
                        )
                    }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterProducts(query)
    }

    private fun filterProducts(query: String) {
        val q = query.lowercase().trim()
        if (q.isEmpty()) {
            _uiState.update { it.copy(products = _allProducts.value) }
            return
        }
        val filtered = _allProducts.value.filter {
            it.name.lowercase().contains(q) ||
            it.sku.lowercase().contains(q) ||
            it.barcode?.lowercase()?.contains(q) == true
        }
        _uiState.update { it.copy(products = filtered) }
    }

    /** 1-tap thêm sản phẩm vào giỏ */
    fun addToCart(product: Product) {
        var hitLimit = false
        _uiState.update { state ->
            val existing = state.cartItems.find { it.product.id == product.id }
            val maxQty = product.currentStock

            val newCart = if (existing != null) {
                if (existing.quantity >= maxQty) {
                    hitLimit = true
                    return@update state
                }
                state.cartItems.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1.0) else it
                }
            } else {
                state.cartItems + CartItem(product, 1.0)
            }
            state.copy(cartItems = newCart, error = null)
        }
        if (hitLimit) emitStockLimitEvent()
    }

    fun increaseQuantity(productId: String) {
        var hitLimit = false
        _uiState.update { state ->
            val item = state.cartItems.find { it.product.id == productId } ?: return@update state
            if (item.quantity >= item.product.currentStock) {
                hitLimit = true
                return@update state
            }
            val step = if (item.product.allowDecimal) 0.5 else 1.0
            val next = (item.quantity + step).coerceAtMost(item.product.currentStock)
            state.copy(cartItems = state.cartItems.map {
                if (it.product.id == productId) it.copy(quantity = next) else it
            })
        }
        if (hitLimit) emitStockLimitEvent()
    }

    fun decreaseQuantity(productId: String) {
        _uiState.update { state ->
            val item = state.cartItems.find { it.product.id == productId } ?: return@update state
            val step = if (item.product.allowDecimal) 0.5 else 1.0
            val next = item.quantity - step
            if (next <= 0.0) {
                state.copy(cartItems = state.cartItems.filter { it.product.id != productId })
            } else {
                state.copy(cartItems = state.cartItems.map {
                    if (it.product.id == productId) it.copy(quantity = next) else it
                })
            }
        }
    }

    fun removeFromCart(productId: String) {
        _uiState.update { state ->
            state.copy(cartItems = state.cartItems.filter { it.product.id != productId })
        }
    }

    fun setQuantity(productId: String, qty: Int) {
        var hitLimit = false
        _uiState.update { state ->
            val item = state.cartItems.find { it.product.id == productId } ?: return@update state
            when {
                qty <= 0 -> state.copy(cartItems = state.cartItems.filter { it.product.id != productId })
                qty > item.product.currentStock.toInt() -> {
                    hitLimit = true
                    state.copy(cartItems = state.cartItems.map {
                        if (it.product.id == productId) it.copy(quantity = item.product.currentStock) else it
                    })
                }
                else -> state.copy(cartItems = state.cartItems.map {
                    if (it.product.id == productId) it.copy(quantity = qty.toDouble()) else it
                })
            }
        }
        if (hitLimit) emitStockLimitEvent()
    }

    fun setQuantityDecimal(productId: String, qty: Double) {
        var hitLimit = false
        _uiState.update { state ->
            val item = state.cartItems.find { it.product.id == productId } ?: return@update state
            when {
                qty <= 0.0 -> state.copy(cartItems = state.cartItems.filter { it.product.id != productId })
                qty > item.product.currentStock -> {
                    hitLimit = true
                    state.copy(cartItems = state.cartItems.map {
                        if (it.product.id == productId) it.copy(quantity = item.product.currentStock) else it
                    })
                }
                else -> state.copy(cartItems = state.cartItems.map {
                    if (it.product.id == productId) it.copy(quantity = qty) else it
                })
            }
        }
        if (hitLimit) emitStockLimitEvent()
    }

    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList()) }
    }

    private fun emitStockLimitEvent() {
        val now = System.currentTimeMillis()
        if (now - lastLimitEventAt >= limitEventCooldownMs) {
            lastLimitEventAt = now
            _limitEvents.trySend(stockLimitMsg)
        }
    }

    fun notifyStockLimit() {
        emitStockLimitEvent()
    }

    fun setPaymentMethod(method: PaymentMethod) {
        if (method == PaymentMethod.QR && !_uiState.value.qrEnabled) return
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun requestCheckout() {
        val state = _uiState.value
        if (state.isCartEmpty || state.isCheckingOut) return
        
        if (state.selectedPaymentMethod == PaymentMethod.QR) {
            if (!state.qrEnabled) {
                _limitEvents.trySend(
                    if (state.qrImageUrl.isNullOrBlank())
                        "Chưa cấu hình mã QR. Vui lòng cấu hình trong Tài khoản → Cấu hình thanh toán QR"
                    else "Bạn cần xác minh email trước khi sử dụng thanh toán QR"
                )
                return
            }
        }
        
        _uiState.update { it.copy(showCashConfirmDialog = true) }
    }

    fun dismissCashConfirmDialog() {
        _uiState.update { it.copy(showCashConfirmDialog = false) }
    }

    // ── Debt form helpers ──────────────────────────────────────────────────────
    fun setEnableDebt(enabled: Boolean) = _uiState.update { it.copy(enableDebt = enabled) }
    fun setDebtCustomerName(v: String) = _uiState.update { it.copy(debtCustomerName = v) }
    fun setDebtCustomerPhone(v: String) = _uiState.update { it.copy(debtCustomerPhone = v) }
    fun setDebtNote(v: String) = _uiState.update { it.copy(debtNote = v) }
    fun setDebtDueDateMs(ms: Long?) = _uiState.update { it.copy(debtDueDateMs = ms) }

    private fun resetDebtForm() = _uiState.update {
        it.copy(
            enableDebt = false,
            debtCustomerName = "",
            debtCustomerPhone = "",
            debtNote = "",
            debtDueDateMs = null
        )
    }

    fun confirmCashCheckout(paidAmount: Double) {
        val user = currentUser ?: return
        val state = _uiState.value
        if (state.isCartEmpty || state.isCheckingOut) return

        viewModelScope.launch {
            val total = state.cartTotal
            val itemCount = state.cartItems.size
            _uiState.update {
                it.copy(
                    isCheckingOut = true,
                    showCashConfirmDialog = false,
                    error = null,
                    paymentFlowError = null
                )
            }

            // Resolve debt info
            val debt = total - paidAmount
            val custName = if (state.enableDebt && debt > 0) state.debtCustomerName.takeIf { it.isNotBlank() } else null
            val custPhone = if (state.enableDebt && debt > 0) state.debtCustomerPhone.takeIf { it.isNotBlank() } else null
            val debtNote = if (state.enableDebt && debt > 0) state.debtNote.takeIf { it.isNotBlank() } else null
            val debtDue = if (state.enableDebt && debt > 0) state.debtDueDateMs else null

            when (
                val result = checkoutUseCase(
                    cartItems = state.cartItems,
                    warehouseId = user.warehouseId,
                    paymentMethod = PaymentMethod.CASH,
                    createdBy = user.id,
                    createdByName = user.name,
                    paidAmount = paidAmount,
                    customerName = custName,
                    customerPhone = custPhone,
                    debtNote = debtNote,
                    debtDueDate = debtDue
                )
            ) {
                is ApiResult.Success -> {
                    resetDebtForm()
                    _uiState.update {
                        it.copy(
                            isCheckingOut = false,
                            checkoutSuccess = true,
                            checkoutOrderId = result.data,
                            cartItems = emptyList(),
                            pendingOrderAmount = total,
                            pendingOrderItemCount = itemCount,
                            error = null,
                            lastCheckoutMethod = PaymentMethod.CASH
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isCheckingOut = false,
                        error = result.message,
                        paymentFlowError = result.message
                    )
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun startQrCheckout(paidAmount: Double) {
        val user = currentUser ?: return
        val state = _uiState.value
        if (state.isCartEmpty || state.isCheckingOut) return

        viewModelScope.launch {
            val total = state.cartTotal
            val itemCount = state.cartItems.size
            _uiState.update {
                it.copy(
                    isCheckingOut = true,
                    error = null,
                    paymentFlowError = null,
                    showQrSheet = false,
                    pendingOrderId = null
                )
            }

            // Resolve debt info for QR
            val debt = total - paidAmount
            val custName = if (state.enableDebt && debt > 0) state.debtCustomerName.takeIf { it.isNotBlank() } else null
            val custPhone = if (state.enableDebt && debt > 0) state.debtCustomerPhone.takeIf { it.isNotBlank() } else null
            val debtNote = if (state.enableDebt && debt > 0) state.debtNote.takeIf { it.isNotBlank() } else null
            val debtDue = if (state.enableDebt && debt > 0) state.debtDueDateMs else null

            when (
                val result = checkoutUseCase(
                    cartItems = state.cartItems,
                    warehouseId = user.warehouseId,
                    paymentMethod = PaymentMethod.QR,
                    createdBy = user.id,
                    createdByName = user.name,
                    paidAmount = paidAmount,
                    customerName = custName,
                    customerPhone = custPhone,
                    debtNote = debtNote,
                    debtDueDate = debtDue
                )
            ) {
                is ApiResult.Success -> {
                    resetDebtForm()
                    _uiState.update {
                        it.copy(
                            isCheckingOut = false,
                            cartItems = emptyList(),
                            pendingOrderId = result.data,
                            pendingOrderAmount = total,
                            pendingOrderItemCount = itemCount,
                            showQrSheet = true,
                            checkoutOrderId = result.data,
                            checkoutSuccess = false,
                            lastCheckoutMethod = null
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isCheckingOut = false,
                        error = result.message,
                        paymentFlowError = result.message,
                        showQrSheet = false,
                        pendingOrderId = null
                    )
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun confirmQrPayment() {
        val orderId = _uiState.value.pendingOrderId ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isConfirmingPayment = true, paymentFlowError = null) }

            when (val result = confirmPaymentUseCase(orderId)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isConfirmingPayment = false,
                        checkoutSuccess = true,
                        showQrSheet = false,
                        pendingOrderId = null,
                        paymentFlowError = null,
                        lastCheckoutMethod = PaymentMethod.QR
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isConfirmingPayment = false,
                        paymentFlowError = result.message
                    )
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun cancelQrCheckout() {
        val orderId = _uiState.value.pendingOrderId
        if (orderId == null) {
            _uiState.update { it.copy(showQrSheet = false) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isConfirmingPayment = true, paymentFlowError = null) }

            when (val result = cancelOrderUseCase(orderId)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isConfirmingPayment = false,
                        showQrSheet = false,
                        pendingOrderId = null,
                        paymentFlowError = null,
                        pendingOrderAmount = 0.0,
                        pendingOrderItemCount = 0,
                        checkoutOrderId = null,
                        lastCheckoutMethod = null
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isConfirmingPayment = false,
                        paymentFlowError = result.message
                    )
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun resetCheckoutState() {
        _uiState.update {
            it.copy(
                checkoutSuccess = false,
                checkoutOrderId = null,
                error = null,
                showCashConfirmDialog = false,
                showQrSheet = false,
                pendingOrderId = null,
                isConfirmingPayment = false,
                paymentFlowError = null,
                pendingOrderAmount = 0.0,
                pendingOrderItemCount = 0,
                lastCheckoutMethod = null
            )
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null, paymentFlowError = null) }
    }
}
