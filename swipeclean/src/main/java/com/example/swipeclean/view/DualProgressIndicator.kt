package com.example.swipeclean.view

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.util.AttributeSet
import androidx.core.content.ContextCompat
import com.example.tools.R
import com.google.android.material.progressindicator.LinearProgressIndicator

class DualProgressIndicator(context: Context, attrs: AttributeSet) :
    LinearProgressIndicator(context, attrs) {

    private var mSecondaryProgress = 0
    private val mPaint: Paint = Paint()

    init {
        mPaint.setColor(ContextCompat.getColor(context, R.color.blue_main))
        mPaint.style = Paint.Style.FILL
        mPaint.isAntiAlias = true
    }

    override fun setSecondaryProgress(progress: Int) {
        mSecondaryProgress = progress
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        val width = (width * mSecondaryProgress) / max.toFloat()
        val height = height.toFloat()

        canvas.drawRoundRect(0F, 0F, width, height, height / 2f, height / 2f, mPaint)
    }

}