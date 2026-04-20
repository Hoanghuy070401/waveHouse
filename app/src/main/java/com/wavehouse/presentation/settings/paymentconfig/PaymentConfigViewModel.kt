package com.wavehouse.presentation.settings.paymentconfig

import android.content.Context
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.User
import com.wavehouse.domain.repository.AuthRepository
import com.wavehouse.domain.repository.WarehouseRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class PaymentConfigUiState(
    val isLoading: Boolean = true,
    val isAdmin: Boolean = false,
    val isEmailVerified: Boolean = false,
    val warehouseId: String = "",
    val warehouseName: String = "",
    val qrImageUrl: String? = null,
    val isUploading: Boolean = false,
    val isSaving: Boolean = false,
    val error: String? = null
) {
    val canEdit: Boolean get() = isAdmin && isEmailVerified
}

@HiltViewModel
class PaymentConfigViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val getCurrentUserUseCase: GetCurrentUserUseCase,
    private val warehouseRepository: WarehouseRepository,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PaymentConfigUiState())
    val uiState = _uiState.asStateFlow()

    private val _events = Channel<String>(Channel.BUFFERED)
    val events: Flow<String> = _events.receiveAsFlow()

    private var currentUser: User? = null

    init {
        loadInitial()
    }

    private fun loadInitial() {
        viewModelScope.launch {
            val user = getCurrentUserUseCase()
            if (user == null) {
                _uiState.update { it.copy(isLoading = false, error = "Chưa đăng nhập") }
                return@launch
            }
            currentUser = user
            val verified = runCatching { authRepository.reloadAndCheckVerified() }.getOrDefault(false)

            when (val result = warehouseRepository.getWarehouseById(user.warehouseId)) {
                is ApiResult.Success -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAdmin = user.isAdmin,
                        isEmailVerified = verified,
                        warehouseId = result.data.id,
                        warehouseName = result.data.name,
                        qrImageUrl = result.data.qrImageUrl
                    )
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(
                        isLoading = false,
                        isAdmin = user.isAdmin,
                        isEmailVerified = verified,
                        warehouseId = user.warehouseId,
                        error = result.message
                    )
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun onPickImage(uri: Uri) {
        val state = _uiState.value
        if (!state.canEdit) {
            _events.trySend("Chỉ ADMIN đã xác minh email mới có thể đổi mã QR")
            return
        }
        val warehouseId = state.warehouseId.ifBlank { return }
        val previousUrl = state.qrImageUrl

        viewModelScope.launch {
            _uiState.update { it.copy(isUploading = true, error = null) }

            val bytes = runCatching {
                context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            }.getOrNull()

            if (bytes == null || bytes.isEmpty()) {
                _uiState.update { it.copy(isUploading = false, error = "Không đọc được ảnh đã chọn") }
                return@launch
            }

            if (!previousUrl.isNullOrBlank()) {
                when (val deleteResult = warehouseRepository.deleteQrImage(previousUrl)) {
                    is ApiResult.Error -> {
                        _uiState.update { it.copy(isUploading = false, error = deleteResult.message) }
                        _events.trySend("Không thể xoá ảnh QR cũ. Vui lòng thử lại.")
                        return@launch
                    }
                    else -> Unit
                }
            }

            when (val upload = warehouseRepository.uploadQrImage(warehouseId, bytes)) {
                is ApiResult.Success -> {
                    val newUrl = upload.data
                    when (val save = warehouseRepository.updateQrImageUrl(warehouseId, newUrl)) {
                        is ApiResult.Success -> {
                            _uiState.update {
                                it.copy(isUploading = false, qrImageUrl = newUrl, error = null)
                            }
                            _events.trySend("Đã cập nhật mã QR")
                        }
                        is ApiResult.Error -> {
                            warehouseRepository.deleteQrImage(newUrl)
                            _uiState.update {
                                it.copy(isUploading = false, error = save.message)
                            }
                        }
                        ApiResult.Loading -> Unit
                    }
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isUploading = false, error = upload.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun removeQrImage() {
        val state = _uiState.value
        if (!state.canEdit) return
        val warehouseId = state.warehouseId.ifBlank { return }
        viewModelScope.launch {
            _uiState.update { it.copy(isSaving = true, error = null) }
            when (val result = warehouseRepository.updateQrImageUrl(warehouseId, null)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isSaving = false, qrImageUrl = null) }
                    _events.trySend("Đã xoá mã QR")
                }
                is ApiResult.Error -> _uiState.update {
                    it.copy(isSaving = false, error = result.message)
                }
                ApiResult.Loading -> Unit
            }
        }
    }

    fun refreshVerification() {
        viewModelScope.launch {
            val verified = runCatching { authRepository.reloadAndCheckVerified() }.getOrDefault(false)
            _uiState.update { it.copy(isEmailVerified = verified) }
            if (!verified) {
                _events.trySend("Vui lòng xác minh email trước khi cấu hình QR")
            }
        }
    }
}
