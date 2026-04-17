package com.wavehouse.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.domain.model.User
import com.wavehouse.domain.model.UserRole
import com.wavehouse.domain.usecase.auth.ObserveCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

/** ViewModel cấp Application — cung cấp currentUser cho toàn bộ NavHost */
@HiltViewModel
class AppViewModel @Inject constructor(
    observeCurrentUserUseCase: ObserveCurrentUserUseCase
) : ViewModel() {

    /** Observe realtime — tự động cập nhật khi Admin thay đổi role */
    val currentUser = observeCurrentUserUseCase()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = null
        )

    val currentUserRole = currentUser
        .map { it?.role ?: UserRole.STAFF }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = UserRole.STAFF
        )
}
