package com.example.lib.utils

import android.app.Activity
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Context.CLIPBOARD_SERVICE
import android.content.Context.INPUT_METHOD_SERVICE
import android.content.Intent
import android.content.res.Configuration
import android.content.res.Resources
import android.graphics.BitmapFactory
import android.graphics.RectF
import android.location.Geocoder
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.Uri
import android.os.Environment
import android.os.StatFs
import android.view.View
import android.view.inputmethod.InputMethodManager
import android.webkit.MimeTypeMap
import android.widget.ImageView
import androidx.annotation.RequiresPermission
import androidx.annotation.WorkerThread
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.FileProvider
import androidx.core.net.toUri
import androidx.exifinterface.media.ExifInterface
import java.io.File
import java.io.IOException
import java.util.Locale

object AndroidUtils {

    /**
     * 获取屏幕宽度
     */
    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    /**
     * 获取屏幕高度
     */
    fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    /**
     * dp -> px
     */
    fun dpToPx(dp: Int): Int {
        return (Resources.getSystem().displayMetrics.density * dp).toInt()
    }

    /**
     * 获取图片在imageview中的真实显示区域
     */
    fun getVisibleImageRect(imageView: ImageView): RectF? {
        val drawable = imageView.drawable ?: return null
        val drawableRect =
            RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        imageView.imageMatrix.mapRect(drawableRect) // 映射 drawable 到 imageView 的坐标系
        return drawableRect
    }

    /**
     * 根据uri获取图片的宽高
     */
    fun getImageSizeFromUri(context: Context, uri: Uri): Pair<Int, Int>? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                // 先读取尺寸
                val options = BitmapFactory.Options().apply { inJustDecodeBounds = true }
                BitmapFactory.decodeStream(inputStream, null, options)

                // 再读取一次流用于读取 EXIF（因为 decodeStream 会关闭它）
                context.contentResolver.openInputStream(uri)?.use { exifStream ->
                    val exif = ExifInterface(exifStream)
                    val orientation = exif.getAttributeInt(
                        ExifInterface.TAG_ORIENTATION,
                        ExifInterface.ORIENTATION_UNDEFINED
                    )

                    val isRotated = orientation == ExifInterface.ORIENTATION_ROTATE_90 ||
                            orientation == ExifInterface.ORIENTATION_ROTATE_270

                    if (isRotated) {
                        Pair(options.outHeight, options.outWidth)

                    } else {
                        Pair(options.outWidth, options.outHeight)
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
            Pair(0, 0)
        }
    }

    /**
     * 判断啊网络是否可用
     */
    @RequiresPermission("android.permission.ACCESS_NETWORK_STATE")
    fun isNetworkAvailable(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        val network = connectivityManager.activeNetwork ?: return false
        val networkCapabilities =
            connectivityManager.getNetworkCapabilities(network) ?: return false
        return when {
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> true
            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) -> true
            else -> false
        }
    }

    /**
     * 获取粘贴板的内容
     */
    fun getTextFromClipboard(context: Context): String {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val clipData = clipboard.primaryClip

        return if (clipData != null && clipData.itemCount > 0) {
            clipData.getItemAt(0).coerceToText(context).toString()
        } else {
            ""
        }
    }

    /**
     * 清空粘贴板
     */
    fun clearClipboard(context: Context) {
        val clipboard = context.getSystemService(CLIPBOARD_SERVICE) as ClipboardManager
        val emptyClipData = ClipData.newPlainText("", "")
        clipboard.setPrimaryClip(emptyClipData)
    }

    /**
     * 隐藏软键盘
     */
    fun hideKeyboard(context: Context, view: View) {
        val imm = context.getSystemService(INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(view.windowToken, 0)
    }

    /**
     * 判断是否为黑暗模式
     */
    fun isDarkMode(context: Context): Boolean {
        return when (AppCompatDelegate.getDefaultNightMode()) {
            AppCompatDelegate.MODE_NIGHT_NO -> false
            AppCompatDelegate.MODE_NIGHT_YES -> true
            else -> (context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }
    }

    /**
     * 获取内部存储空间总大小
     */
    fun getTotalInternalStorage(): Long {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        return stat.blockCountLong * stat.blockSizeLong
    }

    /**
     * 获取内部可用存储空间大小
     */
    fun getAvailableInternalStorageSize(): Long {
        val path = Environment.getDataDirectory()
        val stat = StatFs(path.path)
        return stat.availableBlocksLong * stat.blockSizeLong
    }

    /**
     * 分享文件
     */
    fun shareFile(context: Context, path: String?) {
        if (path.isNullOrEmpty()) return

        val file = File(path)
        if (!file.exists()) return

        val fileName = file.name
        val dotIndex = fileName.lastIndexOf('.')

        val extension = if (dotIndex != -1) {
            fileName.substring(dotIndex + 1).lowercase(Locale.ROOT)
        } else {
            ""
        }

        val mimeType = MimeTypeMap.getSingleton()
            .getMimeTypeFromExtension(extension) ?: "*/*"

        val uri = FileProvider.getUriForFile(
            context,
            "${context.packageName}.file_provider",
            file
        )

        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = mimeType
            putExtra(Intent.EXTRA_STREAM, uri)
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }

        val chooserIntent = Intent.createChooser(shareIntent, file.name)
        if (context !is Activity) {
            chooserIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }

        context.startActivity(chooserIntent)
    }

    /**
     * 打开网址
     */
    fun openUrl(context: Context, url: String?) {
        val intent = Intent(Intent.ACTION_VIEW)
        intent.data = url?.toUri()
        context.startActivity(intent)
    }


    /**
     * 根据图片的uri获取经纬度信息
     */
    @WorkerThread
    fun getLatLongByUri(context: Context, uri: Uri): DoubleArray? {
        return try {
            context.contentResolver.openInputStream(uri)?.use { inputStream ->
                val exif = ExifInterface(inputStream)
                exif.latLong
            }
        } catch (e: IOException) {
            null
        }
    }

    /**
     * 根据经纬度获取位置信息
     */
    @WorkerThread
    fun getAddress(context: Context, longitude: Double, latitude: Double): String? {
        try {
            val addresses =
                Geocoder(context, Locale.getDefault()).getFromLocation(latitude, longitude, 1)
            return if (addresses.isNullOrEmpty()) "" else addresses[0].getAddressLine(0)

        } catch (_: IOException) {
            return ""
        }
    }
}