package com.wavehouse.presentation.auth.forgotpassword

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

data class NewPasswordUiState(
    val newPassword: String = "",
    val confirmPassword: String = "",
    val newPasswordError: String? = null,
    val confirmPasswordError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val isSuccess: Boolean = false,
    val showNewPassword: Boolean = false,
    val showConfirmPassword: Boolean = false
)

@HiltViewModel
class NewPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(NewPasswordUiState())
    val uiState = _uiState.asStateFlow()

    fun onNewPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                newPassword = value,
                newPasswordError = validatePassword(value),
                confirmPasswordError = if (it.confirmPassword.isNotBlank())
                    validateConfirm(value, it.confirmPassword) else it.confirmPasswordError
            )
        }
    }

    fun onConfirmPasswordChange(value: String) {
        _uiState.update {
            it.copy(
                confirmPassword = value,
                confirmPasswordError = validateConfirm(it.newPassword, value)
            )
        }
    }

    fun toggleShowNewPassword() =
        _uiState.update { it.copy(showNewPassword = !it.showNewPassword) }

    fun toggleShowConfirmPassword() =
        _uiState.update { it.copy(showConfirmPassword = !it.showConfirmPassword) }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun confirmReset(oobCode: String) {
        val state = _uiState.value
        val pwdError = validatePassword(state.newPassword)
        val cfmError = validateConfirm(state.newPassword, state.confirmPassword)

        if (pwdError != null || cfmError != null) {
            _uiState.update { it.copy(newPasswordError = pwdError, confirmPasswordError = cfmError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.confirmPasswordReset(oobCode, state.newPassword)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isLoading = false, isSuccess = true) }
                }
                is ApiResult.Error -> {
                    val msg = when {
                        result.message?.contains("expired", ignoreCase = true) == true ||
                        result.message?.contains("invalid", ignoreCase = true) == true ->
                            "Link đã hết hạn. Vui lòng gửi lại email khôi phục."
                        else -> "Không thể đặt lại mật khẩu. Vui lòng thử lại."
                    }
                    _uiState.update { it.copy(isLoading = false, errorMessage = msg) }
                }
                ApiResult.Loading -> {}
            }
        }
    }

    private fun validatePassword(pwd: String): String? = when {
        pwd.isBlank() -> "Mật khẩu không được để trống"
        pwd.length < 6 -> "Mật khẩu phải có ít nhất 6 ký tự"
        else -> null
    }

    private fun validateConfirm(pwd: String, confirm: String): String? = when {
        confirm.isBlank() -> "Vui lòng xác nhận mật khẩu"
        confirm != pwd -> "Mật khẩu xác nhận không khớp"
        else -> null
    }
}
