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
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.flatMapLatest
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

@OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
@HiltViewModel
class ProductListViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getProductsUseCase: GetProductsUseCase,
    private val searchProductsUseCase: SearchProductsUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProductListUiState())
    val uiState = _uiState.asStateFlow()

    private val _warehouseId = MutableStateFlow("")
    private val _searchQuery = MutableStateFlow("")

    init {
        initUser()
        observeProducts()
    }

    private fun initUser() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            if (user == null) {
                _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = "Chưa đăng nhập")
                }
                return@launch
            }
            _uiState.update { it.copy(warehouseId = user.warehouseId) }
            _warehouseId.value = user.warehouseId
        }
    }

    /** Single product stream driven by warehouseId + debounced query.
     *  `flatMapLatest` auto-cancels the previous Firebase listener when inputs change,
     *  preventing duplicate collectors from accumulating across refresh/search. */
    private fun observeProducts() {
        viewModelScope.launch {
            combine(
                _warehouseId.filter { it.isNotBlank() }.distinctUntilChanged(),
                _searchQuery.debounce(300).distinctUntilChanged()
            ) { wId, query -> wId to query.trim() }
                .flatMapLatest { (wId, query) ->
                    if (query.isBlank()) getProductsUseCase(wId)
                    else searchProductsUseCase(wId, query)
                }
                .collectLatest { result ->
                    when (result) {
                        is ApiResult.Success -> _uiState.update {
                            it.copy(products = result.data, isLoading = false, isRefreshing = false, error = null)
                        }
                        is ApiResult.Error -> _uiState.update {
                            it.copy(error = result.message, isLoading = false, isRefreshing = false)
                        }
                        ApiResult.Loading -> _uiState.update { it.copy(isLoading = true) }
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
        // Re-emit the current warehouseId so the flow re-fetches without spawning new collectors.
        val current = _warehouseId.value
        if (current.isBlank()) {
            initUser()
        } else {
            _warehouseId.value = ""
            _warehouseId.value = current
        }
    }

    fun deleteProduct(productId: String) {
        viewModelScope.launch {
            deleteProductUseCase(productId)
        }
    }
}
