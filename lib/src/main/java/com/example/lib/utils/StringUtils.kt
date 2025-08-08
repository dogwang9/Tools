package com.example.lib.utils

import android.text.TextUtils
import android.text.format.DateUtils
import android.util.Patterns
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.ln
import kotlin.math.pow

object StringUtils {

    // 把bytes转化为kb/mb/gb
    fun getHumanFriendlyByteCount(bytes: Long, decimalPlaces: Int = 1): String {
        val unit = 1024L
        val units = "KMGTPE"
        val locale = Locale.getDefault()

        if (bytes < unit) return "$bytes B"

        val exp = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
        val prefix = units[exp - 1]
        val result = bytes / unit.toDouble().pow(exp.toDouble())

        return "%.${decimalPlaces}f %sB".format(locale, result, prefix)
    }

    //把时长转化为 00：00的形式
    fun formatDuration(
        seconds: Long,
        withColon: Boolean = true,
        withDay: Boolean = false,
        dayFormat: String? = null
    ): String {
        val dayInSeconds = DateUtils.DAY_IN_MILLIS / DateUtils.SECOND_IN_MILLIS
        val hourInSeconds = DateUtils.HOUR_IN_MILLIS / DateUtils.SECOND_IN_MILLIS

        return if (withDay && seconds >= dayInSeconds) {
            val format = if (withColon) "$dayFormat %02d:%02d:%02d" else "$dayFormat %02d%02d%02d"
            String.format(
                Locale.getDefault(),
                format,
                seconds / (3600 * 24),
                (seconds % (24 * 3600)) / 3600,
                (seconds % 3600) / 60,
                seconds % 60
            )
        } else if (seconds >= hourInSeconds) {
            val format = if (withColon) "%02d:%02d:%02d" else "%02d%02d%02d"
            String.format(
                Locale.getDefault(),
                format,
                seconds / 3600,
                (seconds % 3600) / 60,
                seconds % 60
            )
        } else {
            val format = if (withColon) "%02d:%02d" else "%02d%02d"
            String.format(
                Locale.getDefault(),
                format,
                (seconds % 3600) / 60,
                seconds % 60
            )
        }
    }

    //20250609 -> 根据不同国家显示合适的格式
    fun formatDate(dateStr: String?, locale: Locale = Locale.getDefault()): String {
        if (TextUtils.isEmpty(dateStr)) return ""
        val inputFormat = SimpleDateFormat("yyyyMMdd", Locale.US)
        val date = inputFormat.parse(dateStr!!)

        val localizedFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, locale)
        return localizedFormat.format(date!!)
    }

    //时间戳 -> 根据不同国家显示合适的格式
    fun formatDate(timestamp: Long, locale: Locale = Locale.getDefault()): String {
        val date = Date(timestamp)
        val localizedFormat = SimpleDateFormat.getDateInstance(SimpleDateFormat.LONG, locale)
        return localizedFormat.format(date)
    }

    //判断url是否合法
    fun isValidWebUrl(text: String): Boolean {
        if (text.isEmpty()) {
            return false
        }
        val urlPattern = Patterns.WEB_URL
        return urlPattern.matcher(text).matches()
    }

    //加工url(去除http前面无用的字段)
    fun processUrl(url: String): String {
        return if (url.contains("http")) {
            url.substring(url.indexOf("http"))
        } else {
            url
        }
    }
}