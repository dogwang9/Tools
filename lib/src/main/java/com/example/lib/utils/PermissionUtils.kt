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
import androidx.core.content.ContextCompat

object PermissionUtils {

    fun getReadImagePermission(activity: Activity, launcher: ActivityResultLauncher<String>) {
        if (checkReadImagePermission(activity)) {
            return
        }
        val permission = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
            Manifest.permission.READ_EXTERNAL_STORAGE
        else
            Manifest.permission.READ_MEDIA_IMAGES

        // 当用户多次点击不在询问(一般是两次),授权界面就不再会出现
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            launcher.launch(permission)

        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        }
    }

    fun checkReadImagePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU)
                Manifest.permission.READ_EXTERNAL_STORAGE
            else
                Manifest.permission.READ_MEDIA_IMAGES
        ) == PackageManager.PERMISSION_GRANTED
    }

    fun getWritePermission(activity: Activity, launcher: ActivityResultLauncher<String>) {
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.Q || checkWritePermission(activity)) {
            return
        }

        val permission =  Manifest.permission.WRITE_EXTERNAL_STORAGE

        // 当用户多次点击不在询问(一般是两次),授权界面就不再会出现
        if (ActivityCompat.shouldShowRequestPermissionRationale(activity, permission)) {
            launcher.launch(permission)

        } else {
            val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.fromParts("package", activity.packageName, null)
            }
            activity.startActivity(intent)
        }
    }

    fun checkWritePermission(context: Context): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.WRITE_EXTERNAL_STORAGE
        ) == PackageManager.PERMISSION_GRANTED
    }
}