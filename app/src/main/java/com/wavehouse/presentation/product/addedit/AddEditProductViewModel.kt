package com.wavehouse.presentation.product.addedit

import android.content.Context
import android.net.Uri
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.Category
import com.wavehouse.domain.model.Product
import com.wavehouse.domain.model.UnitOfMeasure
import com.wavehouse.domain.model.UserRole
import com.wavehouse.domain.repository.ProductRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import com.wavehouse.domain.usecase.product.*
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
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
    /** Trọng lượng quy đổi: 1 đơn vị (bó/thùng/túi) = bao nhiêu kg. null = không ấn định */
    val weightPerUnit: String = "",
    /** Cho phép bán lẻ (số thập phân). false = chỉ bán số nguyên */
    val allowDecimal: Boolean = true,
    val imageUrl: String? = null,
    /** Uri ảnh mới vừa chọn (chưa upload) — để preview */
    val pendingImageUri: Uri? = null,
    val isUploadingImage: Boolean = false,

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
    val warehouseId: String = "",

    // Phân quyền: chỉ ADMIN và WAREHOUSE mới được thêm/sửa sản phẩm
    val isAccessDenied: Boolean = false
) {
    /** true nếu đơn vị chọn là hệ cân (đã là kg, không cần quy đổi) */
    val isWeightUnit: Boolean get() = selectedUnitName.lowercase() in setOf("kg", "gr", "gram", "g", "tấn")
}

@HiltViewModel
class AddEditProductViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val getProductByIdUseCase: GetProductByIdUseCase,
    private val createProductUseCase: CreateProductUseCase,
    private val updateProductUseCase: UpdateProductUseCase,
    private val productRepository: ProductRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    /** Bytes ảnh đang chờ upload sau khi save (chỉ giữ trong scope VM) */
    private var pendingImageBytes: ByteArray? = null

    private val editProductId: String? = savedStateHandle["productId"]

    private val _uiState = MutableStateFlow(AddEditProductUiState(isEditMode = editProductId != null))
    val uiState = _uiState.asStateFlow()

    init {
        loadUserAndInit()
    }

    private fun loadUserAndInit() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase() ?: return@launch
            // Chỉ ADMIN và WAREHOUSE mới được thêm/sửa sản phẩm
            val hasAccess = user.role == UserRole.ADMIN || user.role == UserRole.WAREHOUSE
            if (!hasAccess) {
                _uiState.update { it.copy(isAccessDenied = true) }
                return@launch
            }
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
                            allowDecimal = p.allowDecimal,
                            // Phân tích cả số thập phân (ví dụ: 0.5 kg/bó)
                            weightPerUnit = p.weightPerUnit?.toString() ?: "",
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
        // Reset weightPerUnit khi chuyển sang đơn vị cân (kg/gr)
        it.copy(selectedUnitId = id, selectedUnitName = name, weightPerUnit = "")
    }
    /** Cho phép số thập phân dương — 1 bó/thùng/túi = N kg (ví dụ: 0.5, 1.2, 5) */
    fun onWeightPerUnitChange(v: String) = _uiState.update {
        // Filter: chữ số + tối đa 1 dấu phân cách (chấm hoặc phẩy)
        val normalized = v.replace(',', '.')
        val filtered = normalized.filter { c -> c.isDigit() || c == '.' }.let { s ->
            val parts = s.split('.')
            if (parts.size > 2) parts[0] + '.' + parts[1] else s
        }
        it.copy(weightPerUnit = filtered)
    }
    fun onAllowDecimalChange(v: Boolean) = _uiState.update { it.copy(allowDecimal = v) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    /** Người dùng chọn ảnh mới từ picker — đọc bytes và lưu tạm, chưa upload. */
    fun onPickImage(uri: Uri) {
        viewModelScope.launch {
            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()
            if (bytes == null || bytes.isEmpty()) {
                _uiState.update { it.copy(errorMessage = "Không đọc được ảnh đã chọn") }
                return@launch
            }
            pendingImageBytes = bytes
            _uiState.update { it.copy(pendingImageUri = uri) }
        }
    }

    /** Bỏ ảnh (hoặc ảnh đang chọn hoặc xoá ảnh hiện tại sau khi save). */
    fun onRemoveImage() {
        pendingImageBytes = null
        _uiState.update { it.copy(pendingImageUri = null, imageUrl = null) }
    }

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
            val baseProduct = Product(
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
                minStock = state.minStock.toDoubleOrNull() ?: 0.0,
                warehouseId = state.warehouseId,
                imageUrl = state.imageUrl,
                weightPerUnit = if (!state.isWeightUnit) state.weightPerUnit.toDoubleOrNull() else null,
                allowDecimal = state.allowDecimal
            )

            // Bước 1: create/update (chưa có imageUrl mới) để lấy productId chắc chắn.
            val productId: String = if (state.isEditMode) {
                when (val r = updateProductUseCase(baseProduct)) {
                    is ApiResult.Success -> baseProduct.id
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = r.message) }
                        return@launch
                    }
                    ApiResult.Loading -> return@launch
                }
            } else {
                when (val r = createProductUseCase(baseProduct)) {
                    is ApiResult.Success -> r.data
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(isLoading = false, errorMessage = r.message) }
                        return@launch
                    }
                    ApiResult.Loading -> return@launch
                }
            }

            // Bước 2: nếu có ảnh mới được chọn → upload & cập nhật imageUrl; xoá ảnh cũ (nếu khác file).
            val bytes = pendingImageBytes
            if (bytes != null) {
                _uiState.update { it.copy(isUploadingImage = true) }
                val previousUrl = baseProduct.imageUrl
                when (val upload = productRepository.uploadProductImage(productId, bytes)) {
                    is ApiResult.Success -> {
                        val newUrl = upload.data
                        // Update DB với URL mới
                        val updated = baseProduct.copy(id = productId, imageUrl = newUrl)
                        when (val saveUrl = updateProductUseCase(updated)) {
                            is ApiResult.Success -> {
                                // Ảnh mới giờ ghi đè file cùng đường dẫn nên không cần xoá.
                                // Chỉ xoá khi URL thực sự khác (ví dụ bị người khác thay đổi đường dẫn).
                                if (!previousUrl.isNullOrBlank() && previousUrl != newUrl) {
                                    productRepository.deleteProductImage(previousUrl)
                                }
                                pendingImageBytes = null
                                _uiState.update {
                                    it.copy(
                                        isLoading = false,
                                        isUploadingImage = false,
                                        saveSuccess = true,
                                        imageUrl = newUrl,
                                        pendingImageUri = null
                                    )
                                }
                            }
                            is ApiResult.Error -> {
                                // Rollback ảnh mới nếu ghi DB fail
                                productRepository.deleteProductImage(newUrl)
                                _uiState.update {
                                    it.copy(isLoading = false, isUploadingImage = false, errorMessage = saveUrl.message)
                                }
                            }
                            ApiResult.Loading -> Unit
                        }
                    }
                    is ApiResult.Error -> _uiState.update {
                        it.copy(isLoading = false, isUploadingImage = false, errorMessage = upload.message)
                    }
                    ApiResult.Loading -> Unit
                }
            } else {
                // Không chọn ảnh mới; nếu người dùng bấm "Xoá ảnh" (imageUrl=null nhưng trước đó có)
                val previousUrl = if (state.isEditMode) {
                    runCatching { getProductByIdUseCase(productId) }.getOrNull()
                        ?.let { (it as? ApiResult.Success)?.data?.imageUrl }
                } else null
                if (state.isEditMode && !previousUrl.isNullOrBlank() && state.imageUrl == null) {
                    productRepository.deleteProductImage(previousUrl)
                }
                _uiState.update { it.copy(isLoading = false, saveSuccess = true) }
            }
        }
    }
}
