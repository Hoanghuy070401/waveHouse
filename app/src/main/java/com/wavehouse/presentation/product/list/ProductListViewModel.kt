package com.wavehouse.presentation.product.list

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.product.DeleteProductUseCase
import com.wavehouse.domain.usecase.product.GetProductsUseCase
import com.wavehouse.domain.usecase.product.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ProductListUiState(
    val products: List<Product> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: String? = null,
    val warehouseId: String = "",
    val deleteSuccess: Boolean = false
)

@OptIn(FlowPreview::class)
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")

    init {
        initUser()
        observeSearch()
    }

    private fun initUser() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            _uiState.update { it.copy(warehouseId = user.warehouseId) }
            loadProducts(user.warehouseId)
        }
    }

    private fun loadProducts(warehouseId: String) {
        viewModelScope.launch {
            getProductsUseCase(warehouseId).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> _uiState.update {
                        it.copy(products = result.data, isLoading = false, isRefreshing = false)
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(error = result.message, isLoading = false, isRefreshing = false)
                    }
                    ApiResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    private fun observeSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    val warehouseId = _uiState.value.warehouseId
                    if (warehouseId.isBlank()) return@collectLatest
                    if (query.isBlank()) {
                        loadProducts(warehouseId)
                    } else {
                        searchProductsUseCase(warehouseId, query).collectLatest { result ->
                            if (result is ApiResult.Success) {
                                _uiState.update { it.copy(products = result.data) }
                            }
                        }
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    fun onRefresh() {
        _uiState.update { it.copy(isRefreshing = true) }
        initUser()
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            deleteProductUseCase(productId)
        }
    }
}
