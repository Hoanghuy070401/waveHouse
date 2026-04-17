package com.wavehouse.presentation.auth.login

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.repository.AuthRepository
import com.wavehouse.domain.usecase.auth.LoginUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val email: String = "",
    val password: String = "",
    val emailError: String? = null,
    val passwordError: String? = null,
    val isLoading: Boolean = false,
    val isPasswordVisible: Boolean = false,
    val errorMessage: String? = null,
    val loginSuccess: Boolean = false,
    // Email verification flow
    val requiresEmailVerification: Boolean = false,
    val verificationEmail: String = ""
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val loginUseCase: LoginUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState = _uiState.asStateFlow()

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = validateEmail(email)) }
    }

    fun onPasswordChange(password: String) {
        _uiState.update {
            it.copy(
                password = password,
                passwordError = if (password.length < 6) "Mật khẩu tối thiểu 6 ký tự" else null
            )
        }
    }

    fun togglePasswordVisibility() {
        _uiState.update { it.copy(isPasswordVisible = !it.isPasswordVisible) }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun clearVerificationFlag() =
        _uiState.update { it.copy(requiresEmailVerification = false, verificationEmail = "") }

    fun login() {
        val state = _uiState.value
        val emailError = validateEmail(state.email)
        val passwordError = if (state.password.length < 6) "Mật khẩu tối thiểu 6 ký tự" else null
        if (emailError != null || passwordError != null) {
            _uiState.update { it.copy(emailError = emailError, passwordError = passwordError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = loginUseCase(state.email, state.password)) {
                is ApiResult.Success -> {
                    // Reload user and check email verification
                    val isVerified = authRepository.reloadAndCheckVerified()
                    if (isVerified) {
                        _uiState.update { it.copy(isLoading = false, loginSuccess = true) }
                    } else {
                        // Redirect to verification screen — do NOT auto-resend here
                        // (Firebase rate-limits resends; the verification screen has its own resend button)
                        _uiState.update {
                            it.copy(
                                isLoading = false,
                                requiresEmailVerification = true,
                                verificationEmail = state.email
                            )
                        }
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
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return "Email không hợp lệ"
        return null
    }
}
