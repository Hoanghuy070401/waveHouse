package com.wavehouse.presentation.stock.stockin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.product.GetProductsUseCase
import com.wavehouse.domain.usecase.product.GetProductByBarcodeUseCase
import com.wavehouse.domain.usecase.stock.CreateStockInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockInUiState(
    // Form state (used by bottom sheet)
    val productId: String = "",
    val productName: String = "",
    val currentStock: Double = -1.0,
    val unit: String = "cái",
    val quantity: String = "",
    val supplierName: String = "",
    val note: String = "",
    val warehouseId: String = "",
    val productError: String? = null,
    val quantityError: String? = null,
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null,
    // Product list state
    val products: List<Product> = emptyList(),
    val isLoadingProducts: Boolean = false,
    val searchQuery: String = ""
)

@OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
@HiltViewModel
class StockInViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val createStockInUseCase: CreateStockInUseCase,
    private val getProductByBarcodeUseCase: GetProductByBarcodeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockInUiState())
    val uiState = _uiState.asStateFlow()

    private val _warehouseId = MutableStateFlow("")

    /** Filtered product list — reacts to search query changes */
    val filteredProducts: StateFlow<List<Product>> = combine(
        _uiState.map { it.products },
        _uiState.map { it.searchQuery }.debounce(300)
    ) { products, query ->
        if (query.isBlank()) products
        else products.filter {
            it.name.contains(query, ignoreCase = true) ||
            it.sku.contains(query, ignoreCase = true)
        }
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    init {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            val wId = user?.warehouseId ?: ""
            _uiState.update { it.copy(warehouseId = wId) }
            _warehouseId.value = wId
            if (wId.isNotBlank()) loadProducts(wId)
        }
    }

    private fun loadProducts(warehouseId: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingProducts = true) }
            getProductsUseCase(warehouseId).collect { result ->
                when (result) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(products = result.data, isLoadingProducts = false)
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(isLoadingProducts = false, errorMessage = result.message)
                    }
                    ApiResult.Loading -> _uiState.update { it.copy(isLoadingProducts = true) }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) = _uiState.update { it.copy(searchQuery = query) }

    fun onProductNameChange(name: String) =
        _uiState.update { it.copy(productName = name, productError = null) }

    fun onQuantityChange(v: String) {
        // Allow digits and a single comma/dot for decimal
        val filtered = v.replace(',', '.').filter { it.isDigit() || it == '.' }
            .let { s ->
                // Only allow one decimal point
                val dotIdx = s.indexOf('.')
                if (dotIdx == -1) s
                else s.substring(0, dotIdx + 1) + s.substring(dotIdx + 1).filter { it.isDigit() }
            }
        _uiState.update {
            it.copy(
                quantity = filtered,
                quantityError = if (filtered.toDoubleOrNull()?.let { q -> q <= 0.0 } == true)
                    "Số lượng nhập phải lớn hơn 0" else null
            )
        }
    }

    fun onSupplierNameChange(v: String) = _uiState.update { it.copy(supplierName = v) }
    fun onNoteChange(v: String) = _uiState.update { it.copy(note = v) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun resetForm() = _uiState.update {
        it.copy(
            productId = "", productName = "", currentStock = -1.0,
            quantity = "", supplierName = "", note = "",
            success = false
        )
    }

    fun selectProduct(product: Product) {
        _uiState.update {
            it.copy(
                productId = product.id,
                productName = product.name,
                currentStock = product.currentStock,
                unit = product.unitName ?: "cái",
                productError = null
            )
        }
    }

    fun submit() {
        val state = _uiState.value
        val productError = if (state.productId.isBlank() && state.productName.isBlank())
            "Vui lòng chọn sản phẩm" else null
        val qty = state.quantity.toDoubleOrNull()
        val quantityError = when {
            state.quantity.isBlank() -> "Vui lòng nhập số lượng"
            qty == null || qty <= 0.0 -> "Số lượng nhập phải lớn hơn 0"
            else -> null
        }

        if (productError != null || quantityError != null) {
            _uiState.update { it.copy(productError = productError, quantityError = quantityError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = createStockInUseCase(
                productId = state.productId,
                warehouseId = state.warehouseId,
                quantity = qty!!,
                note = state.note.ifBlank { null }
            )) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, success = true) }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    /** Direct submit — avoids state-read race: UI passes params explicitly from local compose state */
    fun submitDirect(product: Product, quantity: Double, unitCostPrice: Double? = null, note: String?) {
        if (quantity <= 0.0) {
            _uiState.update { it.copy(errorMessage = "Số lượng nhập phải lớn hơn 0") }
            return
        }
        val warehouseId = _uiState.value.warehouseId
        if (warehouseId.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Không xác định được kho") }
            return
        }
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = createStockInUseCase(
                productId = product.id,
                warehouseId = warehouseId,
                quantity = quantity,
                unitCostPrice = unitCostPrice,   // → MAC algorithm
                note = note
            )) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, success = true) }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }
}
