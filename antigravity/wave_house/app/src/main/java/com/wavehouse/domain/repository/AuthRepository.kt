package com.wavehouse.domain.repository

import com.wavehouse.core.network.ApiResult
import com.wavehouse.domain.model.User
import kotlinx.coroutines.flow.Flow

/** Repository interface cho Authentication */
interface AuthRepository {
    /** Đăng ký tài khoản mới: isOwner = true sẽ tạo kho, ngược lại tham gia kho */
    suspend fun register(
        name: String, 
        email: String, 
        password: String,
        isOwner: Boolean,
        warehouseCode: String
    ): ApiResult<User>

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

    /** Xác nhận đặt lại mật khẩu từ oobCode trong deep link */
    suspend fun confirmPasswordReset(oobCode: String, newPassword: String): ApiResult<Unit>

    /** Cập nhật profile */
    suspend fun updateProfile(name: String, avatarUrl: String?): ApiResult<Unit>

    /** Admin cập nhật role của user khác */
    suspend fun updateUserRole(targetUserId: String, newRole: com.wavehouse.domain.model.UserRole): ApiResult<Unit>

    /** Observe realtime user profile (phát hiện thay đổi role ngay lập tức) */
    fun observeCurrentUser(): kotlinx.coroutines.flow.Flow<com.wavehouse.domain.model.User?>

    /** Admin duyệt tài khoản nhân viên PENDING → ACTIVE */
    suspend fun approveStaff(targetUserId: String): ApiResult<Unit>

    /** Lấy mã join của kho hiện tại (chỉ Admin mới gọn đặng dùng) */
    suspend fun getWarehouseJoinCode(warehouseId: String): ApiResult<String>

    /** Lấy danh sách tất cả user trong warehouse (Admin only) */
    suspend fun getAllUsers(): ApiResult<List<com.wavehouse.domain.model.User>>
}
