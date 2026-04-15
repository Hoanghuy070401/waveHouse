package com.wavehouse.presentation.pos

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.*
import com.wavehouse.domain.repository.ProductRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.pos.CheckoutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
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
    val error: String? = null
) {
    val cartTotal: Double get() = cartItems.sumOf { it.lineTotal }
    val cartCount: Int get() = cartItems.sumOf { it.quantity }.toInt()
    val isCartEmpty: Boolean get() = cartItems.isEmpty()
}

@HiltViewModel
class PosViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val productRepository: ProductRepository,
    private val checkoutUseCase: CheckoutUseCase
) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _uiState = MutableStateFlow(PosUiState())
    val uiState = _uiState.asStateFlow()

    private var currentUser: User? = null

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            currentUser = getCurrentUserUseCase() ?: return@launch
            productRepository.getProducts(currentUser!!.warehouseId).collectLatest { result ->
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
        _uiState.update { state ->
            val existing = state.cartItems.find { it.product.id == product.id }
            val maxQty = product.currentStock

            val newCart = if (existing != null) {
                if (existing.quantity >= maxQty) return@update state
                state.cartItems.map {
                    if (it.product.id == product.id) it.copy(quantity = it.quantity + 1.0) else it
                }
            } else {
                state.cartItems + CartItem(product, 1.0)
            }
            state.copy(cartItems = newCart, error = null)
        }
    }

    fun increaseQuantity(productId: String) {
        _uiState.update { state ->
            val item = state.cartItems.find { it.product.id == productId } ?: return@update state
            if (item.quantity >= item.product.currentStock) return@update state
            state.copy(cartItems = state.cartItems.map {
                if (it.product.id == productId) it.copy(quantity = it.quantity + 1.0) else it
            })
        }
    }

    fun decreaseQuantity(productId: String) {
        _uiState.update { state ->
            val item = state.cartItems.find { it.product.id == productId } ?: return@update state
            if (item.quantity <= 1.0) {
                // Remove from cart
                state.copy(cartItems = state.cartItems.filter { it.product.id != productId })
            } else {
                state.copy(cartItems = state.cartItems.map {
                    if (it.product.id == productId) it.copy(quantity = it.quantity - 1.0) else it
                })
            }
        }
    }

    fun removeFromCart(productId: String) {
        _uiState.update { state ->
            state.copy(cartItems = state.cartItems.filter { it.product.id != productId })
        }
    }

    fun clearCart() {
        _uiState.update { it.copy(cartItems = emptyList()) }
    }

    fun setPaymentMethod(method: PaymentMethod) {
        _uiState.update { it.copy(selectedPaymentMethod = method) }
    }

    fun checkout() {
        val user = currentUser ?: return
        val state = _uiState.value
        if (state.isCartEmpty || state.isCheckingOut) return

        viewModelScope.launch {
            _uiState.update { it.copy(isCheckingOut = true, error = null) }

            val result = checkoutUseCase(
                cartItems = state.cartItems,
                warehouseId = user.warehouseId,
                paymentMethod = state.selectedPaymentMethod,
                createdBy = user.id,
                createdByName = user.name
            )

            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isCheckingOut = false,
                            checkoutSuccess = true,
                            checkoutOrderId = result.data,
                            cartItems = emptyList()
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isCheckingOut = false, error = result.message)
                }
                ApiResult.Loading -> { /* Handled above */ }
            }
        }
    }

    fun resetCheckoutState() {
        _uiState.update {
            it.copy(checkoutSuccess = false, checkoutOrderId = null, error = null)
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
