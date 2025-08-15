package com.example.lib.photoview

import android.content.Context
import android.view.MotionEvent
import android.view.ScaleGestureDetector
import android.view.ScaleGestureDetector.OnScaleGestureListener
import android.view.VelocityTracker
import android.view.ViewConfiguration
import java.lang.Float
import kotlin.Boolean
import kotlin.Exception
import kotlin.IllegalArgumentException
import kotlin.Int
import kotlin.let
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

class CustomGestureDetector(context: Context, listener: OnGestureListener) {
    private val INVALID_POINTER_ID: Int = -1

    private var mActivePointerId: Int = INVALID_POINTER_ID
    private var mActivePointerIndex = 0
    private var mVelocityTracker: VelocityTracker? = null
    private var mIsDragging = false
    private var mLastTouchX = 0f
    private var mLastTouchY = 0f
    private var mTouchSlop = 0f
    private var mMinimumVelocity = 0f
    private var mListener: OnGestureListener
    private var mDetector: ScaleGestureDetector

    init {
        val configuration = ViewConfiguration
            .get(context)
        mMinimumVelocity = configuration.scaledMinimumFlingVelocity.toFloat()
        mTouchSlop = configuration.scaledTouchSlop.toFloat()

        mListener = listener
        val mScaleListener: OnScaleGestureListener = object : OnScaleGestureListener {
            private var lastFocusX = 0f
            private var lastFocusY = 0f

            override fun onScale(detector: ScaleGestureDetector): Boolean {
                val scaleFactor = detector.getScaleFactor()

                if (Float.isNaN(scaleFactor) || Float.isInfinite(scaleFactor)) return false

                if (scaleFactor >= 0) {
                    mListener.onScale(
                        scaleFactor,
                        detector.focusX,
                        detector.focusY,
                        detector.focusX - lastFocusX,
                        detector.focusY - lastFocusY
                    )
                    lastFocusX = detector.focusX
                    lastFocusY = detector.focusY
                }
                return true
            }

            override fun onScaleBegin(detector: ScaleGestureDetector): Boolean {
                lastFocusX = detector.focusX
                lastFocusY = detector.focusY
                return true
            }

            override fun onScaleEnd(detector: ScaleGestureDetector) {
                // NO-OP
            }
        }
        mDetector = ScaleGestureDetector(context, mScaleListener)
    }

    private fun getActiveX(ev: MotionEvent): kotlin.Float {
        return try {
            ev.getX(mActivePointerIndex)
        } catch (_: Exception) {
            ev.x
        }
    }

    private fun getActiveY(ev: MotionEvent): kotlin.Float {
        return try {
            ev.getY(mActivePointerIndex)
        } catch (_: Exception) {
            ev.y
        }
    }

    fun isScaling(): Boolean {
        return mDetector.isInProgress
    }

    fun isDragging(): Boolean {
        return mIsDragging
    }

    fun onTouchEvent(ev: MotionEvent): Boolean {
        try {
            mDetector.onTouchEvent(ev)
            return processTouchEvent(ev)
        } catch (_: IllegalArgumentException) {
            // Fix for support lib bug, happening when onDestroy is called
            return true
        }
    }

    private fun processTouchEvent(ev: MotionEvent): Boolean {
        val action = ev.action
        when (action and MotionEvent.ACTION_MASK) {
            MotionEvent.ACTION_DOWN -> {
                mActivePointerId = ev.getPointerId(0)

                mVelocityTracker = VelocityTracker.obtain()
                mVelocityTracker?.addMovement(ev)

                mLastTouchX = getActiveX(ev)
                mLastTouchY = getActiveY(ev)
                mIsDragging = false
            }

            MotionEvent.ACTION_MOVE -> {
                val x = getActiveX(ev)
                val y = getActiveY(ev)
                val dx = x - mLastTouchX
                val dy = y - mLastTouchY

                if (!mIsDragging) {
                    // Use Pythagoras to see if drag length is larger than
                    // touch slop
                    mIsDragging = sqrt(((dx * dx) + (dy * dy)).toDouble()) >= mTouchSlop
                }

                if (mIsDragging) {
                    mListener.onDrag(dx, dy)
                    mLastTouchX = x
                    mLastTouchY = y

                    mVelocityTracker?.addMovement(ev)
                }
            }

            MotionEvent.ACTION_CANCEL -> {
                mActivePointerId = INVALID_POINTER_ID
                // Recycle Velocity Tracker
                mVelocityTracker?.let {
                    it.recycle()
                    mVelocityTracker = null
                }
            }

            MotionEvent.ACTION_UP -> {
                mActivePointerId = INVALID_POINTER_ID
                if (mIsDragging) {
                    mVelocityTracker?.let {
                        mLastTouchX = getActiveX(ev)
                        mLastTouchY = getActiveY(ev)

                        // Compute velocity within the last 1000ms
                        it.addMovement(ev)
                        it.computeCurrentVelocity(1000)

                        val vX = it.xVelocity
                        val vY = it.yVelocity

                        // If the velocity is greater than minVelocity, call
                        // listener
                        if (max(abs(vX), abs(vY)) >= mMinimumVelocity) {
                            mListener.onFling(
                                mLastTouchX, mLastTouchY, -vX,
                                -vY
                            )
                        }
                    }
                }

                // Recycle Velocity Tracker
                mVelocityTracker?.let {
                    it.recycle()
                    mVelocityTracker = null
                }
            }

            MotionEvent.ACTION_POINTER_UP -> {
                val pointerIndex =
                    (ev.action and MotionEvent.ACTION_POINTER_INDEX_MASK) shr MotionEvent.ACTION_POINTER_INDEX_SHIFT
                val pointerId = ev.getPointerId(pointerIndex)
                if (pointerId == mActivePointerId) {
                    // This was our active pointer going up. Choose a new
                    // active pointer and adjust accordingly.
                    val newPointerIndex = if (pointerIndex == 0) 1 else 0
                    mActivePointerId = ev.getPointerId(newPointerIndex)
                    mLastTouchX = ev.getX(newPointerIndex)
                    mLastTouchY = ev.getY(newPointerIndex)
                }
            }
        }

        mActivePointerIndex = ev
            .findPointerIndex(
                if (mActivePointerId != INVALID_POINTER_ID)
                    mActivePointerId
                else
                    0
            )
        return true
    }
}