package com.example.swipeclean.activity

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.RectF
import android.graphics.drawable.BitmapDrawable
import android.os.Bundle
import android.view.MotionEvent
import android.view.View
import android.view.View.OnTouchListener
import android.widget.ImageView
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.core.graphics.createBitmap
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.lifecycle.lifecycleScope
import com.bumptech.glide.Glide
import com.bumptech.glide.Priority
import com.example.lib.mvvm.BaseActivity
import com.example.lib.utils.AndroidUtils
import com.example.swipeclean.business.AlbumController
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Image
import com.example.swipeclean.other.Constants.KEY_INTENT_ALBUM_ID
import com.example.tools.R
import com.example.tools.databinding.ActivityOperationBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.util.Locale
import kotlin.math.abs
import kotlin.math.max

class OperationActivity : BaseActivity<ActivityOperationBinding>() {

    companion object {
        const val PHOTO_OPERATION_KEEP = 0
        const val PHOTO_OPERATION_DELETE = 1
        const val PHOTO_OPERATION_CANCEL = 2

        const val AUTO_OPERATION_ANIMATOR_DURATION = 300L
        const val MANUAL_OPERATION_ANIMATOR_DURATION = 200L

        const val SHOW_TAG_TRANSLATE_X: Int = 200
        const val DOWN_IMAGE_SCALE: Float = 0.9f
    }

    private var mShowMagnifyPhotoRunnable: Runnable? = null
    private var mUpImageViewRectF: RectF? = null
    private var mTouchX = -1f
    private var mTouchY = -1f
    private var mIsAnimating = false
    private var mIsOperating = false
    private val mScreenWidth = AndroidUtils.getScreenWidth()
    private var mAlbum: Album? = null

    private val mRecycleBinLauncher = registerForActivityResult(
        StartActivityForResult()
    ) { _ ->
        refreshTitle()
        refreshTrashButton()
        setPhotos()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        initView()
    }

    override fun onPostCreate(savedInstanceState: Bundle?) {
        super.onPostCreate(savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom)
            WindowInsetsCompat.CONSUMED
        }
    }

    override fun finish() {
        val intent = Intent()
        intent.putExtra(KEY_INTENT_ALBUM_ID, mAlbum?.getId() ?: 0L)
        setResult(RESULT_OK, intent)
        super.finish()
    }

    override fun dispatchTouchEvent(ev: MotionEvent?): Boolean {
        if (mIsAnimating) {
            return true
        }
        return super.dispatchTouchEvent(ev)
    }

    private fun initView() {
        val params = binding.ivMagnifyImage.layoutParams
        params.width = mScreenWidth / 2 - AndroidUtils.dpToPx(20)
        binding.ivMagnifyImage.layoutParams = params

        binding.ivDownImage.scaleX = DOWN_IMAGE_SCALE
        binding.ivDownImage.scaleY = DOWN_IMAGE_SCALE
        binding.btnTrash.isEnabled = false

        mAlbum = AlbumController.getAlbums().find { item ->
            item.getId() == intent.getLongExtra(KEY_INTENT_ALBUM_ID, 0)
        }

        refreshTitle()
        refreshTrashButton()
        setPhotos()

        findViewById<View>(R.id.btn_back).setOnClickListener { finish() }

        binding.clUpImage.setOnTouchListener(mOnTouchListener)

        binding.ivKeep.setOnClickListener { startKeepPhoto() }

        binding.ivDelete.setOnClickListener { startDeletePhoto() }

        binding.btnTrash.setOnClickListener { openTrashActivity() }

        binding.btnCancel.setOnClickListener { startCancelPhoto() }
    }

    private fun setPhotos() {
        mAlbum?.let {
            setPhoto(binding.ivUpImage, it.images[it.getOperatedIndex()])
            val nextIndex = it.getOperatedIndex() + 1
            if (nextIndex == it.getTotalCount()) {
                binding.ivDownImage.visibility = View.GONE

            } else {
                if (nextIndex < it.images.size) {
                    setPhoto(binding.ivDownImage, it.images[nextIndex])
                }
            }
        }
    }

    private fun setPhoto(image: ImageView, photo: Image) {
        Glide.with(this)
            .load(photo.sourceUri)
            .priority(Priority.HIGH)
            .error(R.drawable.ic_vector_image)
            .fitCenter()
            .into(image)
    }

    private fun refreshTitle() {
        mAlbum?.let {
            binding.tvDate.text = it.formatData
            val completeCount: Int = it.getOperatedIndex()
            binding.tvCount.text =
                String.format(
                    Locale.getDefault(),
                    "%d/%d",
                    completeCount + 1,
                    it.images.size
                )
            binding.btnCancel.isEnabled = completeCount != 0
        }
    }

    private fun refreshTrashButton() {
        mAlbum?.let {
            val deleteCount: Int =
                it.images.stream().filter { item -> item.isDelete() }.count().toInt()
            binding.btnTrash.isEnabled = deleteCount != 0
            binding.btnTrash.text = resources.getQuantityString(
                R.plurals.open_trash_bin_picture_count,
                deleteCount,
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
        binding.tvKeep.alpha = 1f
        val downScaleXAnimator = ObjectAnimator.ofFloat(
            binding.ivDownImage,
            View.SCALE_X,
            DOWN_IMAGE_SCALE,
            1f
        )
        val downScaleYAnimator = ObjectAnimator.ofFloat(
            binding.ivDownImage,
            View.SCALE_Y,
            DOWN_IMAGE_SCALE,
            1f
        )
        val scaleXAnimator = ObjectAnimator.ofFloat(binding.ivKeep, View.SCALE_X, 1f, 1.1f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(binding.ivKeep, View.SCALE_Y, 1f, 1.1f, 1f)
        val translateXAnimator =
            ObjectAnimator.ofFloat(
                binding.clUpImage,
                View.TRANSLATION_X,
                mScreenWidth.toFloat()
            )
        val translateYAnimator =
            ObjectAnimator.ofFloat(binding.clUpImage, View.TRANSLATION_Y, 200f)
        val alphaAnimator = ObjectAnimator.ofFloat(binding.clUpImage, View.ALPHA, 0f)
        val rotateAnimator = ObjectAnimator.ofFloat(binding.clUpImage, View.ROTATION, 20f)
        val animatorSet = AnimatorSet()
        animatorSet.duration = AUTO_OPERATION_ANIMATOR_DURATION
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
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                mIsAnimating = true
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                binding.tvKeep.alpha = 0f
                binding.ivDownImage.scaleX = DOWN_IMAGE_SCALE
                binding.ivDownImage.scaleY = DOWN_IMAGE_SCALE
                doOnCompleted(PHOTO_OPERATION_KEEP)
                mIsAnimating = false
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                binding.ivDownImage.scaleX = DOWN_IMAGE_SCALE
                binding.ivDownImage.scaleY = DOWN_IMAGE_SCALE
                mIsAnimating = false
            }
        })

        animatorSet.start()
    }

    private fun startDeletePhoto() {
        if (mIsOperating || mAlbum?.isOperated() == true) {
            return
        }
        binding.tvDelete.alpha = 1f
        val downScaleXAnimator = ObjectAnimator.ofFloat(
            binding.ivDownImage,
            View.SCALE_X, DOWN_IMAGE_SCALE,
            1f
        )
        val downScaleYAnimator = ObjectAnimator.ofFloat(
            binding.ivDownImage,
            View.SCALE_Y, DOWN_IMAGE_SCALE,
            1f
        )
        val scaleXAnimator = ObjectAnimator.ofFloat(binding.ivDelete, View.SCALE_X, 1f, 1.1f, 1f)
        val scaleYAnimator = ObjectAnimator.ofFloat(binding.ivDelete, View.SCALE_Y, 1f, 1.1f, 1f)
        val translateXAnimator =
            ObjectAnimator.ofFloat(
                binding.clUpImage,
                View.TRANSLATION_X,
                -mScreenWidth.toFloat()
            )
        val translateYAnimator =
            ObjectAnimator.ofFloat(binding.clUpImage, View.TRANSLATION_Y, 200f)
        val alphaAnimator = ObjectAnimator.ofFloat(binding.clUpImage, View.ALPHA, 0f)
        val rotateAnimator = ObjectAnimator.ofFloat(binding.clUpImage, View.ROTATION, -20f)
        val animatorSet = AnimatorSet()
        animatorSet.duration = AUTO_OPERATION_ANIMATOR_DURATION
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
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                mIsAnimating = true
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                binding.tvDelete.alpha = 0f
                binding.ivDownImage.scaleX = DOWN_IMAGE_SCALE
                binding.ivDownImage.scaleY = DOWN_IMAGE_SCALE
                doOnCompleted(PHOTO_OPERATION_DELETE)
                mIsAnimating = false
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                binding.ivDownImage.scaleX = DOWN_IMAGE_SCALE
                binding.ivDownImage.scaleY = DOWN_IMAGE_SCALE
                mIsAnimating = false
            }
        })

        animatorSet.start()
    }

    private fun startCancelPhoto() {
        if (mIsOperating) {
            return
        }

        var lastImage: Image? = null
        var currentImage: Image? = null

        mAlbum?.apply {
            lastImage = images[getOperatedIndex() - 1]
            currentImage = images[getOperatedIndex()]
        }

        if (lastImage == null || currentImage == null) {
            return
        }

        binding.ivDownImage.visibility = View.VISIBLE
        setPhoto(binding.ivUpImage, lastImage)
        setPhoto(binding.ivDownImage, currentImage)

        val scaleXAnimator = ObjectAnimator.ofFloat(
            binding.ivDownImage,
            View.SCALE_X,
            1f,
            DOWN_IMAGE_SCALE
        )
        val scaleYAnimator = ObjectAnimator.ofFloat(
            binding.ivDownImage,
            View.SCALE_Y,
            1f,
            DOWN_IMAGE_SCALE
        )
        val textAlphaAnimator = ObjectAnimator.ofFloat(
            if (lastImage.isKeep()) binding.tvKeep else binding.tvDelete,
            View.ALPHA,
            1f,
            0f
        )
        val translateXAnimator = ObjectAnimator.ofFloat(
            binding.clUpImage,
            View.TRANSLATION_X,
            (mScreenWidth * (if (lastImage.isKeep()) 1 else -1)).toFloat(),
            0f
        )
        val translateYAnimator =
            ObjectAnimator.ofFloat(binding.clUpImage, View.TRANSLATION_Y, 200f, 0f)
        val alphaAnimator = ObjectAnimator.ofFloat(binding.clUpImage, View.ALPHA, 0f, 1f)
        val rotateAnimator = ObjectAnimator.ofFloat(
            binding.clUpImage,
            View.ROTATION,
            (20 * (if (lastImage.isKeep()) 1 else -1)).toFloat(),
            0f
        )
        val animatorSet = AnimatorSet()
        animatorSet.duration = AUTO_OPERATION_ANIMATOR_DURATION
        animatorSet.playTogether(
            translateXAnimator,
            alphaAnimator,
            rotateAnimator,
            translateYAnimator,
            textAlphaAnimator,
            scaleXAnimator,
            scaleYAnimator
        )
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                mIsAnimating = true
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                doOnCompleted(PHOTO_OPERATION_CANCEL)
                mIsAnimating = false
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                mIsAnimating = false
            }
        })
        animatorSet.start()
    }

    private fun doOnCompleted(operationType: Int) {
        var image: Image? = null
        mAlbum?.apply {
            image =
                if (operationType == PHOTO_OPERATION_CANCEL) images[getOperatedIndex() - 1] else images[getOperatedIndex()]
        }

        if (image == null) {
            return
        }

        when (operationType) {
            PHOTO_OPERATION_CANCEL -> {
                image.cancelOperated()
                lifecycleScope.launch(Dispatchers.IO) {
                    AlbumController.cleanCompletedImage(image)
                }
                refreshTrashButton()
            }

            PHOTO_OPERATION_KEEP -> {
                image.doKeep()
                lifecycleScope.launch(Dispatchers.IO) {
                    AlbumController.addImage(image)
                }
            }

            PHOTO_OPERATION_DELETE -> {
                image.doDelete()
                lifecycleScope.launch(Dispatchers.IO) {
                    AlbumController.addImage(image)
                }
                refreshTrashButton()
            }
        }

        if (operationType == PHOTO_OPERATION_CANCEL) {
            refreshTitle()
            return
        }

        mAlbum?.takeIf { it.isOperated() }?.let { album ->
            if (album.images.any { it.isDelete() }) {
                openTrashActivity()
            }
            finish()
            return
        }

        setPhotos()
        refreshTitle()

        binding.clUpImage.translationX = 0f
        binding.clUpImage.translationY = 0f
        binding.clUpImage.alpha = 1f
        binding.clUpImage.rotation = 0f
    }

    private fun showMagnifyImageView(rateX: Float, rateY: Float) {
        val drawable = binding.ivUpImage.drawable
        if (drawable is BitmapDrawable) {
            val bitmap = drawable.bitmap
            binding.ivMagnifyImage.visibility = View.VISIBLE
            binding.ivMagnifyImage.setImageBitmap(
                cropFromOriginalBitmap(
                    bitmap,
                    rateX,
                    rateY,
                    AndroidUtils.dpToPx(50)
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

        val centerX = rateX * originalBitmap.width
        val centerY = rateY * originalBitmap.height

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

            if (binding.ivMagnifyImage.translationX == 0f) {
                if (x < binding.ivMagnifyImage.width && y < binding.ivMagnifyImage.height) {
                    binding.ivMagnifyImage.translationX =
                        (mScreenWidth - binding.ivMagnifyImage.width).toFloat()
                }

            } else {
                if (x > mScreenWidth - binding.ivMagnifyImage.width && y < binding.ivMagnifyImage.height) {
                    binding.ivMagnifyImage.translationX = 0f
                }
            }

            mUpImageViewRectF = AndroidUtils.getVisibleImageRect(binding.ivUpImage)
            if (mUpImageViewRectF != null && mUpImageViewRectF!!.contains(x, y)) {
                mShowMagnifyPhotoRunnable = Runnable {
                    showMagnifyImageView(
                        (x - mUpImageViewRectF!!.left) / mUpImageViewRectF!!.width(),
                        (y - mUpImageViewRectF!!.top) / mUpImageViewRectF!!.height()
                    )
                }
                binding.ivMagnifyImage.postDelayed(mShowMagnifyPhotoRunnable, 500)
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
                binding.ivMagnifyImage.removeCallbacks(mShowMagnifyPhotoRunnable)
            }

            if (binding.ivMagnifyImage.isVisible) {
                //intercept, do magnify
                val x = motionEvent.x
                val y = motionEvent.y

                if (binding.ivMagnifyImage.translationX == 0f) {
                    if (x < binding.ivMagnifyImage.width && y < binding.ivMagnifyImage.height) {
                        binding.ivMagnifyImage.translationX =
                            (mScreenWidth - binding.ivMagnifyImage.width).toFloat()
                    }

                } else {
                    if (x > mScreenWidth - binding.ivMagnifyImage.width && y < binding.ivMagnifyImage.height) {
                        binding.ivMagnifyImage.translationX = 0f
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

            binding.tvKeep.alpha = if (isRightSwipe) max(
                0.0,
                (translationX / SHOW_TAG_TRANSLATE_X - 0.2f).toDouble()
            ).toFloat() else 0f
            binding.tvDelete.alpha = if (!isRightSwipe) max(
                0.0,
                (-translationX / SHOW_TAG_TRANSLATE_X - 0.2f).toDouble()
            ).toFloat() else 0f

            if (absTranslationX < SHOW_TAG_TRANSLATE_X) {
                val alphaFactor: Float =
                    1 - absTranslationX / SHOW_TAG_TRANSLATE_X
                val scaleFactor: Float =
                    1 + (if (isRightSwipe) translationX else -translationX) / (SHOW_TAG_TRANSLATE_X * 10)

                binding.ivDelete.alpha = if (isRightSwipe) alphaFactor else 1f
                binding.ivKeep.alpha = if (!isRightSwipe) alphaFactor else 1f

                if (isRightSwipe) {
                    binding.ivKeep.scaleX = scaleFactor
                    binding.ivKeep.scaleY = scaleFactor

                } else {
                    binding.ivDelete.scaleX = scaleFactor
                    binding.ivDelete.scaleY = scaleFactor
                }

            } else {
                binding.ivDelete.alpha = if (isRightSwipe) 0f else 1f
                binding.ivKeep.alpha = if (isRightSwipe) 1f else 0f
            }

        } else if (motionEvent.actionMasked == MotionEvent.ACTION_UP || motionEvent.actionMasked == MotionEvent.ACTION_CANCEL || motionEvent.actionMasked == MotionEvent.ACTION_POINTER_UP) {
            mIsOperating = false
            if (mShowMagnifyPhotoRunnable != null) {
                binding.ivMagnifyImage.removeCallbacks(mShowMagnifyPhotoRunnable)
            }
            //intercept when magnify
            if (binding.ivMagnifyImage.isVisible) {
                binding.ivMagnifyImage.visibility = View.INVISIBLE
                binding.ivMagnifyImage.translationX = 0f
                return@OnTouchListener true
            }
            val translationX = motionEvent.rawX - mTouchX
            val absTranslationX = abs(translationX.toDouble()).toFloat()
            val isRightSwipe = translationX > 0

            if (absTranslationX >= SHOW_TAG_TRANSLATE_X) {
                val scaleXAnimator = ObjectAnimator.ofFloat(
                    binding.ivDownImage,
                    View.SCALE_X,
                    DOWN_IMAGE_SCALE,
                    1f
                )
                val scaleYAnimator = ObjectAnimator.ofFloat(
                    binding.ivDownImage,
                    View.SCALE_Y,
                    DOWN_IMAGE_SCALE,
                    1f
                )
                val translateXAnimator = ObjectAnimator.ofFloat(
                    binding.clUpImage,
                    View.TRANSLATION_X,
                    (if (isRightSwipe) mScreenWidth else -mScreenWidth).toFloat()
                )
                val alphaAnimator =
                    ObjectAnimator.ofFloat(binding.clUpImage, View.ALPHA, 0f)
                val animatorSet = AnimatorSet()
                animatorSet.duration = MANUAL_OPERATION_ANIMATOR_DURATION
                animatorSet.playTogether(
                    translateXAnimator,
                    alphaAnimator,
                    scaleXAnimator,
                    scaleYAnimator
                )
                animatorSet.addListener(object : AnimatorListenerAdapter() {
                    override fun onAnimationStart(animation: Animator) {
                        super.onAnimationStart(animation)
                        mIsAnimating = true
                    }

                    override fun onAnimationEnd(animation: Animator) {
                        super.onAnimationEnd(animation)
                        binding.ivDownImage.scaleX = DOWN_IMAGE_SCALE
                        binding.ivDownImage.scaleY = DOWN_IMAGE_SCALE
                        doOnCompleted(if (isRightSwipe) PHOTO_OPERATION_KEEP else PHOTO_OPERATION_DELETE)
                        mIsAnimating = false
                    }

                    override fun onAnimationCancel(animation: Animator) {
                        super.onAnimationCancel(animation)
                        binding.ivDownImage.scaleX = DOWN_IMAGE_SCALE
                        binding.ivDownImage.scaleY = DOWN_IMAGE_SCALE
                        mIsAnimating = false
                    }
                })
                animatorSet.start()

            } else {
                val keepAlphaAnimator = ObjectAnimator.ofFloat(binding.tvKeep, View.ALPHA, 0f)
                val deleteAlphaAnimator = ObjectAnimator.ofFloat(binding.tvDelete, View.ALPHA, 0f)
                val translateXAnimator =
                    ObjectAnimator.ofFloat(binding.clUpImage, View.TRANSLATION_X, 0f)
                val translateYAnimator =
                    ObjectAnimator.ofFloat(binding.clUpImage, View.TRANSLATION_Y, 0f)
                val rotationAnimator =
                    ObjectAnimator.ofFloat(binding.clUpImage, View.ROTATION, 0f)
                val animatorSet = AnimatorSet()
                animatorSet.duration = MANUAL_OPERATION_ANIMATOR_DURATION
                animatorSet.playTogether(
                    translateXAnimator,
                    translateYAnimator,
                    rotationAnimator,
                    keepAlphaAnimator,
                    deleteAlphaAnimator
                )
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
                ObjectAnimator.ofFloat(binding.ivDelete, View.ALPHA, 1f)
            val deleteImageViewScaleXAnimator =
                ObjectAnimator.ofFloat(binding.ivDelete, View.SCALE_X, 1f)
            val deleteImageViewScaleYAnimator =
                ObjectAnimator.ofFloat(binding.ivDelete, View.SCALE_Y, 1f)
            val keepImageViewAlphaAnimator = ObjectAnimator.ofFloat(binding.ivKeep, View.ALPHA, 1f)
            val keepImageViewScaleXAnimator =
                ObjectAnimator.ofFloat(binding.ivKeep, View.SCALE_X, 1f)
            val keepImageViewScaleYAnimator =
                ObjectAnimator.ofFloat(binding.ivKeep, View.SCALE_Y, 1f)
            val animatorSet = AnimatorSet()
            animatorSet.duration = MANUAL_OPERATION_ANIMATOR_DURATION
            animatorSet.playTogether(
                deleteImageViewAlphaAnimator, deleteImageViewScaleXAnimator,
                deleteImageViewScaleYAnimator, keepImageViewAlphaAnimator,
                keepImageViewScaleXAnimator, keepImageViewScaleYAnimator
            )
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    mIsAnimating = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    binding.tvKeep.alpha = 0f
                    binding.tvDelete.alpha = 0f
                    mIsAnimating = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    binding.tvKeep.alpha = 0f
                    binding.tvDelete.alpha = 0f
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