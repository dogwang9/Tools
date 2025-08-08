package com.example.downloader.model

import com.example.downloader.business.model.VideoInfo
import com.example.downloader.business.model.YtDlpException

class VideoTask(val videoInfo: VideoInfo, var state: Int = 0, var error: YtDlpException? = null)

class DownloadState(
) {
    companion object {
        const val NOT_DOWNLOAD: Int = 0
        const val DOWNLOADING: Int = 1
        const val DOWNLOADED: Int = 2
        const val DOWNLOAD_FAILED: Int = 3
    }
}