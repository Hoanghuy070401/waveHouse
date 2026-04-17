package com.wavehouse.presentation.product.detail

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.PriceRecord
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.model.User
import com.wavehouse.domain.repository.AuthRepository
import com.wavehouse.domain.usecase.product.GetProductByIdUseCase
import com.wavehouse.domain.usecase.product.GetProductPriceHistoryUseCase
import com.wavehouse.domain.usecase.product.UpdateProductPriceUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductDetailUiState(
    val product: Product? = null,
    val priceHistory: List<PriceRecord> = emptyList(),
    val isLoading: Boolean = true,
    val isHistoryLoading: Boolean = true,
    val isUpdatingPrice: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null,
    val showUpdatePriceDialog: Boolean = false
)

@HiltViewModel
class ProductDetailViewModel @Inject constructor(
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val getProductPriceHistoryUseCase: GetProductPriceHistoryUseCase,
    private val updateProductPriceUseCase: UpdateProductPriceUseCase,
    private val authRepository: AuthRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val productId: String = checkNotNull(savedStateHandle["productId"])
    private var currentUser: User? = null

    private val _uiState = MutableStateFlow(ProductDetailUiState())
    val uiState = _uiState.asStateFlow()

    init {
        viewModelScope.launch { currentUser = authRepository.getCurrentUser() }
        loadProduct()
        loadPriceHistory()
    }

    fun loadProduct() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getProductByIdUseCase(productId)) {
                is ApiResult.Success -> _uiState.update { it.copy(product = result.data, isLoading = false) }
                is ApiResult.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                ApiResult.Loading -> {}
            }
        }
    }

    private fun loadPriceHistory() {
        getProductPriceHistoryUseCase(productId)
            .onEach { result ->
                when (result) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(priceHistory = result.data, isHistoryLoading = false)
                    }
                    is ApiResult.Error -> _uiState.update { it.copy(isHistoryLoading = false) }
                    ApiResult.Loading -> _uiState.update { it.copy(isHistoryLoading = true) }
                }
            }
            .launchIn(viewModelScope)
    }

    fun showUpdatePriceDialog() = _uiState.update { it.copy(showUpdatePriceDialog = true) }
    fun hideUpdatePriceDialog() = _uiState.update { it.copy(showUpdatePriceDialog = false) }

    fun updatePrice(
        costPrice: Double,
        salePrice: Double,
        reason: String?
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isUpdatingPrice = true) }
            val result = updateProductPriceUseCase(
                productId, costPrice, salePrice,
                updatedBy = currentUser?.id ?: "unknown",
                updatedByName = currentUser?.name,
                reason = reason
            )
            when (result) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isUpdatingPrice = false,
                            showUpdatePriceDialog = false,
                            successMessage = "Cập nhật giá thành công"
                        )
                    }
                    loadProduct() // refresh product with new prices
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isUpdatingPrice = false, error = result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(error = null) }
    fun clearSuccess() = _uiState.update { it.copy(successMessage = null) }
}
