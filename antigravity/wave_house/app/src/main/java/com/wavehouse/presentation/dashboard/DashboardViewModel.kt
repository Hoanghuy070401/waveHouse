package com.wavehouse.presentation.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.DashboardStats
import com.wavehouse.domain.model.StockEntry
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.stock.GetDashboardStatsUseCase
import com.wavehouse.domain.usecase.stock.GetStockHistoryUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DashboardUiState(
    val userName: String = "",
    val warehouseId: String = "",
    val stats: DashboardStats = DashboardStats(),
    val recentEntries: List<StockEntry> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class DashboardViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getDashboardStatsUseCase: GetDashboardStatsUseCase,
    private val getStockHistoryUseCase: GetStockHistoryUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(DashboardUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadDashboard()
    }

    fun loadDashboard() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "Chưa đăng nhập") }
                return@launch
            }
            _uiState.update { it.copy(userName = user.name, warehouseId = user.warehouseId) }
            loadStats(user.warehouseId)
            loadRecentHistory(user.warehouseId)
        }
    }

    private fun loadStats(warehouseId: String) {
        viewModelScope.launch {
            getDashboardStatsUseCase(warehouseId).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(stats = result.data, isLoading = false)
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(error = result.message, isLoading = false)
                    }
                    ApiResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun loadRecentHistory(warehouseId: String) {
        viewModelScope.launch {
            getStockHistoryUseCase(warehouseId).collectLatest { result ->
                if (result is ApiResult.Success) {
                    _uiState.update { it.copy(recentEntries = result.data.take(10)) }
                }
            }
        }
    }
}
