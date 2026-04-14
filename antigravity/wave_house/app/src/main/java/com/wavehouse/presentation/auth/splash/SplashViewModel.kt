package com.wavehouse.presentation.auth.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.domain.repository.AuthRepository
import com.wavehouse.domain.usecase.auth.ObserveAuthStateUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

sealed interface SplashDestination {
    data object Loading : SplashDestination
    data object Unauthenticated : SplashDestination
    data class EmailVerificationRequired(val email: String) : SplashDestination
    data object Dashboard : SplashDestination
}

// Keep old sealed interface for compatibility with existing code
sealed interface SplashUiState {
    data object Loading : SplashUiState
    data object Authenticated : SplashUiState
    data object Unauthenticated : SplashUiState
}

@HiltViewModel
class SplashViewModel @Inject constructor(
    observeAuthStateUseCase: ObserveAuthStateUseCase,
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _destination = MutableStateFlow<SplashDestination>(SplashDestination.Loading)
    val destination = _destination.asStateFlow()

    // Keep for legacy SplashScreen usage
    val authState = observeAuthStateUseCase()
        .map { user -> if (user != null) SplashUiState.Authenticated else SplashUiState.Unauthenticated }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = SplashUiState.Loading
        )

    init {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser()
            if (user == null) {
                _destination.update { SplashDestination.Unauthenticated }
                return@launch
            }
            val verified = authRepository.reloadAndCheckVerified()
            if (verified) {
                _destination.update { SplashDestination.Dashboard }
            } else {
                _destination.update { SplashDestination.EmailVerificationRequired(user.email) }
            }
        }
    }
}
