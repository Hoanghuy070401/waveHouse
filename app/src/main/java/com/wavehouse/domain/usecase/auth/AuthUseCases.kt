package com.wavehouse.domain.usecase.auth

import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.User
import com.wavehouse.domain.repository.AuthRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class LoginUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String, password: String): ApiResult<User> {
        if (email.isBlank()) return ApiResult.Error("Email không được để trống")
        if (password.isBlank()) return ApiResult.Error("Mật khẩu không được để trống")
        if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches())
            return ApiResult.Error("Email không hợp lệ")
        return authRepository.login(email.trim(), password)
    }
}

class LogoutUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): ApiResult<Unit> = authRepository.logout()
}

class GetCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): User? = authRepository.getCurrentUser()
}

class ObserveAuthStateUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> = authRepository.observeAuthState()
}

class SendPasswordResetEmailUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(email: String): ApiResult<Unit> {
        if (email.isBlank()) return ApiResult.Error("Email không được để trống")
        return authRepository.sendPasswordResetEmail(email.trim())
    }
}

class ConfirmPasswordResetUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(oobCode: String, newPassword: String): ApiResult<Unit> {
        if (oobCode.isBlank()) return ApiResult.Error("Mã xác thực không hợp lệ")
        if (newPassword.length < 6) return ApiResult.Error("Mật khẩu phải có ít nhất 6 ký tự")
        return authRepository.confirmPasswordReset(oobCode, newPassword)
    }
}

/** Observe thay đổi profile user theo thời gian thực (phát hiện cấp quyền ngay lập tức) */
class ObserveCurrentUserUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    operator fun invoke(): Flow<User?> = authRepository.observeCurrentUser()
}

/** Admin cấp / thay đổi role của nhân viên */
class UpdateUserRoleUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(targetUserId: String, newRole: com.wavehouse.domain.model.UserRole): ApiResult<Unit> {
        if (targetUserId.isBlank()) return ApiResult.Error("UserId không hợp lệ")
        return authRepository.updateUserRole(targetUserId, newRole)
    }
}

/** Lấy danh sách toàn bộ nhân viên trong hệ thống (Admin only) */
class GetAllUsersUseCase @Inject constructor(
    private val authRepository: AuthRepository
) {
    suspend operator fun invoke(): ApiResult<List<User>> = authRepository.getAllUsers()
}

