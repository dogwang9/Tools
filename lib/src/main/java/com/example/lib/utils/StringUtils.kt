package com.example.lib.utils

import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object StringUtils {

    // 把bytes转化为kb/mb/gb
    fun getHumanFriendlyByteCount(bytes: Long, decimalPlaces: Int): String {
        val unit = 1024L
        if (bytes == 0L) {
            return "0 KB"
        }
        if (bytes < unit) {
            return "$bytes B"
        }
        val exp = (ln(bytes.toDouble()) / ln(unit.toDouble())).toInt()
        val pre = "KMGTPE"[exp - 1]
        return String.format(
            Locale.getDefault(), "%.${decimalPlaces}f %sB", bytes / unit.toDouble()
                .pow(exp.toDouble()), pre
        )
    }
}