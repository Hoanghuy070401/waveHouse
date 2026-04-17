package com.wavehouse.presentation.supplier

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Supplier
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

data class SupplierListUiState(
    val suppliers: List<Supplier> = emptyList(),
    val searchQuery: String = "",
    val isLoading: Boolean = true,
    val error: String? = null
)

@HiltViewModel
class SupplierListViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val supplierRepository: SupplierRepository
) : ViewModel() {

    private val _allSuppliers = MutableStateFlow<List<Supplier>>(emptyList())
    private val _uiState = MutableStateFlow(SupplierListUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadSuppliers()
    }

    private fun loadSuppliers() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            supplierRepository.getSuppliers(user.warehouseId).collectLatest { result ->
                when (result) {
                    is ApiResult.Success -> {
                        _allSuppliers.value = result.data
                        filterSuppliers(_uiState.value.searchQuery)
                        _uiState.update { it.copy(isLoading = false) }
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(error = result.message, isLoading = false)
                    }
                    ApiResult.Loading -> _uiState.update { it.copy(isLoading = true) }
                }
            }
        }
    }

    fun onSearchQueryChange(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
        filterSuppliers(query)
    }

    private fun filterSuppliers(query: String) {
        val q = query.lowercase().trim()
        if (q.isEmpty()) {
            _uiState.update { it.copy(suppliers = _allSuppliers.value) }
            return
        }
        val filtered = _allSuppliers.value.filter {
            it.name.lowercase().contains(q) ||
            (it.phone?.lowercase()?.contains(q) == true) ||
            (it.email?.lowercase()?.contains(q) == true)
        }
        _uiState.update { it.copy(suppliers = filtered) }
    }
}
