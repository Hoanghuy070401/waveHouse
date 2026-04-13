package com.wavehouse.core.network

/**
 * Sealed class representing the result of an API/data operation.
 * Used throughout the entire data → domain → presentation pipeline.
 */
sealed class ApiResult<out T> {

    data class Success<T>(val data: T) : ApiResult<T>()

    data class Error(
        val message: String,
        val code: Int? = null,
        val cause: Throwable? = null
    ) : ApiResult<Nothing>()

    data object Loading : ApiResult<Nothing>()

    /** Returns true if this is a [Success] */
    val isSuccess: Boolean get() = this is Success

    /** Returns true if this is an [Error] */
    val isError: Boolean get() = this is Error

    /** Returns true if this is [Loading] */
    val isLoading: Boolean get() = this is Loading

    /** Returns data if [Success], null otherwise */
    fun getOrNull(): T? = (this as? Success)?.data

    /** Returns error message if [Error], null otherwise */
    fun errorMessage(): String? = (this as? Error)?.message
}

/** Map the data inside [ApiResult.Success] */
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Error -> this
    is ApiResult.Loading -> ApiResult.Loading
}

/** Execute a block if [ApiResult.Success] */
inline fun <T> ApiResult<T>.onSuccess(action: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) action(data)
    return this
}

/** Execute a block if [ApiResult.Error] */
inline fun <T> ApiResult<T>.onError(action: (String, Int?) -> Unit): ApiResult<T> {
    if (this is ApiResult.Error) action(message, code)
    return this
}

/** Safely wrap a suspend call in [ApiResult] */
suspend fun <T> safeApiCall(call: suspend () -> T): ApiResult<T> = try {
    ApiResult.Success(call())
} catch (e: Exception) {
    ApiResult.Error(
        message = e.message ?: "Đã xảy ra lỗi không xác định",
        cause = e
    )
}
