package com.example.downloader.utils

import android.content.Context
import android.view.MotionEvent
import android.view.animation.LinearInterpolator
import android.widget.OverScroller
import androidx.recyclerview.widget.RecyclerView
import com.example.lib.utils.AndroidUtils

class DragSelectTouchListener(
    private val selectListener: OnDragSelectListener
) : RecyclerView.OnItemTouchListener {

    interface OnDragSelectListener {
        fun onSelectChange(start: Int, end: Int)
    }

    private var isActive = false
    private var inTopSpot = false
    private var inBottomSpot = false
    private var scrollDistance = 0
    private var recyclerView: RecyclerView? = null
    private var scroller: OverScroller? = null
    private var recyclerViewHeight = 0
    private var regionTop = 0
    private var regionBottom = 0
    private var autoScrollSpeed = 16
    private var autoScrollDistance = AndroidUtils.dpToPx(30)
    private var lastStart = RecyclerView.NO_POSITION
    private var lastEnd = RecyclerView.NO_POSITION
    private var start = RecyclerView.NO_POSITION
    private var end = RecyclerView.NO_POSITION

    private val scrollRunnable = object : Runnable {
        override fun run() {
            scroller?.let {
                if (it.computeScrollOffset()) {
                    scrollBy(scrollDistance)
                    recyclerView?.postOnAnimation(this)
                }
            }
        }
    }

    fun setAutoScrollSpeed(speed: Int) {
        autoScrollSpeed = speed
    }

    fun setAutoScrollDistanceInTopAndBottom(distance: Int) {
        autoScrollDistance = distance
    }

    fun startDragSelection(position: Int) {
        isActive = true
        start = position
        end = position
        lastStart = position
        lastEnd = position
    }

    override fun onInterceptTouchEvent(rv: RecyclerView, e: MotionEvent): Boolean {
        if (!isActive || rv.adapter == null || rv.adapter!!.itemCount == 0) return false

        when (e.action) {
            MotionEvent.ACTION_DOWN, MotionEvent.ACTION_POINTER_DOWN -> reset()
        }

        recyclerView = rv
        recyclerViewHeight = rv.height
        regionTop = autoScrollDistance
        regionBottom = recyclerViewHeight - autoScrollDistance
        return true
    }

    fun startAutoScroll() {
        recyclerView?.let {
            initScroller(it.context)
            scroller?.let { s ->
                if (s.isFinished) {
                    it.removeCallbacks(scrollRunnable)
                    s.startScroll(0, s.currY, 0, 5000, 100000)
                    it.postOnAnimation(scrollRunnable)
                }
            }
        }
    }

    private fun initScroller(context: Context) {
        if (scroller == null) {
            scroller = OverScroller(context, LinearInterpolator())
        }
    }

    fun stopAutoScroll() {
        if (scroller != null && !scroller!!.isFinished) {
            recyclerView?.removeCallbacks(scrollRunnable)
            scroller?.abortAnimation()
        }
    }

    override fun onTouchEvent(rv: RecyclerView, e: MotionEvent) {
        if (!isActive) return

        when (e.action) {
            MotionEvent.ACTION_MOVE -> {
                if (!inTopSpot && !inBottomSpot) {
                    updateSelectedRange(rv, e.x, e.y)
                }
                processAutoScroll(e)
            }

            MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_POINTER_UP -> reset()
        }
    }

    private fun updateSelectedRange(rv: RecyclerView, x: Float, y: Float) {
        val child = rv.findChildViewUnder(x, y)
        if (child != null) {
            val position = rv.getChildAdapterPosition(child)
            if (position != RecyclerView.NO_POSITION && end != position) {
                end = position
                notifySelectRangeChange()
            }
        }
    }

    private fun processAutoScroll(event: MotionEvent) {
        val y = event.y.toInt()
        when {
            y <= regionTop -> {
                scrollDistance = -autoScrollSpeed
                if (!inTopSpot) {
                    inTopSpot = true
                    startAutoScroll()
                }
            }

            y >= regionBottom -> {
                scrollDistance = autoScrollSpeed
                if (!inBottomSpot) {
                    inBottomSpot = true
                    startAutoScroll()
                }
            }

            else -> {
                inBottomSpot = false
                inTopSpot = false
                stopAutoScroll()
            }
        }
    }

    private fun notifySelectRangeChange() {
        if (start == RecyclerView.NO_POSITION || end == RecyclerView.NO_POSITION) return

        val newStart = minOf(start, end)
        val newEnd = maxOf(start, end)

        if (lastStart == RecyclerView.NO_POSITION || lastEnd == RecyclerView.NO_POSITION) {
            selectListener.onSelectChange(newStart, newEnd)
        } else {
            if (newStart > lastStart) selectListener.onSelectChange(lastStart, newStart - 1)
            else if (newStart < lastStart) selectListener.onSelectChange(newStart, lastStart - 1)

            if (newEnd > lastEnd) selectListener.onSelectChange(lastEnd + 1, newEnd)
            else if (newEnd < lastEnd) selectListener.onSelectChange(newEnd + 1, lastEnd)
        }

        lastStart = newStart
        lastEnd = newEnd
    }

    private fun reset() {
        isActive = false
        start = RecyclerView.NO_POSITION
        end = RecyclerView.NO_POSITION
        lastStart = RecyclerView.NO_POSITION
        lastEnd = RecyclerView.NO_POSITION
        inTopSpot = false
        inBottomSpot = false
        stopAutoScroll()
    }

    override fun onRequestDisallowInterceptTouchEvent(disallowIntercept: Boolean) {
        // ignored
    }

    private fun scrollBy(distance: Int) {
        recyclerView?.let {
            if (distance > 0) {
                it.scrollBy(0, autoScrollSpeed)
                updateSelectedRange(it, 0f, recyclerViewHeight.toFloat())
            } else {
                it.scrollBy(0, -autoScrollSpeed)
                updateSelectedRange(it, 0f, 0f)
            }
        }
    }
}
