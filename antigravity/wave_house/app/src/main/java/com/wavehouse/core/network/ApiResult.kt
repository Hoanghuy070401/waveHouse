package com.wavehouse.core.network

import retrofit2.HttpException
import java.net.SocketTimeoutException
import java.net.UnknownHostException

/**
 * Wrapper kết quả API — dùng thống nhất trong toàn bộ app.
 */
sealed class ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>()
    data class Error(val message: String, val code: Int? = null) : ApiResult<Nothing>()
    data object Loading : ApiResult<Nothing>()
}

/** Extension để map data */
fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> this
    ApiResult.Loading -> ApiResult.Loading
}

/** Extension để unwrap hoặc trả null */
fun <T> ApiResult<T>.getOrNull(): T? = if (this is ApiResult.Success) data else null

/**
 * Safe wrapper cho Firebase / suspend calls.
 * Tự động bắt exception và convert → ApiResult.Error với message tiếng Việt.
 */
suspend fun <T> safeApiCall(block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: HttpException) {
        val message = when (e.code()) {
            401 -> "Phiên đăng nhập hết hạn"
            403 -> "Bạn không có quyền thực hiện thao tác này"
            404 -> "Không tìm thấy dữ liệu"
            500 -> "Lỗi máy chủ, vui lòng thử lại sau"
            else -> "Lỗi kết nối (${e.code()})"
        }
        ApiResult.Error(message, e.code())
    } catch (e: UnknownHostException) {
        ApiResult.Error("Không có kết nối mạng")
    } catch (e: SocketTimeoutException) {
        ApiResult.Error("Kết nối quá thời gian, vui lòng thử lại")
    } catch (e: com.google.firebase.FirebaseNetworkException) {
        ApiResult.Error("Không có kết nối mạng")
    } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
        ApiResult.Error("Email hoặc mật khẩu không đúng")
    } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
        ApiResult.Error("Tài khoản không tồn tại hoặc đã bị vô hiệu hóa")
    } catch (e: com.google.firebase.firestore.FirebaseFirestoreException) {
        ApiResult.Error(e.message ?: "Lỗi cơ sở dữ liệu")
    } catch (e: Exception) {
        ApiResult.Error(e.message ?: "Đã xảy ra lỗi không xác định")
    }
}
