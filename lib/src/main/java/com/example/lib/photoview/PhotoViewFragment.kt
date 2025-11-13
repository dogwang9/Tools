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
import android.widget.ImageView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.FragmentActivity
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.example.lib.databinding.FragmentPhotoViewBinding
import com.example.lib.databinding.ItemPhotoViewBinding
import com.example.lib.mvvm.BaseFragment
import com.example.lib.utils.AndroidUtils
import java.util.Locale
import kotlin.math.min

/**
需要原图centerCrop，大图fitCenter
 */
class PhotoViewFragment() : BaseFragment<FragmentPhotoViewBinding>() {
    companion object {
        const val TAG_FRAGMENT = "tag_fragment"
        const val TAG_SIZE = "tag_size"
        const val TAG_INDEX = "tag_index"
        const val TAG_LOCATION_X = "tag_location_x"
        const val TAG_LOCATION_Y = "tag_location_y"
        const val TAG_LOCATION_WIDTH = "tag_location_width"
        const val TAG_LOCATION_HEIGHT = "tag_location_height"
        const val TAG_URI = "tag_uri"

        const val ANIMATOR_DURATION = 3000L

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
    private var mIsChangeBackgroundAnimating = false
    private var mIsEnterAnimating = false

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ViewCompat.setOnApplyWindowInsetsListener(binding.root) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            binding.clTitleBar.setPadding(
                systemBars.left,
                systemBars.top,
                systemBars.right,
                0
            )
            WindowInsetsCompat.CONSUMED
        }
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

    override fun initView() {
        val arguments = arguments ?: return

        val size = arguments.getInt(TAG_SIZE)
        val index = arguments.getInt(TAG_INDEX)
        val uri = arguments.getParcelable<Uri>(TAG_URI)
        val (sourceWidth, sourceHeight) = AndroidUtils.getImageSizeFromUri(
            binding.root.context,
            uri!!
        )
            ?: (0 to 0)

        if (sourceWidth == 0 || sourceHeight == 0) {
            binding.ivTransition.visibility = View.GONE
            binding.vpPhotos.visibility = View.VISIBLE

        } else {
            binding.ivTransition.visibility = View.VISIBLE
            binding.vpPhotos.visibility = View.INVISIBLE

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

        binding.vpPhotos.offscreenPageLimit = 1
        binding.vpPhotos.adapter = object : RecyclerView.Adapter<MyViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): MyViewHolder {
                return MyViewHolder(
                    ItemPhotoViewBinding.inflate(
                        LayoutInflater.from(parent.context),
                        parent,
                        false
                    )
                )
            }

            @SuppressLint("ClickableViewAccessibility")
            override fun onBindViewHolder(
                holder: MyViewHolder,
                position: Int
            ) {
                mListener.showPhoto(holder.binding.vImage, position)
                holder.binding.vImage.setOnViewTapListener(object : OnViewTapListener {
                    override fun onViewTap(
                        view: View?,
                        x: Float,
                        y: Float
                    ) {
                        doBackgroundChangeAnimator()
                    }

                })
                holder.binding.vImage.setOnSingleFlingListener(object : OnSingleFlingListener {
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
        binding.vpPhotos.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                binding.tvCount.text =
                    String.format(Locale.getDefault(), "%d/%d", position + 1, size)
            }
        })

        if (index == 0) {
            binding.tvCount.text = String.format(Locale.getDefault(), "%d/%d", 1, size)

        } else {
            binding.vpPhotos.setCurrentItem(index, false)
        }

        binding.vpPhotos.setPageTransformer { page, position ->
            val clampedPosition = position.coerceIn(-1f, 1f)
            val scale = 1f - kotlin.math.abs(clampedPosition) * 0.2f
            val alpha = 1f - kotlin.math.abs(clampedPosition) * 0.4f

            page.scaleX = scale
            page.scaleY = scale
            page.alpha = alpha
        }

        binding.btnBack.setOnClickListener {
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
        val position = binding.vpPhotos.currentItem
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
        if (binding.clTitleBar.isVisible) {
            val colorAnimator = ObjectAnimator.ofArgb(
                binding.root,
                "backgroundColor",
                Color.WHITE,
                Color.BLACK
            )
            val alphaAnimator = ObjectAnimator.ofFloat(
                binding.clTitleBar,
                View.ALPHA,
                1f,
                0f
            )
            val translateAnimator = ObjectAnimator.ofFloat(
                binding.clTitleBar,
                View.TRANSLATION_Y,
                0f,
                -binding.clTitleBar.height.toFloat()
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
                    binding.clTitleBar.visibility = View.INVISIBLE
                }
            })

        } else {
            val colorAnimator = ObjectAnimator.ofArgb(
                binding.root,
                "backgroundColor",
                Color.BLACK,
                Color.WHITE
            )
            val alphaAnimator = ObjectAnimator.ofFloat(
                binding.clTitleBar,
                View.ALPHA,
                0f,
                1f
            )
            val translateAnimator = ObjectAnimator.ofFloat(
                binding.clTitleBar,
                View.TRANSLATION_Y,
                -binding.clTitleBar.height.toFloat(),
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
                    binding.clTitleBar.visibility = View.VISIBLE
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
            .with(binding.ivTransition.context)
            .load(uri)
            .override(centerCropWidth, centerCropHeight)
            .into(binding.ivTransition)

        //获取中心点
        val centerX = locationX + originWidth / 2f
        val centerY = locationY + originHeight / 2f

        //使transitionImageView对齐原始imageView中心点
        binding.ivTransition.x = centerX - centerCropWidth / 2f
        binding.ivTransition.y = centerY - centerCropHeight / 2f

        //获取transitionImageView全屏显示时需要放大的倍数
        val animatorScale = min(
            AndroidUtils.getScreenWidth() / centerCropWidth.toFloat(),
            AndroidUtils.getScreenHeight() / centerCropHeight.toFloat()
        )

        val animatorSet = AnimatorSet()
        animatorSet.duration = ANIMATOR_DURATION
        animatorSet.interpolator = FastOutSlowInInterpolator()
        val scaleXAnimator = ObjectAnimator.ofFloat(
            binding.ivTransition,
            View.SCALE_X,
            animatorScale
        )
        val scaleYAnimator = ObjectAnimator.ofFloat(
            binding.ivTransition,
            View.SCALE_Y,
            animatorScale
        )
        val backgroundAnimator = ObjectAnimator.ofArgb(
            binding.root,
            "backgroundColor",
            Color.TRANSPARENT,
            Color.BLACK
        )
        val clipBoundAnimator = ObjectAnimator.ofObject(
            binding.ivTransition,
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
            binding.ivTransition,
            View.TRANSLATION_X,
            binding.ivTransition.translationX,
            binding.ivTransition.translationX + AndroidUtils.getScreenWidth() / 2f - centerX
        )
        val translateYAnimator = ObjectAnimator.ofFloat(
            binding.ivTransition,
            View.TRANSLATION_Y,
            binding.ivTransition.translationY,
            binding.ivTransition.translationY + AndroidUtils.getScreenHeight() / 2f - centerY
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
                binding.vpPhotos.visibility = View.VISIBLE
                binding.ivTransition.visibility = View.INVISIBLE

                //重置mTranslationImageView的状态
                binding.ivTransition.scaleX = 1f
                binding.ivTransition.scaleY = 1f
                binding.ivTransition.translationX = 0f
                binding.ivTransition.translationY = 0f

                mIsEnterAnimating = false
            }

            override fun onAnimationCancel(animation: Animator) {
                super.onAnimationCancel(animation)
                binding.vpPhotos.visibility = View.VISIBLE
                binding.ivTransition.visibility = View.INVISIBLE

                //重置mTranslationImageView的状态
                binding.ivTransition.scaleX = 1f
                binding.ivTransition.scaleY = 1f
                binding.ivTransition.translationX = 0f
                binding.ivTransition.translationY = 0f

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
            .with(binding.ivTransition.context)
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
                    binding.ivTransition.visibility = View.VISIBLE
                    binding.vpPhotos.visibility = View.INVISIBLE
                    return false
                }

            })
            .into(binding.ivTransition)

        //获取中心点
        val centerX = locationX + originWidth / 2f
        val centerY = locationY + originHeight / 2f

        //使transitionImageView移动到中间
        binding.ivTransition.x = (AndroidUtils.getScreenWidth() - centerCropWidth) / 2f
        binding.ivTransition.y = (AndroidUtils.getScreenHeight() - centerCropHeight) / 2f

        //获取transitionImageView全屏显示时需要放大的倍数
        val animatorScale = min(
            AndroidUtils.getScreenWidth() / centerCropWidth.toFloat(),
            AndroidUtils.getScreenHeight() / centerCropHeight.toFloat()
        )

        val animatorSet = AnimatorSet()
        animatorSet.duration = ANIMATOR_DURATION
        animatorSet.interpolator = DecelerateInterpolator()
        val scaleXAnimator = ObjectAnimator.ofFloat(
            binding.ivTransition,
            View.SCALE_X,
            animatorScale,
            1f
        )
        val scaleYAnimator = ObjectAnimator.ofFloat(
            binding.ivTransition,
            View.SCALE_Y,
            animatorScale,
            1f
        )
        val backgroundAnimator = ObjectAnimator.ofArgb(
            binding.root,
            "backgroundColor",
            if (binding.clTitleBar.isVisible) Color.WHITE else Color.BLACK,
            Color.TRANSPARENT
        )
        val clipBoundAnimator = ObjectAnimator.ofObject(
            binding.ivTransition,
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
            binding.ivTransition,
            View.TRANSLATION_X,
            binding.ivTransition.translationX,
            binding.ivTransition.translationX + centerX - AndroidUtils.getScreenWidth() / 2f
        )
        val translateYAnimator = ObjectAnimator.ofFloat(
            binding.ivTransition,
            View.TRANSLATION_Y,
            binding.ivTransition.translationY,
            binding.ivTransition.translationY + centerY - AndroidUtils.getScreenHeight() / 2f
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

    class MyViewHolder(val binding: ItemPhotoViewBinding) : RecyclerView.ViewHolder(binding.root)
}