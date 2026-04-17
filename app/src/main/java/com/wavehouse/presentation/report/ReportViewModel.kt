package com.wavehouse.presentation.report

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.ReportStats
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.stock.GetReportStatsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ReportUiState(
    val isLoading: Boolean = true,
    val error: String? = null,
    val stats: ReportStats = ReportStats(),
    val days: Int = 0 // 0 = Hôm nay, 7 = Tuần này, 30 = Tháng này
)

@HiltViewModel
class ReportViewModel @Inject constructor(
    private val getReportStatsUseCase: GetReportStatsUseCase,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ReportUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadStatsForDays(0)
    }

    fun onTimeFilterChanged(filterIndex: Int) {
        val days = when (filterIndex) {
            0 -> 0 // Hôm nay
            1 -> 7 // Tuần này
            2 -> 30 // Tháng này
            else -> 0
        }
        _uiState.update { it.copy(days = days, isLoading = true) }
        loadStatsForDays(days)
    }

    private fun loadStatsForDays(days: Int) {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            if (user == null) {
                _uiState.update { it.copy(error = "Chưa đăng nhập", isLoading = false) }
                return@launch
            }
            val warehouseId = user.warehouseId
            if (warehouseId.isBlank()) {
                _uiState.update { it.copy(error = "Chưa chọn kho hàng", isLoading = false) }
                return@launch
            }

            getReportStatsUseCase(warehouseId, days).collectLatest { result ->
                when (result) {
                    is ApiResult.Loading -> _uiState.update { it.copy(isLoading = true, error = null) }
                    is ApiResult.Success -> _uiState.update {
                        it.copy(
                            isLoading = false,
                            stats = result.data,
                            error = null
                        )
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(isLoading = false, error = result.message)
                    }
                }
            }
        }
    }
}
