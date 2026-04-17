package com.wavehouse.presentation.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.usecase.auth.ConfirmPasswordResetUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class NewPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val navigateToLogin: Boolean = false
)

@HiltViewModel
class NewPasswordViewModel @Inject constructor(
    private val confirmPasswordResetUseCase: ConfirmPasswordResetUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onNewPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                newPassword = value,
                newPasswordError = validatePassword(value),
                // Re-validate confirm if already filled
                confirmPasswordError = if (it.confirmPassword.isNotBlank())
                    validateMatch(value, it.confirmPassword) else it.confirmPasswordError
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = validateMatch(it.newPassword, value)
            )
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun clearNavigationFlag() = _uiState.update { it.copy(navigateToLogin = false) }

    fun confirmPasswordReset(oobCode: String) {
        val state = _uiState.value
        val pwdErr = validatePassword(state.newPassword)
        val matchErr = validateMatch(state.newPassword, state.confirmPassword)

        if (pwdErr != null || matchErr != null) {
            _uiState.update { it.copy(newPasswordError = pwdErr, confirmPasswordError = matchErr) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = confirmPasswordResetUseCase(oobCode, state.newPassword)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, navigateToLogin = true) }
                }
                is ApiResult.Error -> {
                    val msg = if (result.message?.contains("expired", ignoreCase = true) == true ||
                        result.message?.contains("invalid", ignoreCase = true) == true
                    ) {
                        "Link đã hết hạn. Vui lòng gửi lại email."
                    } else {
                        "Đặt mật khẩu thất bại. Vui lòng thử lại."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
                ApiResult.Loading -> {}
            }
        }
    }

    private fun validatePassword(password: String): String? {
        if (password.isBlank()) return "Mật khẩu không được để trống"
        if (password.length < 6) return "Mật khẩu phải có ít nhất 6 ký tự"
        return null
    }

    private fun validateMatch(password: String, confirm: String): String? {
        if (confirm.isBlank()) return "Vui lòng xác nhận mật khẩu"
        if (password != confirm) return "Mật khẩu không khớp"
        return null
    }
}
