package com.wavehouse.presentation.auth.changepassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ChangePasswordUiState(
    val currentPassword: String = "",
    val newPassword: String = "",
    val confirmPassword: String = "",
    val isCurrentPasswordVisible: Boolean = false,
    val isNewPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val changeSuccess: Boolean = false,
    // field-level errors
    val currentPasswordError: String? = null,
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
)

@HiltViewModel
class ChangePasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ChangePasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onCurrentPasswordChange(value: String) {
        _uiState.update { it.copy(currentPassword = value, currentPasswordError = null) }
    }

    fun onNewPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                newPassword = value,
                newPasswordError = if (value.length < 6 && value.isNotEmpty())
                    "Mật khẩu tối thiểu 6 ký tự" else null
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = if (value != it.newPassword && value.isNotEmpty())
                    "Mật khẩu xác nhận không khớp" else null
            )
        }
    }

    fun toggleCurrentPasswordVisibility() {
        _uiState.update { it.copy(isCurrentPasswordVisible = !it.isCurrentPasswordVisible) }
    }

    fun toggleNewPasswordVisibility() {
        _uiState.update { it.copy(isNewPasswordVisible = !it.isNewPasswordVisible) }
    }

    fun toggleConfirmPasswordVisibility() {
        _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearSuccess() = _uiState.update { it.copy(successMessage = null) }

    fun changePassword() {
        val state = _uiState.value

        // Validate
        val currentErr = if (state.currentPassword.isBlank()) "Vui lòng nhập mật khẩu hiện tại" else null
        val newErr = when {
            state.newPassword.isBlank() -> "Vui lòng nhập mật khẩu mới"
            state.newPassword.length < 6 -> "Mật khẩu tối thiểu 6 ký tự"
            state.newPassword == state.currentPassword -> "Mật khẩu mới không được giống mật khẩu cũ"
            else -> null
        }
        val confirmErr = when {
            state.confirmPassword.isBlank() -> "Vui lòng xác nhận mật khẩu"
            state.confirmPassword != state.newPassword -> "Mật khẩu xác nhận không khớp"
            else -> null
        }

        if (currentErr != null || newErr != null || confirmErr != null) {
            _uiState.update {
                it.copy(
                    currentPasswordError = currentErr,
                    newPasswordError = newErr,
                    confirmPasswordError = confirmErr
                )
            }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.changePassword(state.currentPassword, state.newPassword)) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            changeSuccess = true,
                            successMessage = "Đổi mật khẩu thành công!"
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, errorMessage = result.message) }
                }
                ApiResult.Loading -> {}
            }
        }
    }
}
