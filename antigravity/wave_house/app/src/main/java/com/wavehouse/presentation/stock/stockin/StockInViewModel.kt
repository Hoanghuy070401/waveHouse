package com.wavehouse.presentation.stock.stockin

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.product.GetProductByBarcodeUseCase
import com.wavehouse.domain.usecase.product.SearchProductsUseCase
import com.wavehouse.domain.usecase.stock.CreateStockInUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockInUiState(
    val productId: String = "",
    val productName: String = "",
    val currentStock: Int = -1,
    val unit: String = "cái",
    val quantity: String = "",
    val supplierName: String = "",
    val note: String = "",
    val warehouseId: String = "",
    val productError: String? = null,
    val quantityError: String? = null,
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StockInViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    private val createStockInUseCase: CreateStockInUseCase,
    private val getProductByBarcodeUseCase: GetProductByBarcodeUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockInUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            _uiState.update { it.copy(warehouseId = user?.warehouseId ?: "") }

            // Pre-fill from scan navigation arg
            savedStateHandle.get<String>("productId")?.let { setProductById(it) }
        }
    }

    private fun setProductById(productId: String) {
        viewModelScope.launch {
            when (val r = getProductByBarcodeUseCase("")) { // placeholder — integrate with product lookup
                else -> {}
            }
        }
    }

    fun onProductNameChange(name: String) {
        _uiState.update { it.copy(productName = name, productError = null) }
    }

    fun onQuantityChange(v: String) {
        val filtered = v.filter { it.isDigit() }
        _uiState.update {
            it.copy(
                quantity = filtered,
                quantityError = if (filtered.toIntOrNull()?.let { q -> q <= 0 } == true)
                    "Số lượng nhập phải lớn hơn 0" else null
            )
        }
    }

    fun onSupplierNameChange(v: String) = _uiState.update { it.copy(supplierName = v) }
    fun onNoteChange(v: String) = _uiState.update { it.copy(note = v) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun resetForm() = _uiState.update {
        it.copy(
            productId = "", productName = "", currentStock = -1,
            quantity = "", supplierName = "", note = "",
            success = false
        )
    }

    fun submit() {
        val state = _uiState.value
        val productError = if (state.productId.isBlank() && state.productName.isBlank())
            "Vui lòng chọn sản phẩm" else null
        val qty = state.quantity.toIntOrNull()
        val quantityError = when {
            state.quantity.isBlank() -> "Vui lòng nhập số lượng"
            qty == null || qty <= 0 -> "Số lượng nhập phải lớn hơn 0"
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
}
