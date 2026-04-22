package com.wavehouse.presentation.debt.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Order
import com.wavehouse.domain.repository.OrderRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.pos.PayDebtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class CustomerDebt(
    val phone: String,
    val name: String,
    val totalDebt: Double,
    val lastUpdate: Long,
    val isOverdue: Boolean,
    val orders: List<Order>
)

data class DebtListUiState(
    val customers: List<CustomerDebt> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null,
    val searchQuery: String = "",
    val filterTab: Int = 0 // 0: Tất cả, 1: Quá hạn, 2: Đã thu (trong tháng)
) {
    val totalDebt: Double get() = customers.sumOf { it.totalDebt }
    val overdueDebt: Double get() = customers.filter { it.isOverdue }.sumOf { it.totalDebt }
    val inTermDebt: Double get() = totalDebt - overdueDebt
}

@HiltViewModel
class DebtListViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val _allOrders = MutableStateFlow<List<Order>>(emptyList())
    private val _uiState = MutableStateFlow(DebtListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDebts()
    }

    private fun loadDebts() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "Chưa đăng nhập") }
                return@launch
            }

            orderRepository.getDebtOrders(user.warehouseId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        _allOrders.value = result.data
                        applyFilters()
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
        applyFilters()
    }
    
    fun onFilterTabChanged(tab: Int) {
        _uiState.update { it.copy(filterTab = tab) }
        applyFilters()
    }

    private fun applyFilters() {
        val q = _uiState.value.searchQuery.lowercase().trim()
        val tab = _uiState.value.filterTab
        
        // Group orders by customer (defaulting missing info to "Khách lẻ")
        val groups = _allOrders.value.groupBy { it.customerPhone ?: "Khách lẻ" }
        
        val overdueThreshold = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000 // 15 days
        
        var customers = groups.map { (phone, orders) ->
            val name = orders.firstOrNull { !it.customerName.isNullOrBlank() }?.customerName ?: "Khách hàng"
            val total = orders.sumOf { it.debtAmount }
            val lastUpdate = orders.maxOfOrNull { it.createdAt } ?: 0L
            val isOverdue = orders.any { it.createdAt < overdueThreshold && it.debtAmount > 0 }
            
            CustomerDebt(
                phone = phone,
                name = if (phone == "Khách lẻ") "Khách hàng vãng lai" else name,
                totalDebt = total,
                lastUpdate = lastUpdate,
                isOverdue = isOverdue,
                orders = orders
            )
        }
        
        // Filter out those with 0 debt (unless we want to show paid off)
        customers = customers.filter { it.totalDebt > 0 }
        
        // Apply search
        if (q.isNotEmpty()) {
            customers = customers.filter { 
                it.name.lowercase().contains(q) || it.phone.contains(q) 
            }
        }
        
        // Apply tab
        customers = when (tab) {
            1 -> customers.filter { it.isOverdue }
            2 -> emptyList() // Đã thu xong (Logic will require tracking paid history. Currently not available)
            else -> customers
        }
        
        // Sort: Overdue first, then by last update
        customers = customers.sortedWith(compareByDescending<CustomerDebt> { it.isOverdue }.thenByDescending { it.lastUpdate })
        
        _uiState.update { it.copy(customers = customers) }
    }
}
