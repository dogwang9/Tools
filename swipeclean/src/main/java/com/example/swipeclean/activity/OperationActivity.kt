package com.example.swipeclean.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Matrix
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AppCompatActivity
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.example.lib.utils.AndroidUtils
import com.example.swipeclean.business.AlbumController
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Photo
import com.example.swipeclean.other.Constants.DOWN_IMAGE_SCALE
import com.example.swipeclean.other.Constants.KEY_INTENT_ALBUM_ID
import com.example.swipeclean.other.Constants.SHOW_TAG_TRANSLATE_X
import com.example.tools.R
import com.google.android.material.button.MaterialButton
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

class OperationActivity : AppCompatActivity() {

    private lateinit var mUpImageContainer: View
    private lateinit var mTrashButton: MaterialButton
    private lateinit var mDownImageView: ImageView
    private lateinit var mUpImageView: ImageView
    private lateinit var mMagnifyImageView: ImageView
    private lateinit var mKeepImageView: ImageView
    private lateinit var mDeleteImageView: ImageView
    private lateinit var mKeepTextView: TextView
    private lateinit var mDeleteTextView: TextView
    private lateinit var mCountTextView: TextView
    private lateinit var mDateTextView: TextView
    private lateinit var mCancelButton: MaterialButton
    private var mShowMagnifyPhotoRunnable: Runnable? = null
    private var mUpImageViewRectF: RectF? = null
    private var mTouchX = -1f
    private var mTouchY = -1f
    private var mIsAnimating = false
    private var mIsOperating = false
    private var mScreenWidth = AndroidUtils.getScreenWidth()
    private var mAlbum: Album? = null

    private enum class OperationType {
        Keep,
        Delete,
        Cancel
    }

    private val mRecycleBinLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { _ ->
        refreshTitle()
        refreshTrashButton()
        setPhotos()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContentView(R.layout.activity_operation)
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            insets
        }
        initView()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (mIsAnimating) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun initView() {
        mCountTextView = findViewById(R.id.tv_count)
        mDateTextView = findViewById(R.id.tv_date)
        mCancelButton = findViewById(R.id.bt_cancel)
        mUpImageContainer = findViewById(R.id.v_up_image)
        mUpImageView = findViewById(R.id.iv_up_image)
        mDownImageView = findViewById(R.id.iv_down_image)
        mKeepTextView = findViewById(R.id.tv_keep)
        mDeleteTextView = findViewById(R.id.tv_delete)
        mKeepImageView = findViewById(R.id.iv_keep)
        mDeleteImageView = findViewById(R.id.iv_delete)
        mTrashButton = findViewById(R.id.bt_trash)
        mMagnifyImageView = findViewById(R.id.iv_magnify_image)

        val params = mMagnifyImageView.layoutParams
        params.width =
            (mScreenWidth / 2 - Resources.getSystem().displayMetrics.density * 20).toInt()
        mMagnifyImageView.layoutParams = params

        mDownImageView.scaleX = DOWN_IMAGE_SCALE
        mDownImageView.scaleY = DOWN_IMAGE_SCALE
        mTrashButton.isEnabled = false

        mAlbum = AlbumController.getAlbums().find { item ->
            item.getId() == intent.getLongExtra(KEY_INTENT_ALBUM_ID, 0)
        }

        refreshTitle()
        refreshTrashButton()
        setPhotos()

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        mUpImageContainer.setOnTouchListener(mOnTouchListener)

        mKeepImageView.setOnClickListener { startKeepPhoto() }

        mDeleteImageView.setOnClickListener { startDeletePhoto() }

        mTrashButton.setOnClickListener { openTrashActivity() }

        mCancelButton.setOnClickListener { startCancelPhoto() }
    }

    private fun setPhotos() {
        mAlbum?.let {
            setPhoto(mUpImageView, it.photos[it.getOperatedIndex()])
            val nextIndex = it.getOperatedIndex() + 1
            if (nextIndex == it.getTotalCount()) {
                mDownImageView.visibility = View.GONE

            } else {
                if (nextIndex < it.photos.size) {
                    setPhoto(mDownImageView, it.photos[nextIndex])
                }
            }
        }
    }

    private fun setPhoto(image: ImageView, photo: Photo) {
        Glide.with(this)
            .load(photo.sourceUri)
            .priority(Priority.HIGH)
            .error(R.drawable.ic_vector_image)
            .diskCacheStrategy(DiskCacheStrategy.ALL)
            .fitCenter()
            .into(image)
    }

    private fun refreshTitle() {
        mAlbum?.let {
            mDateTextView.text = it.formatData
            val completeCount: Int = it.getOperatedIndex()
            val totalCount: Int = it.photos.size
            mCountTextView.text =
                String.format(
                    Locale.getDefault(),
                    "%d/%d",
                    completeCount + 1,
                    totalCount
                )
            mCancelButton.isEnabled = completeCount != 0
        }
    }

    private fun refreshTrashButton() {
        mAlbum?.let {
            val deleteCount: Int =
                it.photos.stream().filter { item -> item.isDelete() }.count().toInt()
            mTrashButton.isEnabled = deleteCount != 0
            mTrashButton.text =
                String.format(
                    Locale.getDefault(),
                    "打开垃圾箱 (%d张照片) ",
                    deleteCount
                )
        }
    }

    private fun openTrashActivity() {
        val intent = Intent(
            this,
            RecycleBinActivity::class.java
        )
        intent.putExtra(KEY_INTENT_ALBUM_ID, mAlbum?.getId())
        mRecycleBinLauncher.launch(intent)
    }

    private fun startKeepPhoto() {
        if (mIsOperating || mAlbum?.isOperated() == true) {
            return
        }
        mKeepTextView.alpha = 1f
        val downScaleXAnimator = ObjectAnimator.ofFloat(
            mDownImageView,
            View.SCALE_X,
            DOWN_IMAGE_SCALE,
            1f
        )
        val downScaleYAnimator = ObjectAnimator.ofFloat(
            mDownImageView,
            View.SCALE_Y,
            DOWN_IMAGE_SCALE,
            1f
        )
        val scaleXAnimator = ObjectAnimator.ofFloat(mKeepImageView, View.SCALE_X, 1f, 1.1f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(mKeepImageView, View.SCALE_Y, 1f, 1.1f, 1f)
        val translateXAnimator =
            ObjectAnimator.ofFloat(mUpImageContainer, View.TRANSLATION_X, mScreenWidth.toFloat())
        val translateYAnimator = ObjectAnimator.ofFloat(mUpImageContainer, View.TRANSLATION_Y, 200f)
        val alphaAnimator = ObjectAnimator.ofFloat(mUpImageContainer, View.ALPHA, 0f)
        val rotateAnimator = ObjectAnimator.ofFloat(mUpImageContainer, View.ROTATION, 20f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            scaleXAnimator,
            scaleYAnimator,
            translateXAnimator,
            alphaAnimator,
            rotateAnimator,
            translateYAnimator,
            downScaleXAnimator,
            downScaleYAnimator
        )
        animatorSet.setDuration(300)

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                mIsAnimating = true
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                mKeepTextView.alpha = 0f
                mDownImageView.scaleX = DOWN_IMAGE_SCALE
                mDownImageView.scaleY = DOWN_IMAGE_SCALE
                doOnCompleted(OperationType.Keep)
                mIsAnimating = false
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                mDownImageView.scaleX = DOWN_IMAGE_SCALE
                mDownImageView.scaleY = DOWN_IMAGE_SCALE
                mIsAnimating = false
            }
        })

        animatorSet.start()
    }

    private fun startDeletePhoto() {
        if (mIsOperating || mAlbum?.isOperated() == true) {
            return
        }
        mDeleteTextView.alpha = 1f
        val downScaleXAnimator = ObjectAnimator.ofFloat(
            mDownImageView,
            View.SCALE_X, DOWN_IMAGE_SCALE,
            1f
        )
        val downScaleYAnimator = ObjectAnimator.ofFloat(
            mDownImageView,
            View.SCALE_Y, DOWN_IMAGE_SCALE,
            1f
        )
        val scaleXAnimator = ObjectAnimator.ofFloat(mDeleteImageView, View.SCALE_X, 1f, 1.1f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(mDeleteImageView, View.SCALE_Y, 1f, 1.1f, 1f)
        val translateXAnimator =
            ObjectAnimator.ofFloat(mUpImageContainer, View.TRANSLATION_X, -mScreenWidth.toFloat())
        val translateYAnimator = ObjectAnimator.ofFloat(mUpImageContainer, View.TRANSLATION_Y, 200f)
        val alphaAnimator = ObjectAnimator.ofFloat(mUpImageContainer, View.ALPHA, 0f)
        val rotateAnimator = ObjectAnimator.ofFloat(mUpImageContainer, View.ROTATION, -20f)
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            scaleXAnimator,
            scaleYAnimator,
            translateXAnimator,
            alphaAnimator,
            rotateAnimator,
            translateYAnimator,
            downScaleXAnimator,
            downScaleYAnimator
        )
        animatorSet.setDuration(300)

        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                mIsAnimating = true
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                mDeleteTextView.alpha = 0f
                mDownImageView.scaleX = DOWN_IMAGE_SCALE
                mDownImageView.scaleY = DOWN_IMAGE_SCALE
                doOnCompleted(OperationType.Delete)
                mIsAnimating = false
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                mDownImageView.scaleX = DOWN_IMAGE_SCALE
                mDownImageView.scaleY = DOWN_IMAGE_SCALE
                mIsAnimating = false
            }
        })

        animatorSet.start()
    }

    private fun startCancelPhoto() {
        if (mIsOperating) {
            return
        }

        var lastPhoto: Photo? = null
        var currentPhoto: Photo? = null

        mAlbum?.apply {
            lastPhoto = photos[getOperatedIndex() - 1]
            currentPhoto = photos[getOperatedIndex()]
        }

        if (lastPhoto == null || currentPhoto == null) {
            return
        }

        mDownImageView.visibility = View.VISIBLE
        setPhoto(mUpImageView, lastPhoto)
        setPhoto(mDownImageView, currentPhoto)

        val scaleXAnimator = ObjectAnimator.ofFloat(
            mDownImageView,
            View.SCALE_X,
            1f,
            DOWN_IMAGE_SCALE
        )
        val scaleYAnimator = ObjectAnimator.ofFloat(
            mDownImageView,
            View.SCALE_Y,
            1f,
            DOWN_IMAGE_SCALE
        )
        val textAlphaAnimator = ObjectAnimator.ofFloat(
            if (lastPhoto.isKeep()) mKeepTextView else mDeleteTextView,
            View.ALPHA,
            1f,
            0f
        )
        val translateXAnimator = ObjectAnimator.ofFloat(
            mUpImageContainer,
            View.TRANSLATION_X,
            (mScreenWidth * (if (lastPhoto.isKeep()) 1 else -1)).toFloat(),
            0f
        )
        val translateYAnimator =
            ObjectAnimator.ofFloat(mUpImageContainer, View.TRANSLATION_Y, 200f, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(mUpImageContainer, View.ALPHA, 0f, 1f)
        val rotateAnimator = ObjectAnimator.ofFloat(
            mUpImageContainer,
            View.ROTATION,
            (20 * (if (lastPhoto.isKeep()) 1 else -1)).toFloat(),
            0f
        )
        val animatorSet = AnimatorSet()
        animatorSet.playTogether(
            translateXAnimator,
            alphaAnimator,
            rotateAnimator,
            translateYAnimator,
            textAlphaAnimator,
            scaleXAnimator,
            scaleYAnimator
        )
        animatorSet.setDuration(300)
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                mIsAnimating = true
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                doOnCompleted(OperationType.Cancel)
                mIsAnimating = false
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                mIsAnimating = false
            }
        })
        animatorSet.start()
    }

    private fun doOnCompleted(operationType: OperationType) {
        var photo: Photo? = null
        mAlbum?.apply {
            photo =
                if (operationType == OperationType.Cancel) photos[getOperatedIndex() - 1] else photos[getOperatedIndex()]
        }

        if (photo == null) {
            return
        }

        when (operationType) {
            OperationType.Cancel -> {
                photo.cancelOperated()
                lifecycleScope.launch(Dispatchers.IO) {
                    AlbumController.cleanCompletedPhoto(photo)
                }
                refreshTrashButton()
            }

            OperationType.Keep -> {
                photo.doKeep()
                lifecycleScope.launch(Dispatchers.IO) {
                    AlbumController.addPhoto(photo)
                }
            }

            OperationType.Delete -> {
                photo.doDelete()
                lifecycleScope.launch(Dispatchers.IO) {
                    AlbumController.addPhoto(photo)
                }
                refreshTrashButton()
            }
        }

        if (operationType == OperationType.Cancel) {
            refreshTitle()
            return
        }

        mAlbum?.takeIf { it.isOperated() }?.let { album ->
            if (album.photos.any { it.isDelete() }) {
                openTrashActivity()
            }
            finish()
            return
        }

        setPhotos()
        refreshTitle()

        mUpImageContainer.translationX = 0f
        mUpImageContainer.translationY = 0f
        mUpImageContainer.alpha = 1f
        mUpImageContainer.rotation = 0f
    }

    private fun showMagnifyImageView(rateX: Float, rateY: Float) {
        val drawable = mUpImageView.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            mMagnifyImageView.visibility = View.VISIBLE
            mMagnifyImageView.setImageBitmap(
                cropFromOriginalBitmap(
                    bitmap,
                    rateX,
                    rateY,
                    (50 * Resources.getSystem().displayMetrics.density).toInt()
                )
            )
        }
    }

    private fun cropFromOriginalBitmap(
        originalBitmap: Bitmap?,
        rateX: Float,
        rateY: Float,
        size: Int
    ): Bitmap? {
        if (originalBitmap == null) {
            return null
        }

        val bitmapWidth = originalBitmap.width
        val bitmapHeight = originalBitmap.height
        val centerX = rateX * bitmapWidth
        val centerY = rateY * bitmapHeight

        val outputBitmap = createBitmap(size, size)

        val srcRect = Rect(
            (centerX - size / 2f).toInt(),  // left
            (centerY - size / 2f).toInt(),  // top
            (centerX + size / 2f).toInt(),  // right
            (centerY + size / 2f).toInt() // bottom
        )

        Canvas(outputBitmap).drawBitmap(
            originalBitmap,
            srcRect,
            Rect(0, 0, size, size),
            Paint(Paint.ANTI_ALIAS_FLAG)
        )

        return outputBitmap
    }

    private fun getDisplayedImageRect(imageView: ImageView): RectF? {
        val drawable = imageView.drawable ?: return null

        val intrinsicWidth = drawable.intrinsicWidth
        val intrinsicHeight = drawable.intrinsicHeight
        val imageViewWidth = imageView.width
        val imageViewHeight = imageView.height

        val matrix = imageView.imageMatrix
        val values = FloatArray(9)
        matrix.getValues(values)

        val scaleX = values[Matrix.MSCALE_X]
        val scaleY = values[Matrix.MSCALE_Y]

        val displayedWidth = intrinsicWidth * scaleX
        val displayedHeight = intrinsicHeight * scaleY

        var translateX = values[Matrix.MTRANS_X]
        var translateY = values[Matrix.MTRANS_Y]

        if (displayedWidth < imageViewWidth) {
            translateX = (imageViewWidth - displayedWidth) / 2
        }

        if (displayedHeight < imageViewHeight) {
            translateY = (imageViewHeight - displayedHeight) / 2
        }

        val left = translateX
        val top = translateY
        val right = translateX + displayedWidth
        val bottom = translateY + displayedHeight

        return RectF(left, top, right, bottom)
    }


    private val mOnTouchListener = OnTouchListener { view: View, motionEvent: MotionEvent ->
        view.performClick()
        //Only first finger interactions are processed
        if (motionEvent.getPointerId(motionEvent.actionIndex) != 0) {
            return@OnTouchListener true
        }

        if (motionEvent.actionMasked == MotionEvent.ACTION_DOWN || motionEvent.actionMasked == MotionEvent.ACTION_POINTER_DOWN) {
            mIsOperating = true
            mTouchX = motionEvent.rawX
            mTouchY = motionEvent.rawY

            val x = motionEvent.x
            val y = motionEvent.y

            if (mMagnifyImageView.translationX == 0f) {
                if (x < mMagnifyImageView.width && y < mMagnifyImageView.height) {
                    mMagnifyImageView.translationX =
                        (mScreenWidth - mMagnifyImageView.width).toFloat()
                }

            } else {
                if (x > mScreenWidth - mMagnifyImageView.width && y < mMagnifyImageView.height) {
                    mMagnifyImageView.translationX = 0f
                }
            }

            mUpImageViewRectF = getDisplayedImageRect(mUpImageView)
            if (mUpImageViewRectF != null && mUpImageViewRectF!!.contains(x, y)) {
                mShowMagnifyPhotoRunnable = Runnable {
                    showMagnifyImageView(
                        (x - mUpImageViewRectF!!.left) / mUpImageViewRectF!!.width(),
                        (y - mUpImageViewRectF!!.top) / mUpImageViewRectF!!.height()
                    )
                }
                mMagnifyImageView.postDelayed(mShowMagnifyPhotoRunnable, 500)
            }

        } else if (motionEvent.actionMasked == MotionEvent.ACTION_MOVE) {
            val translationX = motionEvent.rawX - mTouchX
            val translationY = motionEvent.rawY - mTouchY
            val absTranslationX = abs(translationX.toDouble()).toFloat()
            val isRightSwipe = translationX > 0

            //Ignore tiny translation
            if (absTranslationX < 10 && abs(translationY.toDouble()) < 10) {
                return@OnTouchListener true

            } else {
                //Cancel magnify
                mMagnifyImageView.removeCallbacks(mShowMagnifyPhotoRunnable)
            }

            if (mMagnifyImageView.isVisible) {
                //intercept, do magnify
                val x = motionEvent.x
                val y = motionEvent.y

                if (mMagnifyImageView.translationX == 0f) {
                    if (x < mMagnifyImageView.width && y < mMagnifyImageView.height) {
                        mMagnifyImageView.translationX =
                            (mScreenWidth - mMagnifyImageView.width).toFloat()
                    }

                } else {
                    if (x > mScreenWidth - mMagnifyImageView.width && y < mMagnifyImageView.height) {
                        mMagnifyImageView.translationX = 0f
                    }
                }

                if (mUpImageViewRectF != null && mUpImageViewRectF!!.contains(x, y)) {
                    showMagnifyImageView(
                        (x - mUpImageViewRectF!!.left) / mUpImageViewRectF!!.width(),
                        (y - mUpImageViewRectF!!.top) / mUpImageViewRectF!!.height()
                    )
                }

                return@OnTouchListener true
            }

            view.translationX = translationX
            view.translationY = translationY
            view.rotation = translationX / 40

            mKeepTextView.alpha = if (isRightSwipe) max(
                0.0,
                (translationX / SHOW_TAG_TRANSLATE_X - 0.2f).toDouble()
            ).toFloat() else 0f
            mDeleteTextView.alpha = if (!isRightSwipe) max(
                0.0,
                (-translationX / SHOW_TAG_TRANSLATE_X - 0.2f).toDouble()
            ).toFloat() else 0f

            if (absTranslationX < SHOW_TAG_TRANSLATE_X) {
                val alphaFactor: Float =
                    1 - absTranslationX / SHOW_TAG_TRANSLATE_X
                val scaleFactor: Float =
                    1 + (if (isRightSwipe) translationX else -translationX) / (SHOW_TAG_TRANSLATE_X * 10)

                mDeleteImageView.alpha = if (isRightSwipe) alphaFactor else 1f
                mKeepImageView.alpha = if (!isRightSwipe) alphaFactor else 1f

                if (isRightSwipe) {
                    mKeepImageView.scaleX = scaleFactor
                    mKeepImageView.scaleY = scaleFactor

                } else {
                    mDeleteImageView.scaleX = scaleFactor
                    mDeleteImageView.scaleY = scaleFactor
                }

            } else {
                mDeleteImageView.alpha = if (isRightSwipe) 0f else 1f
                mKeepImageView.alpha = if (isRightSwipe) 1f else 0f
            }

        } else if (motionEvent.actionMasked == MotionEvent.ACTION_UP || motionEvent.actionMasked == MotionEvent.ACTION_CANCEL || motionEvent.actionMasked == MotionEvent.ACTION_POINTER_UP) {
            mIsOperating = false
            if (mShowMagnifyPhotoRunnable != null) {
                mMagnifyImageView.removeCallbacks(mShowMagnifyPhotoRunnable)
            }
            //intercept when magnify
            if (mMagnifyImageView.isVisible) {
                mMagnifyImageView.visibility = View.INVISIBLE
                mMagnifyImageView.translationX = 0f
                return@OnTouchListener true
            }
            val translationX = motionEvent.rawX - mTouchX
            val absTranslationX = abs(translationX.toDouble()).toFloat()
            val isRightSwipe = translationX > 0

            if (absTranslationX >= SHOW_TAG_TRANSLATE_X) {
                val scaleXAnimator = ObjectAnimator.ofFloat<View>(
                    mDownImageView,
                    View.SCALE_X,
                    DOWN_IMAGE_SCALE,
                    1f
                )
                val scaleYAnimator = ObjectAnimator.ofFloat<View>(
                    mDownImageView,
                    View.SCALE_Y,
                    DOWN_IMAGE_SCALE,
                    1f
                )
                val translateXAnimator = ObjectAnimator.ofFloat(
                    mUpImageContainer,
                    View.TRANSLATION_X,
                    (if (isRightSwipe) mScreenWidth else -mScreenWidth).toFloat()
                )
                val alphaAnimator = ObjectAnimator.ofFloat(mUpImageContainer, View.ALPHA, 0f)
                val animatorSet = AnimatorSet()
                animatorSet.playTogether(
                    translateXAnimator,
                    alphaAnimator,
                    scaleXAnimator,
                    scaleYAnimator
                )
                animatorSet.setDuration(200)

                animatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        super.onAnimationStart(animation)
                        mIsAnimating = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        mDownImageView.scaleX = DOWN_IMAGE_SCALE
                        mDownImageView.scaleY = DOWN_IMAGE_SCALE
                        doOnCompleted(if (isRightSwipe) OperationType.Keep else OperationType.Delete)
                        mIsAnimating = false
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        mDownImageView.scaleX = DOWN_IMAGE_SCALE
                        mDownImageView.scaleY = DOWN_IMAGE_SCALE
                        mIsAnimating = false
                    }
                })
                animatorSet.start()

            } else {
                val keepAlphaAnimator = ObjectAnimator.ofFloat(mKeepTextView, View.ALPHA, 0f)
                val deleteAlphaAnimator = ObjectAnimator.ofFloat(mDeleteTextView, View.ALPHA, 0f)
                val translateXAnimator =
                    ObjectAnimator.ofFloat(mUpImageContainer, View.TRANSLATION_X, 0f)
                val translateYAnimator =
                    ObjectAnimator.ofFloat(mUpImageContainer, View.TRANSLATION_Y, 0f)
                val rotationAnimator = ObjectAnimator.ofFloat(mUpImageContainer, View.ROTATION, 0f)
                val animatorSet = AnimatorSet()
                animatorSet.playTogether(
                    translateXAnimator,
                    translateYAnimator,
                    rotationAnimator,
                    keepAlphaAnimator,
                    deleteAlphaAnimator
                )
                animatorSet.setDuration(200)
                animatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        super.onAnimationStart(animation)
                        mIsAnimating = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        mIsAnimating = false
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        mIsAnimating = false
                    }
                })
                animatorSet.start()
            }

            val deleteImageViewAlphaAnimator =
                ObjectAnimator.ofFloat(mDeleteImageView, View.ALPHA, 1f)
            val deleteImageViewScaleXAnimator =
                ObjectAnimator.ofFloat(mDeleteImageView, View.SCALE_X, 1f)
            val deleteImageViewScaleYAnimator =
                ObjectAnimator.ofFloat(mDeleteImageView, View.SCALE_Y, 1f)
            val keepImageViewAlphaAnimator = ObjectAnimator.ofFloat(mKeepImageView, View.ALPHA, 1f)
            val keepImageViewScaleXAnimator =
                ObjectAnimator.ofFloat(mKeepImageView, View.SCALE_X, 1f)
            val keepImageViewScaleYAnimator =
                ObjectAnimator.ofFloat(mKeepImageView, View.SCALE_Y, 1f)
            val animatorSet = AnimatorSet()
            animatorSet.playTogether(
                deleteImageViewAlphaAnimator, deleteImageViewScaleXAnimator,
                deleteImageViewScaleYAnimator, keepImageViewAlphaAnimator,
                keepImageViewScaleXAnimator, keepImageViewScaleYAnimator
            )
            animatorSet.setDuration(200)
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    mIsAnimating = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mKeepTextView.alpha = 0f
                    mDeleteTextView.alpha = 0f
                    mIsAnimating = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    mKeepTextView.alpha = 0f
                    mDeleteTextView.alpha = 0f
                    mIsAnimating = false
                }
            })

            animatorSet.start()

            mTouchX = -1f
            mTouchY = -1f
        }
        true
    }
}