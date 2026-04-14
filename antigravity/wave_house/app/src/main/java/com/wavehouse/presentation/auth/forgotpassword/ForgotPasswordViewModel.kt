package com.wavehouse.presentation.auth.forgotpassword

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ForgotPasswordUiState(
    val email: String = "",
    val emailError: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    // Navigation flag: non-null email = navigate to success screen
    val navigateToSuccess: String? = null,
    // Countdown for resend (seconds remaining, 0 = button active)
    val resendCountdown: Int = 0
)

@HiltViewModel
class ForgotPasswordViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ForgotPasswordUiState())
    val uiState = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun onEmailChange(email: String) {
        _uiState.update { it.copy(email = email, emailError = validateEmail(email)) }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    fun clearNavigationFlag() = _uiState.update { it.copy(navigateToSuccess = null) }

    fun sendResetEmail() {
        val state = _uiState.value
        val emailError = validateEmail(state.email)
        if (emailError != null) {
            _uiState.update { it.copy(emailError = emailError) }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            when (val result = authRepository.sendPasswordResetEmail(state.email.trim())) {
                is ApiResult.Success -> {
                    _uiState.update {
                        it.copy(isLoading = false, navigateToSuccess = state.email.trim())
                    }
                }
                is ApiResult.Error -> {
                    // Generic error — don't reveal if email exists
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            errorMessage = "Không thể gửi email. Vui lòng thử lại."
                        )
                    }
                }
                ApiResult.Loading -> {}
            }
        }
    }

    /** Called from success screen to resend */
    fun resendEmail(email: String) {
        if (_uiState.value.resendCountdown > 0) return

        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }
            authRepository.sendPasswordResetEmail(email)
            _uiState.update { it.copy(isLoading = false) }
            startCountdown()
        }
    }

    fun startCountdown() {
        countdownJob?.cancel()
        countdownJob = viewModelScope.launch {
            for (seconds in 30 downTo 1) {
                _uiState.update { it.copy(resendCountdown = seconds) }
                delay(1_000)
            }
            _uiState.update { it.copy(resendCountdown = 0) }
        }
    }

    private fun validateEmail(email: String): String? {
        if (email.isBlank()) return "Email không được để trống"
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return "Email không hợp lệ"
        return null
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
