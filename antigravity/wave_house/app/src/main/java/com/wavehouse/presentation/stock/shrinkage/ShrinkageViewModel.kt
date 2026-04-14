package com.wavehouse.presentation.stock.shrinkage

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.*
import com.wavehouse.domain.repository.ProductRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.stock.CreateShrinkageUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ShrinkageUiState(
    val products: List<Product> = emptyList(),
    val selectedProduct: Product? = null,
    val quantity: String = "",
    val selectedReason: ShrinkageReason = ShrinkageReason.DAMAGED,
    val note: String = "",
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val saveSuccess: Boolean = false,
    val error: String? = null
)

@HiltViewModel
class ShrinkageViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val productRepository: ProductRepository,
    private val createShrinkageUseCase: CreateShrinkageUseCase
) : ViewModel() {

    private val _allProducts = MutableStateFlow<List<Product>>(emptyList())
    private val _uiState = MutableStateFlow(ShrinkageUiState())
    val uiState = _uiState.asStateFlow()

    private var currentUser: User? = null

    init {
        loadProducts()
    }

    private fun loadProducts() {
        viewModelScope.launch {
            currentUser = getCurrentUserUseCase() ?: return@launch
            productRepository.getProducts(currentUser!!.warehouseId).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        _allProducts.value = result.data.filter { it.currentStock > 0 }
                        filterProducts(_uiState.value.searchQuery)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is ApiResult.Error -> _uiState.update { it.copy(error = result.message, isLoading = false) }
                    ApiResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterProducts(query)
    }

    private fun filterProducts(query: String) {
        val q = query.lowercase().trim()
        if (q.isEmpty()) {
            _uiState.update { it.copy(products = _allProducts.value) }
            return
        }
        _uiState.update { state ->
            state.copy(products = _allProducts.value.filter {
                it.name.lowercase().contains(q) || it.sku.lowercase().contains(q)
            })
        }
    }

    fun selectProduct(product: Product) {
        _uiState.update { it.copy(selectedProduct = product) }
    }

    fun onQuantityChange(value: String) {
        _uiState.update { it.copy(quantity = value.filter { c -> c.isDigit() || c == '.' }) }
    }

    fun onReasonChange(reason: ShrinkageReason) {
        _uiState.update { it.copy(selectedReason = reason) }
    }

    fun onNoteChange(value: String) {
        _uiState.update { it.copy(note = value) }
    }

    fun save() {
        val user = currentUser ?: return
        val state = _uiState.value
        val product = state.selectedProduct ?: return
        val qty = state.quantity.toIntOrNull() ?: return

        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }

            val result = createShrinkageUseCase(
                productId = product.id,
                warehouseId = user.warehouseId,
                quantity = qty,
                currentStock = product.currentStock,
                reason = state.selectedReason,
                note = state.note.ifBlank { null }
            )

            when (result) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isSaving = false,
                        saveSuccess = true,
                        selectedProduct = null,
                        quantity = "",
                        note = ""
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSaving = false, error = result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun clearSuccess() {
        _uiState.update { it.copy(saveSuccess = false) }
    }
}
