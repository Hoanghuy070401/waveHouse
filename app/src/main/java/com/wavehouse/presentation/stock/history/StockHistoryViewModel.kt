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

import com.wavehouse.domain.model.Order
import com.wavehouse.domain.repository.OrderRepository
import com.wavehouse.domain.repository.ProductRepository

data class StockHistoryUiState(
    val entries: List<StockEntry> = emptyList(),
    val orders: List<Order> = emptyList(),
    val filterIndex: Int = 0, // 0=Bán lẻ (Orders), 1=Nhập kho, 2=Xuất Cũ, 3=Hao hụt
    val inStockPercentage: Float = 0f, // Tỷ lệ còn hàng
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class StockHistoryViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getStockHistoryUseCase: GetStockHistoryUseCase,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _allEntries = MutableStateFlow<List<StockEntry>>(emptyList())
    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    private val _uiState = MutableStateFlow(StockHistoryUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            
            // 1. Collect Orders (CHO BÁN LẺ)
            launch {
                orderRepository.getOrders(user.warehouseId, limit = 300).collectLatest { result ->
                    if (result is ApiResult.Success) {
                        _allOrders.value = result.data
                        applyFilter(_uiState.value.filterIndex)
                    }
                }
            }

            // 2. Collect Stock Entries (CHO NHẬP / XUẤT)
            launch {
                getStockHistoryUseCase(user.warehouseId).collectLatest { result ->
                    when (result) {
                        is ApiResult.Success -> {
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
            // 3. Collect Products to calculate stock percentage
            launch {
                productRepository.getProducts(user.warehouseId).collectLatest { result ->
                    if (result is ApiResult.Success) {
                        val products = result.data
                        if (products.isNotEmpty()) {
                            val inStockCount = products.count { it.currentStock > 0.0 }
                            val percentage = inStockCount.toFloat() / products.size.toFloat()
                            _uiState.update { it.copy(inStockPercentage = percentage) }
                        } else {
                            _uiState.update { it.copy(inStockPercentage = 0f) }
                        }
                    }
                }
            }
        }
    }

    fun setFilter(index: Int) {
        _uiState.update { it.copy(filterIndex = index) }
        applyFilter(index)
    }

    private fun applyFilter(index: Int) {
        val filteredEntries = when (index) {
            1 -> _allEntries.value.filter { it.type == StockEntryType.IN }
            2 -> _allEntries.value.filter { it.type == StockEntryType.OUT }
            3 -> _allEntries.value.filter { it.type == StockEntryType.SHRINKAGE }
            else -> emptyList() // Bán lẻ không dùng cái này, dùng orders
        }
        val filteredOrders = if (index == 0) _allOrders.value else emptyList()
        
        _uiState.update { it.copy(entries = filteredEntries, orders = filteredOrders) }
    }
}
