package com.wavehouse.presentation.stock.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.StockEntry
import com.wavehouse.domain.model.StockEntryType
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.stock.GetStockHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class StockHistoryUiState(
    val entries: List<StockEntry> = emptyList(),
    val filterIndex: Int = 0, // 0=Tất cả, 1=Nhập kho, 2=Xuất kho, 3=Hao hụt
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StockHistoryViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getStockHistoryUseCase: GetStockHistoryUseCase
) : ViewModel() {

    private val _allEntries = MutableStateFlow<List<StockEntry>>(emptyList())
    private val _uiState = MutableStateFlow(StockHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            getStockHistoryUseCase(user.warehouseId).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        // Loại bỏ tất cả entries do POS tạo ra (source=SALE).
                        // Lịch sử bán hàng xem ở màn hình Liịch sử Đơn Hàng riêng biệt.
                        _allEntries.value = result.data.filter { it.source != "SALE" }
                        applyFilter(_uiState.value.filterIndex)
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

    fun setFilter(index: Int) {
        _uiState.update { it.copy(filterIndex = index) }
        applyFilter(index)
    }

    private fun applyFilter(index: Int) {
        val filtered = when (index) {
            1 -> _allEntries.value.filter { it.type == StockEntryType.IN }
            2 -> _allEntries.value.filter {
                // Chỉ xuất kho thủ công (MANUAL), không được lọc SALE ở đây nữa
                // vì đã bị loại khỏi _allEntries rồi.
                it.type == StockEntryType.OUT
            }
            3 -> _allEntries.value.filter { it.type == StockEntryType.SHRINKAGE }
            else -> _allEntries.value
        }
        _uiState.update { it.copy(entries = filtered) }
    }
}
