package com.wavehouse.presentation.debt.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Order
import com.wavehouse.domain.repository.OrderRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
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
    val orders: List<Order>,
    val isFullyPaid: Boolean = false  // true = từng nợ nhưng đã trả hết toàn bộ
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

    /** Dọn dẹp ngầm: reset wasDebt=false cho đơn thanh toán > 3 ngày. */
    private fun triggerCleanup(warehouseId: String) {
        viewModelScope.launch {
            orderRepository.cleanupExpiredWasDebtOrders(warehouseId, olderThanDays = 3)
        }
    }

    private fun loadDebts() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "Chưa đăng nhập") }
                return@launch
            }

            // Chạy cleanup trước khi bắt đầu observe — đảm bảo Firebase đã sạch
            // Cleanup chạy song song, không block UI
            triggerCleanup(user.warehouseId)

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
        applyFilters(query = query, tab = _uiState.value.filterTab)
    }

    fun onFilterTabChanged(tab: Int) {
        _uiState.update { it.copy(filterTab = tab) }
        applyFilters(query = _uiState.value.searchQuery, tab = tab)
    }

    private fun applyFilters(
        query: String = _uiState.value.searchQuery,
        tab: Int = _uiState.value.filterTab
    ) {
        val q = query.lowercase().trim()

        // Group ALL debt-related orders (active + completed) by customer
        val groups = _allOrders.value.groupBy { it.customerPhone ?: "Khách lẻ" }

        val overdueThreshold = System.currentTimeMillis() - 15L * 24 * 60 * 60 * 1000

        var customers = groups.map { (phone, orders) ->
            val name = orders.firstOrNull { !it.customerName.isNullOrBlank() }?.customerName
                ?: if (phone == "Khách lẻ") "Khách hàng vãng lai" else "Khách hàng"
            // Chỉ tính nợ hiện còn lại (DEBT orders)
            val total = orders
                .filter { it.status == com.wavehouse.domain.model.OrderStatus.DEBT }
                .sumOf { it.debtAmount }
            val lastUpdate = orders.maxOfOrNull { it.createdAt } ?: 0L
            val hasActiveDebt = total > 0
            val isOverdue = hasActiveDebt && orders.any {
                it.status == com.wavehouse.domain.model.OrderStatus.DEBT &&
                it.createdAt < overdueThreshold
            }
            val isFullyPaid = run {
                // Khách từng có nợ VÀ đã trả hết toàn bộ
                val allCleared = orders.all { it.status != com.wavehouse.domain.model.OrderStatus.DEBT }
                val hadDebt = orders.any { it.wasDebt }
                // Lấy timestamp gần nhất khi khách trả xong nợ — đối chiếu với 3 ngày
                val lastPaidAt = orders
                    .filter { it.wasDebt && it.status != com.wavehouse.domain.model.OrderStatus.DEBT }
                    .mapNotNull { it.paidAt }
                    .maxOrNull() ?: 0L
                val threeDaysAgo = System.currentTimeMillis() - 3L * 24 * 60 * 60 * 1000
                // Chỉ hiển thị nếu trả nợ trong vòng 3 ngày gần nhất
                allCleared && hadDebt && lastPaidAt >= threeDaysAgo
            }

            CustomerDebt(
                phone = phone,
                name = name,
                totalDebt = total,
                lastUpdate = lastUpdate,
                isOverdue = isOverdue,
                orders = orders,
                isFullyPaid = isFullyPaid
            )
        }

        // Apply search
        if (q.isNotEmpty()) {
            customers = customers.filter {
                it.name.lowercase().contains(q) || it.phone.contains(q)
            }
        }

        // Apply tab filter
        customers = when (tab) {
            1 -> customers.filter { it.isOverdue && !it.isFullyPaid } // Quá hạn
            2 -> customers.filter { it.isFullyPaid }                  // Đã thu xong
            else -> customers.filter { it.totalDebt > 0 }             // Tất cả (chỉ khách còn nợ)
        }

        // Sort: overdue first, then most recent
        customers = customers.sortedWith(
            compareByDescending<CustomerDebt> { it.isOverdue }.thenByDescending { it.lastUpdate }
        )

        _uiState.update { it.copy(customers = customers) }
    }
}
