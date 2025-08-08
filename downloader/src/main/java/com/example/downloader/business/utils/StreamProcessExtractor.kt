package com.example.downloader.business.utils

import android.util.Log
import java.io.IOException
import java.io.InputStream
import java.io.InputStreamReader
import java.io.Reader
import java.nio.charset.StandardCharsets

internal class StreamProcessExtractor(
    private val buffer: StringBuffer,
    private val stream: InputStream,
    private val callback: ((Float, String, String) -> Unit)?
) : Thread() {

    private val TAG = "StreamProcessExtractor"

    init {
        start()
    }

    override fun run() {
        try {
            val input: Reader = InputStreamReader(stream, StandardCharsets.UTF_8)
            val currentLine = StringBuilder()
            var nextChar: Int
            while (input.read().also { nextChar = it } != -1) {
                buffer.append(nextChar.toChar())
                if (nextChar == '\r'.code || nextChar == '\n'.code && callback != null) {
                    val line = currentLine.toString()
                    processOutputLine(line)
                    currentLine.setLength(0)
                    continue
                }
                currentLine.append(nextChar.toChar())
            }
        } catch (e: IOException) {
            Log.e(TAG, "run: $e")
        }
    }

    private fun processOutputLine(line: String) {
        if (line.startsWith("[download]")) {
            val regex = """([\d.]+)%.*?at\s+([\d.]+[KMGT]?iB/s)""".toRegex()

            val matchResult = regex.find(line)
            if (matchResult != null) {
                val (progress, speed) = matchResult.destructured
                callback?.let { it(progress.toFloat(), speed, line) }

            } else {
                callback?.let { it(0f, "", line) }
            }

        } else {
            callback?.let { it(0f, "", line) }
        }
    }

}