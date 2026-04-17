package com.wavehouse.presentation.auth.splash

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.domain.model.UserStatus
import com.wavehouse.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.tasks.await
import javax.inject.Inject

data class PendingUiState(
    val warehouseName: String = "",
    val isChecking: Boolean = false,
    val isApproved: Boolean = false   // true → navigate to Dashboard
)

@HiltViewModel
class PendingApprovalViewModel @Inject constructor(
    private val authRepository: AuthRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(PendingUiState())
    val uiState = _uiState.asStateFlow()

    init { loadWarehouseName() }

    private fun loadWarehouseName() {
        viewModelScope.launch {
            val user = authRepository.getCurrentUser() ?: return@launch
            if (user.warehouseId.isNotBlank()) {
                when (val r = authRepository.getWarehouseJoinCode(user.warehouseId)) {
                    else -> {} // join code không cần ở đây
                }
                // Lấy tên kho từ DB
                val snapshot = com.google.firebase.database.FirebaseDatabase.getInstance()
                    .getReference("warehouses").child(user.warehouseId).child("name")
                    .get().await()
                val name = snapshot.getValue(String::class.java) ?: ""
                _uiState.update { it.copy(warehouseName = name) }
            }
        }
    }

    fun recheckStatus(onApproved: () -> Unit) {
        viewModelScope.launch {
            _uiState.update { it.copy(isChecking = true) }
            val user = authRepository.getCurrentUser()
            _uiState.update { it.copy(isChecking = false) }
            if (user?.status == UserStatus.ACTIVE) {
                onApproved()
            }
        }
    }

    fun logout(onDone: () -> Unit) {
        viewModelScope.launch {
            authRepository.logout()
            onDone()
        }
    }
}
