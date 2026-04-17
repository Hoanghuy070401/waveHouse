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
    val filterIndex: Int = 0, // 0=All, 1=In, 2=Out
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
                        _allEntries.value = result.data
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
            2 -> _allEntries.value.filter { it.type == StockEntryType.OUT }
            else -> _allEntries.value
        }
        _uiState.update { it.copy(entries = filtered) }
    }
}
