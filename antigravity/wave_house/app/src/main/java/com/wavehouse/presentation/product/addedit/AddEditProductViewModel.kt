package com.wavehouse.presentation.product.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Category
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.model.UnitOfMeasure
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.product.*
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditProductUiState(
    // Form fields
    val name: String = "",
    val sku: String = "",
    val barcode: String = "",
    val description: String = "",
    val costPrice: String = "",
    val salePrice: String = "",
    val minStock: String = "0",
    val selectedCategoryId: String = "",
    val selectedCategoryName: String = "",
    val selectedUnitId: String = "",
    val selectedUnitName: String = "",
    val imageUrl: String? = null,

    // Validation errors
    val nameError: String? = null,
    val skuError: String? = null,

    // State
    val categories: List<Category> = emptyList(),
    val units: List<UnitOfMeasure> = emptyList(),
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val warehouseId: String = ""
)

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val createProductUseCase: CreateProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editProductId: String? = savedStateHandle["productId"]

    private val _uiState = MutableStateFlow(AddEditProductUiState(isEditMode = editProductId != null))
    val uiState = _uiState.asStateFlow()

    init {
        loadUserAndInit()
    }

    private fun loadUserAndInit() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            _uiState.update { it.copy(warehouseId = user.warehouseId) }
            if (editProductId != null) loadProductForEdit(editProductId)
        }
    }

    private fun loadProductForEdit(id: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            when (val result = getProductByIdUseCase(id)) {
                is ApiResult.Success -> result.data.let { p ->
                    _uiState.update {
                        it.copy(
                            name = p.name,
                            sku = p.sku,
                            barcode = p.barcode ?: "",
                            description = p.description ?: "",
                            costPrice = if (p.costPrice > 0) p.costPrice.toLong().toString() else "",
                            salePrice = if (p.salePrice > 0) p.salePrice.toLong().toString() else "",
                            minStock = p.minStock.toString(),
                            selectedCategoryId = p.categoryId ?: "",
                            selectedCategoryName = p.categoryName ?: "",
                            selectedUnitId = p.unitId ?: "",
                            selectedUnitName = p.unitName ?: "",
                            imageUrl = p.imageUrl,
                            isLoading = false
                        )
                    }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(errorMessage = result.message, isLoading = false)
                }
                ApiResult.Loading -> {}
            }
        }
    }

    // Field update handlers
    fun onNameChange(v: String) = _uiState.update {
        it.copy(name = v, nameError = if (v.isBlank()) "Tên không được để trống" else null)
    }
    fun onSkuChange(v: String) = _uiState.update {
        it.copy(sku = v, skuError = if (v.isBlank()) "SKU không được để trống" else null)
    }
    fun onBarcodeChange(v: String) = _uiState.update { it.copy(barcode = v) }
    fun onDescriptionChange(v: String) = _uiState.update { it.copy(description = v) }
    fun onCostPriceChange(v: String) = _uiState.update { it.copy(costPrice = v.filter { c -> c.isDigit() }) }
    fun onSalePriceChange(v: String) = _uiState.update { it.copy(salePrice = v.filter { c -> c.isDigit() }) }
    fun onMinStockChange(v: String) = _uiState.update { it.copy(minStock = v.filter { c -> c.isDigit() }) }
    fun onCategorySelected(id: String, name: String) = _uiState.update {
        it.copy(selectedCategoryId = id, selectedCategoryName = name)
    }
    fun onUnitSelected(id: String, name: String) = _uiState.update {
        it.copy(selectedUnitId = id, selectedUnitName = name)
    }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun save() {
        val state = _uiState.value
        val nameError = if (state.name.isBlank()) "Tên không được để trống" else null
        val skuError = if (state.sku.isBlank()) "SKU không được để trống" else null
        if (nameError != null || skuError != null) {
            _uiState.update { it.copy(nameError = nameError, skuError = skuError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val product = Product(
                id = editProductId ?: "",
                name = state.name.trim(),
                sku = state.sku.trim().uppercase(),
                barcode = state.barcode.trim().ifBlank { null },
                categoryId = state.selectedCategoryId.ifBlank { null },
                categoryName = state.selectedCategoryName.ifBlank { null },
                unitId = state.selectedUnitId.ifBlank { null },
                unitName = state.selectedUnitName.ifBlank { null },
                description = state.description.trim().ifBlank { null },
                costPrice = state.costPrice.toDoubleOrNull() ?: 0.0,
                salePrice = state.salePrice.toDoubleOrNull() ?: 0.0,
                minStock = state.minStock.toIntOrNull() ?: 0,
                warehouseId = state.warehouseId,
                imageUrl = state.imageUrl
            )

            val result = if (state.isEditMode) updateProductUseCase(product)
                         else createProductUseCase(product)

            when (result) {
                is ApiResult.Success -> _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isLoading = false, errorMessage = result.message)
                }
                ApiResult.Loading -> {}
            }
        }
    }
}
