package com.example.lib.photoview

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Matrix
import android.graphics.RectF
import android.graphics.drawable.Drawable
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.View
import android.view.View.OnLayoutChangeListener
import android.view.View.OnLongClickListener
import android.view.View.OnTouchListener
import android.view.animation.AccelerateDecelerateInterpolator
import android.view.animation.Interpolator
import android.widget.ImageView
import android.widget.ImageView.ScaleType
import android.widget.OverScroller
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.roundToInt
import kotlin.math.sqrt

@SuppressLint("ClickableViewAccessibility")
class PhotoViewAttacher(val mImageView: ImageView) : OnTouchListener,
    OnLayoutChangeListener {

    private val DEFAULT_MAX_SCALE: Float = 3.0f
    private val DEFAULT_MID_SCALE: Float = 1.75f
    private val DEFAULT_MIN_SCALE: Float = 1.0f
    private val DEFAULT_ZOOM_DURATION: Int = 200
    private val HORIZONTAL_EDGE_NONE: Int = -1
    private val HORIZONTAL_EDGE_LEFT: Int = 0
    private val HORIZONTAL_EDGE_RIGHT: Int = 1
    private val HORIZONTAL_EDGE_BOTH: Int = 2
    private val VERTICAL_EDGE_NONE: Int = -1
    private val VERTICAL_EDGE_TOP: Int = 0
    private val VERTICAL_EDGE_BOTTOM: Int = 1
    private val VERTICAL_EDGE_BOTH: Int = 2
    private val SINGLE_TOUCH: Int = 1

    private var mInterpolator: Interpolator = AccelerateDecelerateInterpolator()
    private var mZoomDuration = DEFAULT_ZOOM_DURATION
    private var mMinScale = DEFAULT_MIN_SCALE
    private var mMidScale = DEFAULT_MID_SCALE
    private var mMaxScale = DEFAULT_MAX_SCALE

    private var mAllowParentInterceptOnEdge = true
    private var mBlockParentIntercept = false

    // Gesture Detectors
    private var mGestureDetector: GestureDetector? = null
    private var mScaleDragDetector: CustomGestureDetector? = null

    // These are set so we don't keep allocating them on the heap
    private val mBaseMatrix = Matrix()
    private val mDrawMatrix = Matrix()
    private val mSuppMatrix = Matrix()
    private val mDisplayRect = RectF()
    private val mMatrixValues = FloatArray(9)

    // Listeners
    private var mMatrixChangeListener: OnMatrixChangedListener? = null
    private var mPhotoTapListener: OnPhotoTapListener? = null
    private var mOutsidePhotoTapListener: OnOutsidePhotoTapListener? = null
    private var mViewTapListener: OnViewTapListener? = null
    private var mOnClickListener: View.OnClickListener? = null
    private var mLongClickListener: OnLongClickListener? = null
    private var mScaleChangeListener: OnScaleChangedListener? = null
    private var mSingleFlingListener: OnSingleFlingListener? = null
    private var mOnViewDragListener: OnViewDragListener? = null

    private var mCurrentFlingRunnable: FlingRunnable? = null
    private var mHorizontalScrollEdge = HORIZONTAL_EDGE_BOTH
    private var mVerticalScrollEdge = VERTICAL_EDGE_BOTH
    private var mBaseRotation = 0f

    private var mZoomEnabled = true
    private var mScaleType = ScaleType.FIT_CENTER

    private var onGestureListener: OnGestureListener? = null

    init {
        mImageView.setOnTouchListener(this)
        mImageView.addOnLayoutChangeListener(this)
        if (!mImageView.isInEditMode) {
            mBaseRotation = 0.0f
            // Create Gesture Detectors...
            onGestureListener = object : OnGestureListener {
                override fun onDrag(dx: Float, dy: Float) {
                    if (mScaleDragDetector!!.isScaling()) {
                        return  // Do not drag if we are already scaling
                    }
                    if (mOnViewDragListener != null) {
                        mOnViewDragListener!!.onDrag(dx, dy)
                    }
                    mSuppMatrix.postTranslate(dx, dy)
                    checkAndDisplayMatrix()

                    /*
                     * Here we decide whether to let the ImageView's parent to start taking
                     * over the touch event.
                     *
                     * First we check whether this function is enabled. We never want the
                     * parent to take over if we're scaling. We then check the edge we're
                     * on, and the direction of the scroll (i.e. if we're pulling against
                     * the edge, aka 'overscrolling', let the parent take over).
                     */
                    val parent = mImageView.parent
                    if (mAllowParentInterceptOnEdge && !mScaleDragDetector!!.isScaling() && !mBlockParentIntercept) {
                        if (mHorizontalScrollEdge == HORIZONTAL_EDGE_BOTH || (mHorizontalScrollEdge == HORIZONTAL_EDGE_LEFT && dx >= 1f)
                            || (mHorizontalScrollEdge == HORIZONTAL_EDGE_RIGHT && dx <= -1f)
                            || (mVerticalScrollEdge == VERTICAL_EDGE_TOP && dy >= 1f)
                            || (mVerticalScrollEdge == VERTICAL_EDGE_BOTTOM && dy <= -1f)
                        ) {
                            parent?.requestDisallowInterceptTouchEvent(false)
                        }
                    } else {
                        parent?.requestDisallowInterceptTouchEvent(true)
                    }
                }

                override fun onFling(
                    startX: Float,
                    startY: Float,
                    velocityX: Float,
                    velocityY: Float
                ) {
                    mCurrentFlingRunnable = FlingRunnable(mImageView.context)
                    mCurrentFlingRunnable?.fling(
                        getImageViewWidth(mImageView),
                        getImageViewHeight(mImageView), velocityX.toInt(), velocityY.toInt()
                    )
                    mImageView.post(mCurrentFlingRunnable)
                }

                override fun onScale(scaleFactor: Float, focusX: Float, focusY: Float) {
                    onScale(scaleFactor, focusX, focusY, 0f, 0f)
                }

                override fun onScale(
                    scaleFactor: Float,
                    focusX: Float,
                    focusY: Float,
                    dx: Float,
                    dy: Float
                ) {
                    if (getScale() < mMaxScale || scaleFactor < 1f) {
                        if (mScaleChangeListener != null) {
                            mScaleChangeListener!!.onScaleChange(scaleFactor, focusX, focusY)
                        }
                        mSuppMatrix.postScale(scaleFactor, scaleFactor, focusX, focusY)
                        mSuppMatrix.postTranslate(dx, dy)
                        checkAndDisplayMatrix()
                    }
                }
            }
            mScaleDragDetector =
                CustomGestureDetector(mImageView.context, onGestureListener!!)
            mGestureDetector =
                GestureDetector(mImageView.context, object : SimpleOnGestureListener() {
                    // forward long click listener
                    override fun onLongPress(e: MotionEvent) {
                        if (mLongClickListener != null) {
                            mLongClickListener!!.onLongClick(mImageView)
                        }
                    }

                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        if (mSingleFlingListener != null) {
                            if (getScale() > DEFAULT_MIN_SCALE) {
                                return false
                            }
                            if (e1?.pointerCount!! > SINGLE_TOUCH
                                || e2.pointerCount > SINGLE_TOUCH
                            ) {
                                return false
                            }
                            return mSingleFlingListener!!.onFling(e1, e2, velocityX, velocityY)
                        }
                        return false
                    }
                })
            mGestureDetector?.setOnDoubleTapListener(object : GestureDetector.OnDoubleTapListener {
                override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
                    if (mOnClickListener != null) {
                        mOnClickListener!!.onClick(mImageView)
                    }
                    val displayRect: RectF? = getDisplayRect()
                    val x = e.x
                    val y = e.y
                    if (mViewTapListener != null) {
                        mViewTapListener!!.onViewTap(mImageView, x, y)
                    }
                    if (displayRect != null) {
                        // Check to see if the user tapped on the photo
                        if (displayRect.contains(x, y)) {
                            val xResult = ((x - displayRect.left)
                                    / displayRect.width())
                            val yResult = ((y - displayRect.top)
                                    / displayRect.height())
                            if (mPhotoTapListener != null) {
                                mPhotoTapListener!!.onPhotoTap(mImageView, xResult, yResult)
                            }
                            return true
                        } else {
                            if (mOutsidePhotoTapListener != null) {
                                mOutsidePhotoTapListener!!.onOutsidePhotoTap(mImageView)
                            }
                        }
                    }
                    return false
                }

                override fun onDoubleTap(ev: MotionEvent): Boolean {
                    try {
                        val scale: Float = getScale()
                        val x = ev.x
                        val y = ev.y
                        if (scale < getMediumScale()) {
                            setScale(getMediumScale(), x, y, true)
                        } else if (scale >= getMediumScale() && scale < getMaximumScale()) {
                            setScale(getMaximumScale(), x, y, true)
                        } else {
                            setScale(getMinimumScale(), x, y, true)
                        }
                    } catch (e: ArrayIndexOutOfBoundsException) {
                        // Can sometimes happen when getX() and getY() is called
                    }
                    return true
                }

                override fun onDoubleTapEvent(e: MotionEvent): Boolean {
                    // Wait for the confirmed onDoubleTap() instead
                    return false
                }
            })
        }
    }

    fun setOnDoubleTapListener(newOnDoubleTapListener: GestureDetector.OnDoubleTapListener?) {
        this.mGestureDetector!!.setOnDoubleTapListener(newOnDoubleTapListener)
    }

    fun setOnScaleChangeListener(onScaleChangeListener: OnScaleChangedListener?) {
        this.mScaleChangeListener = onScaleChangeListener
    }

    fun setOnSingleFlingListener(onSingleFlingListener: OnSingleFlingListener?) {
        this.mSingleFlingListener = onSingleFlingListener
    }

    @Deprecated("")
    fun isZoomEnabled(): Boolean {
        return mZoomEnabled
    }

    fun getDisplayRect(): RectF? {
        checkMatrixBounds()
        return getDisplayRect(getDrawMatrix())
    }

    fun setDisplayMatrix(finalMatrix: Matrix): Boolean {
        requireNotNull(finalMatrix) { "Matrix cannot be null" }
        if (mImageView!!.getDrawable() == null) {
            return false
        }
        mSuppMatrix.set(finalMatrix)
        checkAndDisplayMatrix()
        return true
    }

    fun setBaseRotation(degrees: Float) {
        mBaseRotation = degrees % 360
        update()
        setRotationBy(mBaseRotation)
        checkAndDisplayMatrix()
    }

    fun setRotationTo(degrees: Float) {
        mSuppMatrix.setRotate(degrees % 360)
        checkAndDisplayMatrix()
    }

    fun setRotationBy(degrees: Float) {
        mSuppMatrix.postRotate(degrees % 360)
        checkAndDisplayMatrix()
    }

    fun getMinimumScale(): Float {
        return mMinScale
    }

    fun getMediumScale(): Float {
        return mMidScale
    }

    fun getMaximumScale(): Float {
        return mMaxScale
    }

    public fun getScale(): Float {
        return sqrt(
            (getValue(mSuppMatrix, Matrix.MSCALE_X).toDouble().pow(2.0).toFloat() + getValue(
                mSuppMatrix,
                Matrix.MSKEW_Y
            ).toDouble().pow(2.0).toFloat()).toDouble()
        ).toFloat()
    }

    fun getScaleType(): ScaleType {
        return mScaleType
    }

    override fun onLayoutChange(
        v: View?,
        left: Int,
        top: Int,
        right: Int,
        bottom: Int,
        oldLeft: Int,
        oldTop: Int,
        oldRight: Int,
        oldBottom: Int
    ) {
        // Update our base matrix, as the bounds have changed
        if (left != oldLeft || top != oldTop || right != oldRight || bottom != oldBottom) {
            updateBaseMatrix(mImageView.getDrawable())
        }
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View, ev: MotionEvent): Boolean {
        var handled = false
        if (mZoomEnabled && (v as ImageView).getDrawable() != null) {
            when (ev.action) {
                MotionEvent.ACTION_DOWN -> {
                    val parent = v.parent
                    // First, disable the Parent from intercepting the touch
                    // event
                    parent?.requestDisallowInterceptTouchEvent(true)
                    // If we're flinging, and the user presses down, cancel
                    // fling
                    cancelFling()
                }

                MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP ->                     // If the user has zoomed less than min scale, zoom back
                    // to min scale
                    if (getScale() < mMinScale) {
                        val rect = getDisplayRect()
                        if (rect != null) {
                            v.post(
                                AnimatedZoomRunnable(
                                    getScale(), mMinScale,
                                    rect.centerX(), rect.centerY()
                                )
                            )
                            handled = true
                        }
                    } else if (getScale() > mMaxScale) {
                        val rect = getDisplayRect()
                        if (rect != null) {
                            v.post(
                                AnimatedZoomRunnable(
                                    getScale(), mMaxScale,
                                    rect.centerX(), rect.centerY()
                                )
                            )
                            handled = true
                        }
                    }
            }
            // Try the Scale/Drag detector
            mScaleDragDetector?.let {
                val wasScaling = it.isScaling()
                val wasDragging = it.isDragging()
                handled = it.onTouchEvent(ev)
                val didntScale = !wasScaling && !it.isScaling()
                val didntDrag = !wasDragging && !it.isDragging()
                mBlockParentIntercept = didntScale && didntDrag
            }
            // Check to see if the user double tapped
            mGestureDetector?.let {
                if (it.onTouchEvent(ev)) {
                    handled = true
                }
            }
        }
        return handled
    }

    fun setAllowParentInterceptOnEdge(allow: Boolean) {
        mAllowParentInterceptOnEdge = allow
    }

    fun setMinimumScale(minimumScale: Float) {
        checkZoomLevels(minimumScale, mMidScale, mMaxScale)
        mMinScale = minimumScale
    }

    fun setMediumScale(mediumScale: Float) {
        checkZoomLevels(mMinScale, mediumScale, mMaxScale)
        mMidScale = mediumScale
    }

    fun setMaximumScale(maximumScale: Float) {
        checkZoomLevels(mMinScale, mMidScale, maximumScale)
        mMaxScale = maximumScale
    }

    fun setScaleLevels(minimumScale: Float, mediumScale: Float, maximumScale: Float) {
        checkZoomLevels(minimumScale, mediumScale, maximumScale)
        mMinScale = minimumScale
        mMidScale = mediumScale
        mMaxScale = maximumScale
    }

    fun setOnLongClickListener(listener: OnLongClickListener?) {
        mLongClickListener = listener
    }

    fun setOnClickListener(listener: View.OnClickListener?) {
        mOnClickListener = listener
    }

    fun setOnMatrixChangeListener(listener: OnMatrixChangedListener?) {
        mMatrixChangeListener = listener
    }

    fun setOnPhotoTapListener(listener: OnPhotoTapListener?) {
        mPhotoTapListener = listener
    }

    fun setOnOutsidePhotoTapListener(mOutsidePhotoTapListener: OnOutsidePhotoTapListener?) {
        this.mOutsidePhotoTapListener = mOutsidePhotoTapListener
    }

    fun setOnViewTapListener(listener: OnViewTapListener?) {
        mViewTapListener = listener
    }

    fun setOnViewDragListener(listener: OnViewDragListener?) {
        mOnViewDragListener = listener
    }

    fun setScale(scale: Float) {
        setScale(scale, false)
    }

    fun setScale(scale: Float, animate: Boolean) {
        setScale(
            scale,
            (mImageView.right).toFloat() / 2,
            (mImageView.bottom).toFloat() / 2,
            animate
        )
    }

    fun setScale(
        scale: Float, focalX: Float, focalY: Float,
        animate: Boolean
    ) {
        // Check to see if the scale is within bounds
        require(!(scale < mMinScale || scale > mMaxScale)) { "Scale must be within the range of minScale and maxScale" }
        if (animate) {
            mImageView.post(
                AnimatedZoomRunnable(
                    getScale(), scale,
                    focalX, focalY
                )
            )
        } else {
            mSuppMatrix.setScale(scale, scale, focalX, focalY)
            checkAndDisplayMatrix()
        }
    }

    /**
     * Set the zoom interpolator
     *
     * @param interpolator the zoom interpolator
     */
    fun setZoomInterpolator(interpolator: Interpolator) {
        mInterpolator = interpolator
    }

    fun setScaleType(scaleType: ScaleType) {
        if (isSupportedScaleType(scaleType) && scaleType != mScaleType) {
            mScaleType = scaleType
            update()
        }
    }

    fun isZoomable(): Boolean {
        return mZoomEnabled
    }

    fun setZoomable(zoomable: Boolean) {
        mZoomEnabled = zoomable
        update()
    }

    fun update() {
        if (mZoomEnabled) {
            // Update the base matrix using the current drawable
            updateBaseMatrix(mImageView.getDrawable())
        } else {
            // Reset the Matrix...
            resetMatrix()
        }
    }

    /**
     * Get the display matrix
     *
     * @param matrix target matrix to copy to
     */
    fun getDisplayMatrix(matrix: Matrix) {
        matrix.set(getDrawMatrix())
    }

    /**
     * Get the current support matrix
     */
    fun getSuppMatrix(matrix: Matrix) {
        matrix.set(mSuppMatrix)
    }

    private fun getDrawMatrix(): Matrix {
        mDrawMatrix.set(mBaseMatrix)
        mDrawMatrix.postConcat(mSuppMatrix)
        return mDrawMatrix
    }

    fun getImageMatrix(): Matrix {
        return mDrawMatrix
    }

    fun setZoomTransitionDuration(milliseconds: Int) {
        this.mZoomDuration = milliseconds
    }

    /**
     * Helper method that 'unpacks' a Matrix and returns the required value
     *
     * @param matrix     Matrix to unpack
     * @param whichValue Which value from Matrix.M* to return
     * @return returned value
     */
    private fun getValue(matrix: Matrix, whichValue: Int): Float {
        matrix.getValues(mMatrixValues)
        return mMatrixValues[whichValue]
    }

    /**
     * Resets the Matrix back to FIT_CENTER, and then displays its contents
     */
    private fun resetMatrix() {
        mSuppMatrix.reset()
        setRotationBy(mBaseRotation)
        setImageViewMatrix(getDrawMatrix())
        checkMatrixBounds()
    }

    private fun setImageViewMatrix(matrix: Matrix) {
        mImageView.setImageMatrix(matrix)
        // Call MatrixChangedListener if needed
        if (mMatrixChangeListener != null) {
            val displayRect = getDisplayRect(matrix)
            if (displayRect != null) {
                mMatrixChangeListener!!.onMatrixChanged(displayRect)
            }
        }
    }

    /**
     * Helper method that simply checks the Matrix, and then displays the result
     */
    private fun checkAndDisplayMatrix() {
        if (checkMatrixBounds()) {
            setImageViewMatrix(getDrawMatrix())
        }
    }

    /**
     * Helper method that maps the supplied Matrix to the current Drawable
     *
     * @param matrix - Matrix to map Drawable against
     * @return RectF - Displayed Rectangle
     */
    private fun getDisplayRect(matrix: Matrix): RectF? {
        val d = mImageView.getDrawable()
        if (d != null) {
            mDisplayRect.set(
                0f, 0f, d.intrinsicWidth.toFloat(),
                d.intrinsicHeight.toFloat()
            )
            matrix.mapRect(mDisplayRect)
            return mDisplayRect
        }
        return null
    }

    /**
     * Calculate Matrix for FIT_CENTER
     *
     * @param drawable - Drawable being displayed
     */
    private fun updateBaseMatrix(drawable: Drawable?) {
        if (drawable == null) {
            return
        }
        val viewWidth = getImageViewWidth(mImageView).toFloat()
        val viewHeight = getImageViewHeight(mImageView).toFloat()
        val drawableWidth = drawable.intrinsicWidth
        val drawableHeight = drawable.intrinsicHeight
        mBaseMatrix.reset()
        val widthScale = viewWidth / drawableWidth
        val heightScale = viewHeight / drawableHeight
        if (mScaleType == ScaleType.CENTER) {
            mBaseMatrix.postTranslate(
                (viewWidth - drawableWidth) / 2f,
                (viewHeight - drawableHeight) / 2f
            )
        } else if (mScaleType == ScaleType.CENTER_CROP) {
            val scale = max(widthScale, heightScale)
            mBaseMatrix.postScale(scale, scale)
            mBaseMatrix.postTranslate(
                (viewWidth - drawableWidth * scale) / 2f,
                (viewHeight - drawableHeight * scale) / 2f
            )
        } else if (mScaleType == ScaleType.CENTER_INSIDE) {
            val scale = min(1.0f, min(widthScale, heightScale))
            mBaseMatrix.postScale(scale, scale)
            mBaseMatrix.postTranslate(
                (viewWidth - drawableWidth * scale) / 2f,
                (viewHeight - drawableHeight * scale) / 2f
            )
        } else {
            var mTempSrc = RectF(0f, 0f, drawableWidth.toFloat(), drawableHeight.toFloat())
            val mTempDst = RectF(0f, 0f, viewWidth, viewHeight)
            if (mBaseRotation.toInt() % 180 != 0) {
                mTempSrc = RectF(0f, 0f, drawableHeight.toFloat(), drawableWidth.toFloat())
            }
            when (mScaleType) {
                ScaleType.FIT_CENTER -> mBaseMatrix.setRectToRect(
                    mTempSrc,
                    mTempDst,
                    Matrix.ScaleToFit.CENTER
                )

                ScaleType.FIT_START -> mBaseMatrix.setRectToRect(
                    mTempSrc,
                    mTempDst,
                    Matrix.ScaleToFit.START
                )

                ScaleType.FIT_END -> mBaseMatrix.setRectToRect(
                    mTempSrc,
                    mTempDst,
                    Matrix.ScaleToFit.END
                )

                ScaleType.FIT_XY -> mBaseMatrix.setRectToRect(
                    mTempSrc,
                    mTempDst,
                    Matrix.ScaleToFit.FILL
                )

                else -> {}
            }
        }
        resetMatrix()
    }

    private fun checkMatrixBounds(): Boolean {
        val rect = getDisplayRect(getDrawMatrix())
        if (rect == null) {
            return false
        }
        val height = rect.height()
        val width = rect.width()
        var deltaX = 0f
        var deltaY = 0f
        val viewHeight = getImageViewHeight(mImageView)
        if (height <= viewHeight) {
            when (mScaleType) {
                ScaleType.FIT_START -> deltaY = -rect.top
                ScaleType.FIT_END -> deltaY = viewHeight - height - rect.top
                else -> deltaY = (viewHeight - height) / 2 - rect.top
            }
            mVerticalScrollEdge = VERTICAL_EDGE_BOTH
        } else if (rect.top > 0) {
            mVerticalScrollEdge = VERTICAL_EDGE_TOP
            deltaY = -rect.top
        } else if (rect.bottom < viewHeight) {
            mVerticalScrollEdge = VERTICAL_EDGE_BOTTOM
            deltaY = viewHeight - rect.bottom
        } else {
            mVerticalScrollEdge = VERTICAL_EDGE_NONE
        }
        val viewWidth = getImageViewWidth(mImageView)
        if (width <= viewWidth) {
            when (mScaleType) {
                ScaleType.FIT_START -> deltaX = -rect.left
                ScaleType.FIT_END -> deltaX = viewWidth - width - rect.left
                else -> deltaX = (viewWidth - width) / 2 - rect.left
            }
            mHorizontalScrollEdge = HORIZONTAL_EDGE_BOTH
        } else if (rect.left > 0) {
            mHorizontalScrollEdge = HORIZONTAL_EDGE_LEFT
            deltaX = -rect.left
        } else if (rect.right < viewWidth) {
            deltaX = viewWidth - rect.right
            mHorizontalScrollEdge = HORIZONTAL_EDGE_RIGHT
        } else {
            mHorizontalScrollEdge = HORIZONTAL_EDGE_NONE
        }
        // Finally actually translate the matrix
        mSuppMatrix.postTranslate(deltaX, deltaY)
        return true
    }

    private fun getImageViewWidth(imageView: ImageView): Int {
        return imageView.width - imageView.getPaddingLeft() - imageView.getPaddingRight()
    }

    private fun getImageViewHeight(imageView: ImageView): Int {
        return imageView.height - imageView.paddingTop - imageView.paddingBottom
    }

    private fun cancelFling() {
        if (mCurrentFlingRunnable != null) {
            mCurrentFlingRunnable!!.cancelFling()
            mCurrentFlingRunnable = null
        }
    }

    private fun isSupportedScaleType(scaleType: ScaleType?): Boolean {
        if (scaleType == null) {
            return false
        }
        check(scaleType != ScaleType.MATRIX) { "Matrix scale type is not supported" }
        return true
    }

    private fun checkZoomLevels(
        minZoom: Float, midZoom: Float,
        maxZoom: Float
    ) {
        require(!(minZoom >= midZoom)) { "Minimum zoom has to be less than Medium zoom. Call setMinimumZoom() with a more appropriate value" }
        require(!(midZoom >= maxZoom)) { "Medium zoom has to be less than Maximum zoom. Call setMaximumZoom() with a more appropriate value" }
    }

    inner class AnimatedZoomRunnable(
        private val mZoomStart: Float, private val mZoomEnd: Float,
        private val mFocalX: Float, private val mFocalY: Float
    ) : Runnable {
        private val mStartTime: Long = System.currentTimeMillis()

        override fun run() {
            val t = interpolate()
            val scale = mZoomStart + t * (mZoomEnd - mZoomStart)
            val deltaScale: Float = scale / getScale()
            onGestureListener?.onScale(deltaScale, mFocalX, mFocalY)
            // We haven't hit our target scale yet, so post ourselves again
            if (t < 1f) {
                mImageView.postOnAnimation(this)
            }
        }

        fun interpolate(): Float {
            var t: Float = 1f * (System.currentTimeMillis() - mStartTime) / mZoomDuration
            t = min(1f, t)
            t = mInterpolator.getInterpolation(t)
            return t
        }
    }

    inner class FlingRunnable(context: Context?) : Runnable {
        private val mScroller: OverScroller = OverScroller(context)
        private var mCurrentX = 0
        private var mCurrentY = 0

        fun cancelFling() {
            mScroller.forceFinished(true)
        }

        fun fling(
            viewWidth: Int, viewHeight: Int, velocityX: Int,
            velocityY: Int
        ) {
            val rect: RectF? = getDisplayRect()
            if (rect == null) {
                return
            }
            val startX = (-rect.left).roundToInt()
            val minX: Int
            val maxX: Int
            val minY: Int
            val maxY: Int
            if (viewWidth < rect.width()) {
                minX = 0
                maxX = (rect.width() - viewWidth).roundToInt()
            } else {
                maxX = startX
                minX = maxX
            }
            val startY = (-rect.top).roundToInt()
            if (viewHeight < rect.height()) {
                minY = 0
                maxY = (rect.height() - viewHeight).roundToInt()
            } else {
                maxY = startY
                minY = maxY
            }
            mCurrentX = startX
            mCurrentY = startY
            // If we actually can move, fling the scroller
            if (startX != maxX || startY != maxY) {
                mScroller.fling(
                    startX, startY, velocityX, velocityY, minX,
                    maxX, minY, maxY, 0, 0
                )
            }
        }

        override fun run() {
            if (mScroller.isFinished) {
                return  // remaining post that should not be handled
            }
            if (mScroller.computeScrollOffset()) {
                val newX = mScroller.currX
                val newY = mScroller.currY
                mSuppMatrix.postTranslate(
                    (mCurrentX - newX).toFloat(),
                    (mCurrentY - newY).toFloat()
                )
                checkAndDisplayMatrix()
                mCurrentX = newX
                mCurrentY = newY
                // Post On animation
                mImageView.postOnAnimation(this)
            }
        }
    }
}