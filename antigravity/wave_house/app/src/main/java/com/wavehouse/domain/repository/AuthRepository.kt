package com.wavehouse.domain.repository

import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Repository interface cho Authentication */
interface AuthRepository {
    /** Đăng ký tài khoản mới */
    suspend fun register(name: String, email: String, password: String): ApiResult<User>

    /** Đăng nhập bằng email/password → trả về User */
    suspend fun login(email: String, password: String): ApiResult<User>

    /** Đăng xuất */
    suspend fun logout(): ApiResult<Unit>

    /** Gửi email xác minh đến user hiện tại */
    suspend fun sendEmailVerification(): ApiResult<Unit>

    /** Reload Firebase Auth user và trả về trạng thái isEmailVerified */
    suspend fun reloadAndCheckVerified(): Boolean

    /** Lấy user hiện tại (null nếu chưa đăng nhập) */
    suspend fun getCurrentUser(): User?

    /** Observe trạng thái đăng nhập */
    fun observeAuthState(): Flow<User?>

    /** Đổi mật khẩu */
    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit>

    /** Gửi email reset password */
    suspend fun sendPasswordResetEmail(email: String): ApiResult<Unit>

    /** Xác nhận đặt lại mật khẩu bằng oobCode từ deep link */
    suspend fun confirmPasswordReset(oobCode: String, newPassword: String): ApiResult<Unit>

    /** Cập nhật profile */
    suspend fun updateProfile(name: String, avatarUrl: String?): ApiResult<Unit>
}
