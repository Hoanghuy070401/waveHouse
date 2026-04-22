package com.wavehouse.presentation.debt.payment

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Order
import com.wavehouse.domain.repository.OrderRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.pos.PayDebtUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min

data class DebtPaymentUiState(
    val phone: String = "",
    val name: String = "",
    val orders: List<Order> = emptyList(),
    val isLoading: Boolean = true,
    val isPaying: Boolean = false,
    val isPaymentSuccess: Boolean = false,
    val error: String? = null
) {
    val totalDebt: Double get() = orders.sumOf { it.debtAmount }
}

@HiltViewModel
class DebtPaymentViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val orderRepository: OrderRepository,
    private val payDebtUseCase: PayDebtUseCase
) : ViewModel() {

    private val phone: String = checkNotNull(savedStateHandle["phone"])
    
    private val _uiState = MutableStateFlow(DebtPaymentUiState(phone = phone))
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

    fun submitPayment(amount: Double) {
        if (amount <= 0) return
        
        viewModelScope.launch {
            _uiState.update { it.copy(isPaying = true, error = null) }
            
            var remainingAmount = amount
            // Sort by oldest first to clear old debt
            val ordersToPay = _uiState.value.orders.sortedBy { it.createdAt }
            
            for (order in ordersToPay) {
                if (remainingAmount <= 0) break
                
                val payForThisOrder = min(remainingAmount, order.debtAmount)
                if (payForThisOrder > 0) {
                    when (val result = payDebtUseCase(order.id, payForThisOrder)) {
                        is ApiResult.Success -> {
                            remainingAmount -= payForThisOrder
                        }
                        is ApiResult.Error -> {
                            _uiState.update { it.copy(error = result.message, isPaying = false) }
                            return@launch
                        }
                        ApiResult.Loading -> Unit
                    }
                }
            }
            
            _uiState.update { it.copy(isPaying = false, isPaymentSuccess = true) }
        }
    }
}
