package com.wavehouse.presentation.stock.lowstock

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.StockItem
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.stock.GetLowStockAlertsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LowStockAlertUiState(
    val items: List<StockItem> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class LowStockAlertViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getLowStockAlertsUseCase: GetLowStockAlertsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(LowStockAlertUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            getLowStockAlertsUseCase(user.warehouseId).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(
                            // Sort: out-of-stock first, then low stock
                            items = result.data.sortedWith(
                                compareBy({ it.quantity }, { it.productName })
                            ),
                            isLoading = false
                        )
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(error = result.message, isLoading = false)
                    }
                    ApiResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }
}
