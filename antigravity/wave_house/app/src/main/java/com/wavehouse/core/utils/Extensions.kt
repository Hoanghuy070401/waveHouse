package com.wavehouse.core.utils

import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Format số tiền VND: 1500000 → "1.500.000 ₫" */
fun Long.toVndString(): String {
    val formatter = NumberFormat.getNumberInstance(Locale("vi", "VN"))
    return "${formatter.format(this)} ₫"
}

/** Format số lượng tồn kho */
fun Int.toQuantityString(): String = NumberFormat.getNumberInstance(Locale("vi", "VN")).format(this)

/** Lấy tên rút gọn: "Nguyễn Văn An" → "NVA" */
fun String.toInitials(): String = split(" ")
    .filter { it.isNotBlank() }
    .take(2)
    .joinToString("") { it.first().uppercase() }

/** Rút gọn chuỗi nếu quá dài */
fun String.truncate(maxLength: Int = 30, suffix: String = "..."): String =
    if (length <= maxLength) this else "${take(maxLength - suffix.length)}$suffix"

/** Kiểm tra email hợp lệ */
fun String.isValidEmail(): Boolean =
    android.util.Patterns.EMAIL_ADDRESS.matcher(this).matches()

/** Kiểm tra mật khẩu đủ mạnh (≥ 8 ký tự, có số và chữ) */
fun String.isStrongPassword(): Boolean =
    length >= 8 && any { it.isDigit() } && any { it.isLetter() }

/** Kiểm tra SKU hợp lệ (chỉ số và chữ hoa) */
fun String.isValidSku(): Boolean = matches(Regex("[A-Z0-9\\-]+"))
