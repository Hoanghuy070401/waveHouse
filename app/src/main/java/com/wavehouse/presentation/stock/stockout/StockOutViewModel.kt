package com.wavehouse.presentation.stock.stockout

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.stock.CreateStockOutUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockOutUiState(
    val productId: String = "",
    val productName: String = "",
    val currentStock: Double = -1.0,
    val unit: String = "cái",
    val quantity: String = "",
    val note: String = "",
    val warehouseId: String = "",
    val productError: String? = null,
    val quantityError: String? = null,
    val isLoading: Boolean = false,
    val success: Boolean = false,
    val errorMessage: String? = null
)

@HiltViewModel
class StockOutViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val createStockOutUseCase: CreateStockOutUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockOutUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            _uiState.update { it.copy(warehouseId = user?.warehouseId ?: "") }
        }
    }

    fun onProductNameChange(name: String) = _uiState.update {
        it.copy(productName = name, productError = null)
    }

    fun onQuantityChange(v: String) {
        val filtered = v.replace(',', '.').filter { it.isDigit() || it == '.' }
            .let { s ->
                val dot = s.indexOf('.')
                if (dot == -1) s
                else s.substring(0, dot + 1) + s.substring(dot + 1).filter { it.isDigit() }
            }
        val qty = filtered.toDoubleOrNull() ?: 0.0
        val currentStock = _uiState.value.currentStock

        val error = when {
            filtered.isBlank() -> null
            qty <= 0.0 -> "Số lượng xuất phải lớn hơn 0"
            currentStock >= 0.0 && qty > currentStock ->
                "Số tồn trong kho không đủ, hiện tại tồn $currentStock"
            else -> null
        }
        _uiState.update { it.copy(quantity = filtered, quantityError = error) }
    }

    fun onNoteChange(v: String) = _uiState.update { it.copy(note = v) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun resetForm() = _uiState.update {
        it.copy(productId = "", productName = "", currentStock = -1.0, quantity = "", note = "", success = false)
    }

    fun submit() {
        val state = _uiState.value
        val productError = if (state.productId.isBlank()) "Vui lòng chọn sản phẩm" else null
        val qty = state.quantity.toDoubleOrNull()
        val quantityError = when {
            state.quantity.isBlank() -> "Vui lòng nhập số lượng"
            qty == null || qty <= 0.0 -> "Số lượng xuất phải lớn hơn 0"
            else -> null
        }

        if (productError != null || quantityError != null) {
            _uiState.update { it.copy(productError = productError, quantityError = quantityError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = createStockOutUseCase(
                productId = state.productId,
                warehouseId = state.warehouseId,
                quantity = qty!!,
                currentStock = state.currentStock,
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
