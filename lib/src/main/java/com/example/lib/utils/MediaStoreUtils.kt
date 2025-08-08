package com.example.lib.utils

import android.content.Context
import android.content.Intent
import android.media.MediaScannerConnection
import android.net.Uri
import androidx.annotation.WorkerThread

object MediaStoreUtils {

    /**
     * 扫描媒体库
     */
    @WorkerThread
    fun scan(context: Context, paths: List<String>) {
        scan(context, *paths.toTypedArray())
    }

    /**
     * 扫描媒体库
     */
    @WorkerThread
    fun scan(
        context: Context,
        vararg paths: String,
        listener: MediaScannerConnection.OnScanCompletedListener? = null
    ) {
        MediaScannerConnection.scanFile(context, paths, null) { path, uri ->
            listener?.onScanCompleted(path, uri)
        }
    }

    /**
     * 扫描媒体库
     */
    @WorkerThread
    fun scan(context: Context, uri: Uri) {
        val intent = Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE)
        intent.data = uri
        context.sendBroadcast(intent)
    }

}