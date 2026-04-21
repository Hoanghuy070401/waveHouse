package com.wavehouse.presentation.order.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Order
import com.wavehouse.domain.repository.OrderRepository
import com.wavehouse.domain.repository.ProductRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OrderDetailUiState(
    val isLoading: Boolean = true,
    val order: Order? = null,
    val itemImages: Map<String, String?> = emptyMap(),
    val errorMessage: String? = null
)

@HiltViewModel
class OrderDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val orderRepository: OrderRepository,
    private val productRepository: ProductRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(OrderDetailUiState())
    val uiState: StateFlow<OrderDetailUiState> = _uiState.asStateFlow()

    private val orderId: String = checkNotNull(savedStateHandle["orderId"])

    init {
        loadOrder()
    }

    private fun loadOrder() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = orderRepository.getOrderById(orderId)) {
                is ApiResult.Success -> {
                    val order = result.data
                    val itemImages = fetchProductImages(order)
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            order = order,
                            itemImages = itemImages
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = result.message,
                            order = null,
                            itemImages = emptyMap()
                        )
                    }
                }
                is ApiResult.Loading -> {}
            }
        }
    }

    private suspend fun fetchProductImages(order: Order): Map<String, String?> {
        val productIds = order.items.map { it.productId }.distinct()
        if (productIds.isEmpty()) return emptyMap()

        val images = mutableMapOf<String, String?>()
        for (productId in productIds) {
            when (val productResult = productRepository.getProductById(productId)) {
                is ApiResult.Success -> images[productId] = productResult.data.imageUrl
                else -> images[productId] = null
            }
        }
        return images
    }
}
