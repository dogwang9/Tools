package com.example.lib.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.result.ActivityResultLauncher
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat

object PermissionUtils {

    // TODO: 处理 “部分访问”
    /**
     * 获取读图片权限
     */
    fun getReadImagePermission(activity: Activity, launcher: ActivityResultLauncher<String>) {
        if (checkReadImagePermission(activity)) {
            return
        }
        val permission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_EXTERNAL_STORAGE
        else
            Manifest.permission.READ_MEDIA_IMAGES

        // 当用户多次点击不在询问(一般是两次),授权界面就不再会出现
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            launcher.launch(permission)

        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        }
    }

    /**
     * 检查读图片权限
     */
    fun checkReadImagePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_EXTERNAL_STORAGE
            else
                Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 获取写文件权限
     */
    fun getWritePermission(activity: Activity, launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q || checkWritePermission(activity)) {
            return
        }

        val permission = Manifest.permission.WRITE_EXTERNAL_STORAGE

        // 当用户多次点击不在询问(一般是两次),授权界面就不再会出现
        if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            launcher.launch(permission)

        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        }
    }

    /**
     * 检查写文件权限
     */
    fun checkWritePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * 检查通知权限
     */
    fun checkNotificationPermission(context: Context): Boolean {
        return NotificationManagerCompat.from(context).areNotificationsEnabled()
    }

    /**
     * 获取通知权限
     */
    fun getNotificationPermission(
        activity: Activity,
        launcher1: ActivityResultLauncher<String>,
        launcher2: ActivityResultLauncher<Intent>
    ) {
        if (checkNotificationPermission(activity)) {
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            val permission = Manifest.permission.POST_NOTIFICATIONS
            // 当用户多次点击不在询问(一般是两次),授权界面就不再会出现
            if (!ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
                launcher1.launch(permission)

            } else {
                getPermissionByIntent(activity, launcher2)
            }

        } else {
            getPermissionByIntent(activity, launcher2)
        }
    }

    private fun getPermissionByIntent(
        activity: Activity,
        launcher: ActivityResultLauncher<Intent>
    ) {
        //api 26之后可以直接跳转到app的通知权限界面，之前只能跳转到app所有权限界面
        val intent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
                putExtra(Settings.EXTRA_APP_PACKAGE, activity.packageName)
            }
        } else {
            Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
        }
        launcher.launch(intent)
    }
}