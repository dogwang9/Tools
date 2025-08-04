package com.example.lib.utils

import android.content.res.Resources
import android.graphics.RectF
import android.widget.ImageView

object AndroidUtils {

    fun getScreenWidth(): Int {
        return Resources.getSystem().displayMetrics.widthPixels
    }

    fun getScreenHeight(): Int {
        return Resources.getSystem().displayMetrics.heightPixels
    }

    //获取imageview的真实显示区域
    fun getVisibleImageRect(imageView: ImageView): RectF? {
        val drawable = imageView.drawable ?: return null
        val matrix = imageView.imageMatrix
        val drawableRect =
            RectF(0f, 0f, drawable.intrinsicWidth.toFloat(), drawable.intrinsicHeight.toFloat())
        matrix.mapRect(drawableRect) // 映射 drawable 到 imageView 的坐标系
        return drawableRect
    }
}