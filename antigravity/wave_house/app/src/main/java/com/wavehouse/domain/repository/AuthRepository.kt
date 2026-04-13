package com.wavehouse.domain.repository

import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Repository interface cho Authentication */
interface AuthRepository {
    /** Đăng nhập bằng email/password → trả về User */
    suspend fun login(email: String, password: String): ApiResult<User>

    /** Đăng xuất */
    suspend fun logout(): ApiResult<Unit>

    /** Lấy user hiện tại (null nếu chưa đăng nhập) */
    suspend fun getCurrentUser(): User?

    /** Observe trạng thái đăng nhập */
    fun observeAuthState(): Flow<User?>

    /** Đổi mật khẩu */
    suspend fun changePassword(currentPassword: String, newPassword: String): ApiResult<Unit>

    /** Gửi email reset password */
    suspend fun sendPasswordResetEmail(email: String): ApiResult<Unit>

    /** Cập nhật profile */
    suspend fun updateProfile(name: String, avatarUrl: String?): ApiResult<Unit>
}
