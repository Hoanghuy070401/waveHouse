package com.wavehouse.core.utils

/**
 * Format raw digit string with thousand-dot separator for display.
 * "50000" → "50.000", "1222222" → "1.222.222"
 */
fun formatThousands(raw: String): String {
    val digits = raw.filter { it.isDigit() }
    if (digits.isEmpty()) return ""
    return digits.toLongOrNull()?.let {
        "%,d".format(it).replace(',', '.')
    } ?: digits
}

/** Strip thousand-dot separators to get raw digit string. "1.222.222" → "1222222" */
fun stripFormat(formatted: String): String = formatted.filter { it.isDigit() }
