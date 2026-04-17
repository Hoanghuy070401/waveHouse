package com.wavehouse.presentation.order.history

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.core.utils.todayStartMillis
import com.wavehouse.domain.model.Order
import com.wavehouse.domain.repository.AuthRepository
import com.wavehouse.domain.repository.OrderRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.Calendar
import javax.inject.Inject

/** End-of-day epoch for current day */
fun todayEndMillis(): Long = Calendar.getInstance().apply {
    set(Calendar.HOUR_OF_DAY, 23)
    set(Calendar.MINUTE, 59)
    set(Calendar.SECOND, 59)
    set(Calendar.MILLISECOND, 999)
}.timeInMillis

data class OrderHistoryUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val errorMessage: String? = null,
    val startDate: Long = todayStartMillis(),
    val endDate: Long = todayEndMillis()
)

@HiltViewModel
class OrderHistoryViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderHistoryUiState())
    val uiState: StateFlow<OrderHistoryUiState> = _uiState.asStateFlow()

    private var allOrders: List<Order> = emptyList()

    init {
        loadHistory()
    }

    fun loadHistory() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }

            // Get warehouseId from current user profile
            val user = authRepository.getCurrentUser()
            val warehouseId = user?.warehouseId
            if (warehouseId.isNullOrBlank()) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Không xác định được kho hàng") }
                return@launch
            }

            // Subscribe real-time (last 300 orders, client-side filter)
            orderRepository.getOrders(warehouseId, limit = 300).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        allOrders = result.data
                        applyFilter()
                    }
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                    }
                    ApiResult.Loading -> {
                        _uiState.update { it.copy(isLoading = true) }
                    }
                }
            }
        }
    }

    fun setDateRange(start: Long, end: Long) {
        _uiState.update { it.copy(startDate = start, endDate = end) }
        applyFilter()
    }

    private fun applyFilter() {
        val start = _uiState.value.startDate
        val end = _uiState.value.endDate
        val filtered = allOrders.filter {
            it.createdAt in start..end
        }.sortedByDescending { it.createdAt }

        _uiState.update {
            it.copy(isLoading = false, orders = filtered)
        }
    }
}

