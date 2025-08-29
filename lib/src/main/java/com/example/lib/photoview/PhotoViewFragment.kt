package com.example.lib.photoview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.animation.RectEvaluator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.animation.DecelerateInterpolator
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.lib.R
import com.example.lib.utils.AndroidUtils
import java.util.Locale
import kotlin.math.min

/**
需要原图centerCrop，大图fitCenter
 */
class PhotoViewFragment() : Fragment() {
    companion object {
        const val TAG_FRAGMENT = "tag_fragment"
        const val TAG_SIZE = "tag_size"
        const val TAG_INDEX = "tag_index"
        const val TAG_LOCATION_X = "tag_location_x"
        const val TAG_LOCATION_Y = "tag_location_y"
        const val TAG_LOCATION_WIDTH = "tag_location_width"
        const val TAG_LOCATION_HEIGHT = "tag_location_height"
        const val TAG_URI = "tag_uri"

        const val ANIMATOR_DURATION = 300L

        fun show(
            activity: FragmentActivity,
            index: Int,
            size: Int,
            uri: Uri,
            imageView: ImageView
        ) {
            val fragment = PhotoViewFragment()
            val location = IntArray(2)
            imageView.getLocationOnScreen(location)
            val bundle = Bundle()
            bundle.putInt(TAG_INDEX, index)
            bundle.putInt(TAG_SIZE, size)
            bundle.putInt(TAG_LOCATION_X, location[0])
            bundle.putInt(TAG_LOCATION_Y, location[1])
            bundle.putInt(TAG_LOCATION_WIDTH, imageView.width)
            bundle.putInt(TAG_LOCATION_HEIGHT, imageView.height)
            bundle.putParcelable(TAG_URI, uri)
            fragment.arguments = bundle

            activity.supportFragmentManager.beginTransaction()
                .add(android.R.id.content, fragment, TAG_FRAGMENT)
                .show(fragment).commitNowAllowingStateLoss()
        }
    }

    interface Listener {
        fun showPhoto(imageView: ImageView, index: Int)
        fun getRecyclerView(): RecyclerView
        fun getUri(position: Int): Uri?
        fun getImageView(position: Int): ImageView?
    }

    private lateinit var mListener: Listener
    private lateinit var mViewpager: ViewPager2
    private lateinit var mTitleBar: View
    private lateinit var mMainView: View
    private lateinit var mTranslationImageView: ImageView
    private var mIsChangeBackgroundAnimating = false
    private var mIsEnterAnimating = false

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_photo_view, container, false)
        ViewCompat.setOnApplyWindowInsetsListener(view.findViewById(R.id.main)) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            val titleBar = view.findViewById<View>(R.id.v_title_bar)
            titleBar.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )
            insets
        }
        initView(view)
        return view
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        if (context is Listener) {
            mListener = context

        } else {
            throw RuntimeException("You must implement PhotoViewFragment's Listener")
        }
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                doClose()
            }
        })
    }

    private fun initView(view: View) {
        val arguments = arguments ?: return

        mTitleBar = view.findViewById(R.id.v_title_bar)
        mMainView = view.findViewById(R.id.main)
        mViewpager = view.findViewById(R.id.v_viewpager)
        mTranslationImageView = view.findViewById(R.id.iv_transition)

        val countTextView: TextView = view.findViewById(R.id.tv_count)
        val size = arguments.getInt(TAG_SIZE)
        val index = arguments.getInt(TAG_INDEX)
        val uri = arguments.getParcelable<Uri>(TAG_URI)
        val (sourceWidth, sourceHeight) = AndroidUtils.getImageSizeFromUri(view.context, uri!!)
            ?: (0 to 0)

        if (sourceWidth == 0 || sourceHeight == 0) {
            mTranslationImageView.visibility = View.GONE
            mViewpager.visibility = View.VISIBLE

        } else {
            mTranslationImageView.visibility = View.VISIBLE
            mViewpager.visibility = View.INVISIBLE

            val locationX = arguments.getInt(TAG_LOCATION_X)
            val locationY = arguments.getInt(TAG_LOCATION_Y)
            val originWidth = arguments.getInt(TAG_LOCATION_WIDTH)
            val originHeight = arguments.getInt(TAG_LOCATION_HEIGHT)

            doEnterAnimator(
                locationX,
                locationY,
                originWidth,
                originHeight,
                sourceWidth,
                sourceHeight,
                uri
            )
        }

        mViewpager.offscreenPageLimit = 1
        mViewpager.adapter = object : RecyclerView.Adapter<MyViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): MyViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_photo_view, parent, false)
                return MyViewHolder(view)
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onBindViewHolder(
                holder: MyViewHolder,
                position: Int
            ) {
                mListener.showPhoto(holder.photoView, position)
                holder.photoView.setOnViewTapListener(object : OnViewTapListener {
                    override fun onViewTap(
                        view: View?,
                        x: Float,
                        y: Float
                    ) {
                        doBackgroundChangeAnimator()
                    }

                })
                holder.photoView.setOnSingleFlingListener(object : OnSingleFlingListener {
                    override fun onFling(
                        e1: MotionEvent?,
                        e2: MotionEvent?,
                        velocityX: Float,
                        velocityY: Float
                    ): Boolean {
                        doClose()
                        return true
                    }

                })
            }

            override fun getItemCount(): Int {
                return size
            }

        }
        mViewpager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                countTextView.text = String.format(Locale.getDefault(), "%d/%d", position + 1, size)
            }
        })

        if (index == 0) {
            countTextView.text = String.format(Locale.getDefault(), "%d/%d", 1, size)

        } else {
            mViewpager.setCurrentItem(index, false)
        }

        mViewpager.setPageTransformer { page, position ->
            val clampedPosition = position.coerceIn(-1f, 1f)
            val scale = 1f - kotlin.math.abs(clampedPosition) * 0.2f
            val alpha = 1f - kotlin.math.abs(clampedPosition) * 0.4f

            page.scaleX = scale
            page.scaleY = scale
            page.alpha = alpha
        }

        view.findViewById<Button>(R.id.btn_back).setOnClickListener {
            doClose()
        }
    }

    private fun removeFragment() {
        getParentFragmentManager()
            .beginTransaction()
            .remove(this@PhotoViewFragment)
            .commit()
    }

    private fun doClose() {
        if (mIsEnterAnimating) {
            return
        }
        val position = mViewpager.currentItem
        val recyclerView = mListener.getRecyclerView()
        recyclerView.scrollToPosition(position)
        recyclerView.post {
            val uri = mListener.getUri(position)
            val imageView = mListener.getImageView(position)

            if (imageView == null) {
                removeFragment()
                return@post
            }
            val (sourceWidth, sourceHeight) = if (uri == null) {
                removeFragment()
                return@post
            } else {
                AndroidUtils.getImageSizeFromUri(imageView.context, uri) ?: (0 to 0)
            }

            if (sourceWidth == 0 || sourceHeight == 0) {
                removeFragment()
                return@post
            }

            val location = IntArray(2).apply {
                imageView.getLocationOnScreen(this)
            }

            doExitAnimator(
                location[0],
                location[1],
                imageView.width,
                imageView.height,
                sourceWidth,
                sourceHeight,
                uri
            )

        }
    }

    private fun doBackgroundChangeAnimator() {
        if (mIsChangeBackgroundAnimating) {
            return
        }
        val animatorSet = AnimatorSet()
        animatorSet.duration = 200
        if (mTitleBar.isVisible) {
            val colorAnimator = ObjectAnimator.ofArgb(
                mMainView,
                "backgroundColor",
                Color.WHITE,
                Color.BLACK
            )
            val alphaAnimator = ObjectAnimator.ofFloat(
                mTitleBar,
                View.ALPHA,
                1f,
                0f
            )
            val translateAnimator = ObjectAnimator.ofFloat(
                mTitleBar,
                View.TRANSLATION_Y,
                0f,
                -mTitleBar.height.toFloat()
            )
            animatorSet.playTogether(
                colorAnimator,
                alphaAnimator,
                translateAnimator
            )
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    mIsChangeBackgroundAnimating = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mIsChangeBackgroundAnimating = false
                    mTitleBar.visibility = View.INVISIBLE
                }
            })

        } else {
            val colorAnimator = ObjectAnimator.ofArgb(
                mMainView,
                "backgroundColor",
                Color.BLACK,
                Color.WHITE
            )
            val alphaAnimator = ObjectAnimator.ofFloat(
                mTitleBar,
                View.ALPHA,
                0f,
                1f
            )
            val translateAnimator = ObjectAnimator.ofFloat(
                mTitleBar,
                View.TRANSLATION_Y,
                -mTitleBar.height.toFloat(),
                0f
            )
            animatorSet.playTogether(
                colorAnimator,
                alphaAnimator,
                translateAnimator
            )
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    mIsChangeBackgroundAnimating = true
                    mTitleBar.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mIsChangeBackgroundAnimating = false
                }

                override fun onAnimationCancel(animation: Animator) {
                    super.onAnimationCancel(animation)
                    mIsChangeBackgroundAnimating = false
                }
            })
        }
        animatorSet.start()
    }

    private fun doEnterAnimator(
        locationX: Int,
        locationY: Int,
        originWidth: Int,
        originHeight: Int,
        sourceWidth: Int,
        sourceHeight: Int,
        uri: Uri
    ) {
        //获取centerCrop的放缩倍数
        val centerCropScale = maxOf(
            originWidth / sourceWidth.toFloat(),
            originHeight / sourceHeight.toFloat()
        )

        //获取centerCrop下全部显示时的宽高
        val centerCropWidth = (sourceWidth * centerCropScale).toInt()
        val centerCropHeight = (sourceHeight * centerCropScale).toInt()
        Glide
            .with(mTranslationImageView.context)
            .load(uri)
            .override(centerCropWidth, centerCropHeight)
            .into(mTranslationImageView)

        //获取中心点
        val centerX = locationX + originWidth / 2f
        val centerY = locationY + originHeight / 2f

        //使transitionImageView对齐原始imageView中心点
        mTranslationImageView.x = centerX - centerCropWidth / 2f
        mTranslationImageView.y = centerY - centerCropHeight / 2f

        //获取transitionImageView全屏显示时需要放大的倍数
        val animatorScale = min(
            AndroidUtils.getScreenWidth() / centerCropWidth.toFloat(),
            AndroidUtils.getScreenHeight() / centerCropHeight.toFloat()
        )

        val animatorSet = AnimatorSet()
        animatorSet.duration = ANIMATOR_DURATION
        animatorSet.interpolator = FastOutSlowInInterpolator()
        val scaleXAnimator = ObjectAnimator.ofFloat(
            mTranslationImageView,
            View.SCALE_X,
            animatorScale
        )
        val scaleYAnimator = ObjectAnimator.ofFloat(
            mTranslationImageView,
            View.SCALE_Y,
            animatorScale
        )
        val backgroundAnimator = ObjectAnimator.ofArgb(
            mMainView,
            "backgroundColor",
            Color.TRANSPARENT,
            Color.BLACK
        )
        val clipBoundAnimator = ObjectAnimator.ofObject(
            mTranslationImageView,
            "clipBounds",
            RectEvaluator(),
            Rect(
                (centerCropWidth - originWidth) / 2,
                (centerCropHeight - originHeight) / 2,
                (centerCropWidth + originWidth) / 2,
                (centerCropHeight + originHeight) / 2
            ),
            Rect(0, 0, centerCropWidth, centerCropHeight)
        )
        val translateXAnimator = ObjectAnimator.ofFloat(
            mTranslationImageView,
            View.TRANSLATION_X,
            mTranslationImageView.translationX,
            mTranslationImageView.translationX + AndroidUtils.getScreenWidth() / 2f - centerX
        )
        val translateYAnimator = ObjectAnimator.ofFloat(
            mTranslationImageView,
            View.TRANSLATION_Y,
            mTranslationImageView.translationY,
            mTranslationImageView.translationY + AndroidUtils.getScreenHeight() / 2f - centerY
        )
        animatorSet.playTogether(
            scaleXAnimator,
            scaleYAnimator,
            translateXAnimator,
            translateYAnimator,
            backgroundAnimator,
            clipBoundAnimator
        )
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationStart(animation: Animator) {
                super.onAnimationStart(animation)
                mIsEnterAnimating = true
            }

            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                mViewpager.visibility = View.VISIBLE
                mTranslationImageView.visibility = View.INVISIBLE

                //重置mTranslationImageView的状态
                mTranslationImageView.scaleX = 1f
                mTranslationImageView.scaleY = 1f
                mTranslationImageView.translationX = 0f
                mTranslationImageView.translationY = 0f

                mIsEnterAnimating = false
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                mViewpager.visibility = View.VISIBLE
                mTranslationImageView.visibility = View.INVISIBLE

                //重置mTranslationImageView的状态
                mTranslationImageView.scaleX = 1f
                mTranslationImageView.scaleY = 1f
                mTranslationImageView.translationX = 0f
                mTranslationImageView.translationY = 0f

                mIsEnterAnimating = false
            }
        })
        animatorSet.start()
    }

    private fun doExitAnimator(
        locationX: Int,
        locationY: Int,
        originWidth: Int,
        originHeight: Int,
        sourceWidth: Int,
        sourceHeight: Int,
        uri: Uri
    ) {
        //获取centerCrop的放缩倍数
        val scale = maxOf(
            originWidth / sourceWidth.toFloat(),
            originHeight / sourceHeight.toFloat()
        )

        //获取centerCrop下全部显示时的宽高
        val centerCropWidth = (sourceWidth * scale).toInt()
        val centerCropHeight = (sourceHeight * scale).toInt()

        Glide
            .with(mTranslationImageView.context)
            .load(uri)
            .override(centerCropWidth, centerCropHeight)
            .addListener(object : RequestListener<Drawable> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    isFirstResource: Boolean
                ): Boolean {
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable?,
                    model: Any?,
                    target: Target<Drawable?>?,
                    dataSource: DataSource?,
                    isFirstResource: Boolean
                ): Boolean {
                    mTranslationImageView.visibility = View.VISIBLE
                    mViewpager.visibility = View.INVISIBLE
                    return false
                }

            })
            .into(mTranslationImageView)

        //获取中心点
        val centerX = locationX + originWidth / 2f
        val centerY = locationY + originHeight / 2f

        //使transitionImageView移动到中间
        mTranslationImageView.x = (AndroidUtils.getScreenWidth() - centerCropWidth) / 2f
        mTranslationImageView.y = (AndroidUtils.getScreenHeight() - centerCropHeight) / 2f

        //获取transitionImageView全屏显示时需要放大的倍数
        val animatorScale = min(
            AndroidUtils.getScreenWidth() / centerCropWidth.toFloat(),
            AndroidUtils.getScreenHeight() / centerCropHeight.toFloat()
        )

        val animatorSet = AnimatorSet()
        animatorSet.duration = ANIMATOR_DURATION
        animatorSet.interpolator = DecelerateInterpolator()
        val scaleXAnimator = ObjectAnimator.ofFloat(
            mTranslationImageView,
            View.SCALE_X,
            animatorScale,
            1f
        )
        val scaleYAnimator = ObjectAnimator.ofFloat(
            mTranslationImageView,
            View.SCALE_Y,
            animatorScale,
            1f
        )
        val backgroundAnimator = ObjectAnimator.ofArgb(
            mMainView,
            "backgroundColor",
            if (mTitleBar.isVisible) Color.WHITE else Color.BLACK,
            Color.TRANSPARENT
        )
        val clipBoundAnimator = ObjectAnimator.ofObject(
            mTranslationImageView,
            "clipBounds",
            RectEvaluator(),
            Rect(0, 0, centerCropWidth, centerCropHeight),
            Rect(
                (centerCropWidth - originWidth) / 2,
                (centerCropHeight - originHeight) / 2,
                (centerCropWidth + originWidth) / 2,
                (centerCropHeight + originHeight) / 2
            )
        )
        val translateXAnimator = ObjectAnimator.ofFloat(
            mTranslationImageView,
            View.TRANSLATION_X,
            mTranslationImageView.translationX,
            mTranslationImageView.translationX + centerX - AndroidUtils.getScreenWidth() / 2f
        )
        val translateYAnimator = ObjectAnimator.ofFloat(
            mTranslationImageView,
            View.TRANSLATION_Y,
            mTranslationImageView.translationY,
            mTranslationImageView.translationY + centerY - AndroidUtils.getScreenHeight() / 2f
        )
        animatorSet.playTogether(
            scaleXAnimator,
            scaleYAnimator,
            translateXAnimator,
            translateYAnimator,
            backgroundAnimator,
            clipBoundAnimator
        )
        animatorSet.addListener(object : AnimatorListenerAdapter() {
            override fun onAnimationEnd(animation: Animator) {
                super.onAnimationEnd(animation)
                removeFragment()
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                removeFragment()
            }
        })
        animatorSet.start()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.v_image)
    }
}