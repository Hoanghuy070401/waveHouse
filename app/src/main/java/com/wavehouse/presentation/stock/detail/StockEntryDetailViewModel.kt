package com.wavehouse.presentation.stock.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.StockEntry
import com.wavehouse.domain.repository.StockRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.wavehouse.domain.repository.ProductRepository

data class StockEntryDetailUiState(
    val isLoading: Boolean = true,
    val entry: StockEntry? = null,
    val errorMessage: String? = null
)

@HiltViewModel
class StockEntryDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val stockRepository: StockRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(StockEntryDetailUiState())
    val uiState: StateFlow<StockEntryDetailUiState> = _uiState.asStateFlow()

    /** One-shot event: navigate to OrderDetail with this orderId */
    private val _navigateToOrder = Channel<String>(Channel.BUFFERED)
    val navigateToOrder: Flow<String> = _navigateToOrder.receiveAsFlow()

    private val entryId: String = checkNotNull(savedStateHandle["entryId"])

    init {
        load()
    }

    private fun load() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = stockRepository.getStockEntryById(entryId)) {
                is ApiResult.Success -> {
                    var finalEntry = result.data
                    // Hỗ trợ backfill image đối với các record lịch sử không có link ảnh
                    if (finalEntry.productImageUrl.isNullOrBlank()) {
                        when (val prodResult = productRepository.getProductById(finalEntry.productId)) {
                            is ApiResult.Success -> {
                                if (!prodResult.data.imageUrl.isNullOrBlank()) {
                                    finalEntry = finalEntry.copy(productImageUrl = prodResult.data.imageUrl)
                                }
                            }
                            else -> Unit // Fallback if product deleted
                        }
                    }
                    
                    _uiState.update {
                        it.copy(isLoading = false, entry = finalEntry)
                    }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    /** Gọi khi user tap "Xem đơn hàng" trên màn hình entry SALE */
    fun openSourceOrder(orderId: String) {
        _navigateToOrder.trySend(orderId)
    }
}
