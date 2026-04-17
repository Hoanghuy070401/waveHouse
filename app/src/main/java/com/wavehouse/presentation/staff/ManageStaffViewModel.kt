package com.wavehouse.presentation.staff

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.User
import com.wavehouse.domain.model.UserRole
import com.wavehouse.domain.model.UserStatus
import com.wavehouse.domain.repository.AuthRepository
import com.wavehouse.domain.usecase.auth.GetCurrentUserUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

data class ManageStaffUiState(
    val users: List<User> = emptyList(),
    val pendingUsers: List<User> = emptyList(),   // Nhân viên chờ duyệt
    val joinCode: String = "",                      // Mã 6 ký tự để chia sẻ với nhân viên
    val isLoading: Boolean = false,
    val error: String? = null,
    val successMessage: String? = null
)

@HiltViewModel
class ManageStaffViewModel @Inject constructor(
    private val authRepository: AuthRepository,
    private val getCurrentUserUseCase: GetCurrentUserUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(ManageStaffUiState())
    val uiState = _uiState.asStateFlow()

    init {
        loadData()
    }

    fun loadData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true) }

            // Lấy joinCode của kho hiện tại
            val currentUser = getCurrentUserUseCase()
            if (currentUser != null && currentUser.warehouseId.isNotBlank()) {
                when (val codeResult = authRepository.getWarehouseJoinCode(currentUser.warehouseId)) {
                    is ApiResult.Success -> _uiState.update { it.copy(joinCode = codeResult.data) }
                    else -> {}
                }
            }

            // Lấy danh sách nhân viên
            when (val result = authRepository.getAllUsers()) {
                is ApiResult.Success -> {
                    val myWarehouseId = currentUser?.warehouseId ?: ""
                    val allInWarehouse = result.data.filter {
                        it.warehouseId == myWarehouseId && it.role != UserRole.ADMIN
                    }
                    _uiState.update {
                        it.copy(
                            isLoading = false,
                            users = allInWarehouse.filter { u -> u.status == UserStatus.ACTIVE },
                            pendingUsers = allInWarehouse.filter { u -> u.status == UserStatus.PENDING }
                        )
                    }
                }
                is ApiResult.Error -> {
                    _uiState.update { it.copy(isLoading = false, error = result.message) }
                }
                ApiResult.Loading -> {}
            }
        }
    }

    fun updateRole(userId: String, newRole: UserRole) {
        viewModelScope.launch {
            when (val result = authRepository.updateUserRole(userId, newRole)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(successMessage = "Đã cập nhật quyền thành công") }
                    loadData()
                }
                is ApiResult.Error -> _uiState.update { it.copy(error = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    fun approveStaff(userId: String) {
        viewModelScope.launch {
            when (val result = authRepository.approveStaff(userId)) {
                is ApiResult.Success -> {
                    _uiState.update { it.copy(successMessage = "Đã duyệt tài khoản nhân viên") }
                    loadData()
                }
                is ApiResult.Error -> _uiState.update { it.copy(error = result.message) }
                ApiResult.Loading -> {}
            }
        }
    }

    fun clearMessages() = _uiState.update { it.copy(error = null, successMessage = null) }
}
