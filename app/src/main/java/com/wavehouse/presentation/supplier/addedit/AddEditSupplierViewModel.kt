package com.wavehouse.presentation.supplier.addedit

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Supplier
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.repository.SupplierRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class AddEditSupplierUiState(
    val name: String = "",
    val phone: String = "",
    val email: String = "",
    val address: String = "",
    val nameError: String? = null,
    val isLoading: Boolean = false,
    val isEditMode: Boolean = false,
    val saveSuccess: Boolean = false,
    val errorMessage: String? = null,
    val warehouseId: String = ""
)

@HiltViewModel
class AddEditSupplierViewModel @Inject constructor(
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val supplierRepository: SupplierRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val editSupplierId: String? = savedStateHandle["supplierId"]

    private val _uiState = MutableStateFlow(AddEditSupplierUiState(isEditMode = editSupplierId != null))
    val uiState = _uiState.asStateFlow()

    init {
        loadUserAndInit()
    }

    private fun loadUserAndInit() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            _uiState.update { it.copy(warehouseId = user.warehouseId) }
            // Fetch supplier for edit. Assume repository can fetch by ID directly,
            // or just load all and find. Here we just assume placeholder for real implementation
            if (editSupplierId != null) {
                // Implementation to load a single supplier would go here. For now, empty skeleton that handles ID.
            }
        }
    }

    fun onNameChange(v: String) = _uiState.update {
        it.copy(name = v, nameError = if (v.isBlank()) "Tên không được để trống" else null)
    }
    fun onPhoneChange(v: String) = _uiState.update { it.copy(phone = v) }
    fun onEmailChange(v: String) = _uiState.update { it.copy(email = v) }
    fun onAddressChange(v: String) = _uiState.update { it.copy(address = v) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun save() {
        val state = _uiState.value
        val nameError = if (state.name.isBlank()) "Tên không được để trống" else null

        if (nameError != null) {
            _uiState.update { it.copy(nameError = nameError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            val supplier = Supplier(
                id = editSupplierId ?: "",
                name = state.name.trim(),
                phone = state.phone.trim().ifBlank { null },
                email = state.email.trim().ifBlank { null },
                address = state.address.trim().ifBlank { null },
                warehouseId = state.warehouseId
            )

            val result = if (state.isEditMode) supplierRepository.updateSupplier(supplier)
                         else supplierRepository.createSupplier(supplier)

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
