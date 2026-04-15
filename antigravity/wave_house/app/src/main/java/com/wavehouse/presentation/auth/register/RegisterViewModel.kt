package com.wavehouse.presentation.auth.register

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

data class RegisterUiState(
    val isOwner: Boolean = true,
    val warehouseCode: String = "",
    val name: String = "",
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
    val warehouseCodeError: String? = null,
    val nameError: String? = null,
    val emailError: String? = null,
    val passwordError: String? = null,
    val confirmPasswordError: String? = null,
    val isPasswordVisible: Boolean = false,
    val isConfirmPasswordVisible: Boolean = false,
    val isAgreeTerms: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // After register, navigate to email verification
    val navigateToVerification: Boolean = false,
    val registeredEmail: String = ""
)

@HiltViewModel
class RegisterViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(RegisterUiState())
    val uiState = _uiState.asStateFlow()

    fun onToggleOwner(isOwner: Boolean) = _uiState.update {
        it.copy(isOwner = isOwner, warehouseCode = "", warehouseCodeError = null)
    }

    fun onWarehouseCodeChange(value: String) = _uiState.update {
        it.copy(
            warehouseCode = value,
            warehouseCodeError = if (value.isBlank()) {
                if (it.isOwner) "Vui lòng nhập Tên kho" else "Vui lòng nhập Mã kho"
            } else null
        )
    }

    fun onNameChange(value: String) = _uiState.update {
        it.copy(name = value, nameError = if (value.isBlank()) "Họ và tên không được để trống" else null)
    }

    fun onEmailChange(value: String) = _uiState.update {
        it.copy(email = value, emailError = validateEmail(value))
    }

    fun onPasswordChange(value: String) = _uiState.update {
        it.copy(password = value, passwordError = validatePassword(value))
    }

    fun onConfirmPasswordChange(value: String) = _uiState.update {
        it.copy(
            confirmPassword = value,
            confirmPasswordError = if (value != it.password) "Mật khẩu không khớp" else null
        )
    }

    fun togglePasswordVisibility() = _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    fun toggleConfirmPasswordVisibility() = _uiState.update { it.copy(isConfirmPasswordVisible = !it.isConfirmPasswordVisible) }
    fun onAgreeTermsChange(agreed: Boolean) = _uiState.update { it.copy(isAgreeTerms = agreed) }
    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
    fun clearNavigationFlag() = _uiState.update { it.copy(navigateToVerification = false) }

    fun register() {
        val s = _uiState.value
        val whErr = if (s.warehouseCode.isBlank()) {
            if (s.isOwner) "Vui lòng nhập Tên kho" else "Vui lòng nhập Mã kho"
        } else null
        val nameErr = if (s.name.isBlank()) "Họ và tên không được để trống" else null
        val emailErr = validateEmail(s.email)
        val passErr = validatePassword(s.password)
        val confirmErr = if (s.confirmPassword != s.password) "Mật khẩu không khớp" else null

        if (whErr != null || nameErr != null || emailErr != null || passErr != null || confirmErr != null) {
            _uiState.update {
                it.copy(
                    warehouseCodeError = whErr,
                    nameError = nameErr, emailError = emailErr,
                    passwordError = passErr, confirmPasswordError = confirmErr
                )
            }
            return
        }
        if (!s.isAgreeTerms) {
            _uiState.update { it.copy(errorMessage = "Vui lòng đồng ý với điều khoản dịch vụ") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.register(s.name.trim(), s.email.trim(), s.password, s.isOwner, s.warehouseCode.trim())) {
                is ApiResult.Success -> {
                    // Send email verification immediately after register
                    authRepository.sendEmailVerification()
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            navigateToVerification = true,
                            registeredEmail = s.email.trim()
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

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email không được để trống"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) return "Email không hợp lệ"
        return null
    }

    private fun validatePassword(pwd: String): String? {
        if (pwd.length < 6) return "Mật khẩu tối thiểu 6 ký tự"
        return null
    }
}
