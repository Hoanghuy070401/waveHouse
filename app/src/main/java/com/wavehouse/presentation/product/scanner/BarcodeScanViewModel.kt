package com.wavehouse.presentation.product.scanner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.product.GetProductByBarcodeUseCase
import com.wavehouse.domain.usecase.product.SearchProductsUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class BarcodeScanUiState(
    // Barcode mode
    val isLoadingBarcode: Boolean = false,
    val barcodeError: String? = null,
    val foundProductId: String? = null,

    // Search mode
    val searchQuery: String = "",
    val searchResults: List<Product> = emptyList(),
    val isSearching: Boolean = false,

    val warehouseId: String = ""
)

@OptIn(FlowPreview::class)
@HiltViewModel
class BarcodeScanViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getProductByBarcodeUseCase: GetProductByBarcodeUseCase,
    private val searchProductsUseCase: SearchProductsUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(BarcodeScanUiState())
    val uiState = _uiState.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    private var lastScannedBarcode: String? = null

    init {
        loadUser()
        setupSearch()
    }

    private fun loadUser() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            _uiState.update { it.copy(warehouseId = user?.warehouseId ?: "") }
        }
    }

    private fun setupSearch() {
        viewModelScope.launch {
            _searchQuery
                .debounce(300)
                .distinctUntilChanged()
                .collectLatest { query ->
                    if (query.isBlank()) {
                        _uiState.update { it.copy(searchResults = emptyList(), isSearching = false) }
                        return@collectLatest
                    }
                    val warehouseId = _uiState.value.warehouseId
                    _uiState.update { it.copy(isSearching = true) }
                    searchProductsUseCase(warehouseId, query).collectLatest { result ->
                        if (result is ApiResult.Success) {
                            _uiState.update {
                                it.copy(searchResults = result.data, isSearching = false)
                            }
                        } else {
                            _uiState.update { it.copy(isSearching = false) }
                        }
                    }
                }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        _searchQuery.value = query
    }

    fun onBarcodeDetected(barcode: String) {
        // Debounce: ignore same barcode scanned repeatedly
        if (barcode == lastScannedBarcode) return
        lastScannedBarcode = barcode

        viewModelScope.launch {
            _uiState.update { it.copy(isLoadingBarcode = true, barcodeError = null) }
            when (val result = getProductByBarcodeUseCase(barcode)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(isLoadingBarcode = false, foundProductId = result.data.id)
                }
                is ApiResult.Error -> {
                    _uiState.update {
                        it.copy(
                            isLoadingBarcode = false,
                            barcodeError = "Không tìm thấy sản phẩm với barcode: $barcode"
                        )
                    }
                    // Reset barcode to allow retry
                    lastScannedBarcode = null
                }
                ApiResult.Loading -> {}
            }
        }
    }
}
