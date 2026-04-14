package com.wavehouse.presentation.auth.emailverification

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

data class EmailVerificationUiState(
    val email: String = "",
    val isChecking: Boolean = false,
    val isResending: Boolean = false,
    val resendCountdown: Int = 0,       // 0 = can resend; >0 = seconds left
    val errorMessage: String? = null,
    val verifiedSuccess: Boolean = false
)

@HiltViewModel
class EmailVerificationViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EmailVerificationUiState())
    val uiState = _uiState.asStateFlow()

    private var countdownJob: Job? = null

    fun setEmail(email: String) {
        _uiState.update { it.copy(email = email) }
        startCountdown()  // first countdown on screen open
    }

    /** "Xác nhận" — reload Firebase user and check isEmailVerified */
    fun checkVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true, errorMessage = null) }
            val verified = authRepository.reloadAndCheckVerified()
            if (verified) {
                _uiState.update { it.copy(isChecking = false, verifiedSuccess = true) }
            } else {
                _uiState.update {
                    it.copy(
                        isChecking = false,
                        errorMessage = "Email chưa được xác minh. Vui lòng kiểm tra hộp thư."
                    )
                }
            }
        }
    }

    /** "Gửi lại" — resend verification email and restart countdown */
    fun resendVerification() {
        viewModelScope.launch {
            _uiState.update { it.copy(isResending = true, errorMessage = null) }
            when (val result = authRepository.sendEmailVerification()) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(isResending = false) }
                    startCountdown()
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isResending = false, errorMessage = result.message) }
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }

    private fun startCountdown(seconds: Int = 30) {
        countdownJob?.cancel()
        _uiState.update { it.copy(resendCountdown = seconds) }
        countdownJob = viewModelScope.launch {
            repeat(seconds) { elapsed ->
                delay(1_000)
                _uiState.update { it.copy(resendCountdown = (seconds - elapsed - 1).coerceAtLeast(0)) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        countdownJob?.cancel()
    }
}
