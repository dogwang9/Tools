package com.example.downloader.business.model

class YtDlpResponse(
    val command: List<String?>,
    val exitCode: Int,
    val elapsedTime: Long,
    val out: String
)

class YtDlpException(
    val title: String,
    val content: String
) : Exception("$title: $content")