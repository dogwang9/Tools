package com.example.lib.utils

object FileUtils {

    /**
     * 在文件名的最后 拼接上时间戳 保证唯一性
     */
    fun appendTimestampToFilename(filename: String): String {
        val dotIndex = filename.lastIndexOf('.')
        val timestamp = System.currentTimeMillis()

        return if (dotIndex != -1) {
            val name = filename.substring(0, dotIndex)
            val extension = filename.substring(dotIndex)
            "${name}_$timestamp$extension"
        } else {
            "${filename}_$timestamp"
        }
    }
}