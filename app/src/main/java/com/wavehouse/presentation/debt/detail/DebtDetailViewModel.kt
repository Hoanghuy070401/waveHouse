package com.wavehouse.presentation.debt.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Order
import com.wavehouse.domain.repository.OrderRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class DebtDetailUiState(
    val phone: String = "",
    val name: String = "",
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val error: String? = null
) {
    val totalDebt: Double get() = orders.sumOf { it.debtAmount }
}

@HiltViewModel
class DebtDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val orderRepository: OrderRepository
) : ViewModel() {

    private val phone: String = checkNotNull(savedStateHandle["phone"])
    
    private val _uiState = MutableStateFlow(DebtDetailUiState(phone = phone))
    val uiState = _uiState.asStateFlow()

    init {
        loadCustomerDebt()
    }

    private fun loadCustomerDebt() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "Chưa đăng nhập") }
                return@launch
            }

            orderRepository.getDebtOrders(user.warehouseId).collect { result ->
                when (result) {
                    is ApiResult.Success -> {
                        val customerOrders = result.data.filter { 
                            (it.customerPhone ?: "Khách lẻ") == phone 
                        }
                        val name = customerOrders.firstOrNull { !it.customerName.isNullOrBlank() }?.customerName 
                            ?: if (phone == "Khách lẻ") "Khách hàng vãng lai" else "Khách hàng"
                            
                        _uiState.update { 
                            it.copy(
                                isLoading = false,
                                orders = customerOrders,
                                name = name
                            )
                        }
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
