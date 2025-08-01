package com.example.lib.photoview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.animation.AnimatorSet
import android.animation.ObjectAnimator
import android.content.Context
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.activity.OnBackPressedCallback
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import androidx.viewpager2.widget.ViewPager2
import com.example.lib.R
import java.util.Locale

// TODO: 这个页面可以增加一个恢复按钮
class PhotoViewFragment() : Fragment() {
    companion object {
        const val TAG_SIZE = "tag_size"
        const val TAG_INDEX = "tag_index"
        fun show(
            activity: FragmentActivity,
            index: Int,
            size: Int
        ) {
            val fragment = PhotoViewFragment()
            val bundle = Bundle()
            bundle.putInt(TAG_INDEX, index)
            bundle.putInt(TAG_SIZE, size)
            fragment.arguments = bundle

            activity.supportFragmentManager.beginTransaction().add(android.R.id.content, fragment)
                .show(fragment).commitNowAllowingStateLoss()
        }
    }

    interface Listener {
        fun showPhoto(imageView: ImageView, index: Int)
    }

    private var mListener: Listener? = null
    private lateinit var mViewpager: ViewPager2
    private lateinit var mTitleBar: View
    private lateinit var mMainView: View
    private var mIsAnimating = false

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
        }
        activity?.onBackPressedDispatcher?.addCallback(this, object : OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                close()
            }
        })
    }

    private fun initView(view: View) {
        val arguments = arguments ?: return
        val size = arguments.getInt(TAG_SIZE)
        val index = arguments.getInt(TAG_INDEX)

        val countTextView: TextView = view.findViewById(R.id.tv_count)
        mTitleBar = view.findViewById(R.id.v_title_bar)
        mMainView = view.findViewById(R.id.main)
        mViewpager = view.findViewById(R.id.v_viewpager)

        mViewpager.offscreenPageLimit = 2
        mViewpager.adapter = object : RecyclerView.Adapter<MyViewHolder>() {
            override fun onCreateViewHolder(
                parent: ViewGroup,
                viewType: Int
            ): MyViewHolder {
                val view = LayoutInflater.from(parent.context)
                    .inflate(R.layout.item_photo_view, parent, false)
                return MyViewHolder(view)
            }

            override fun onBindViewHolder(
                holder: MyViewHolder,
                position: Int
            ) {
                mListener?.showPhoto(holder.photoView, position)
                holder.photoView.setOnClickListener {
                    doAnimator()
                }
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
            close()
        }
    }

    private fun close() {
        getParentFragmentManager()
            .beginTransaction()
            .remove(this@PhotoViewFragment)
            .commit()
    }

    private fun doAnimator() {
        if (mIsAnimating) {
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
            val titleBarColorAnimator = ObjectAnimator.ofArgb(
                mTitleBar,
                "backgroundColor",
                Color.WHITE,
                Color.BLACK
            )
            val translateAnimator = ObjectAnimator.ofFloat(
                mTitleBar,
                View.TRANSLATION_Y,
                -mTitleBar.height.toFloat()
            )
            animatorSet.playTogether(
                colorAnimator,
                titleBarColorAnimator,
                translateAnimator
            )
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    mIsAnimating = true
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mIsAnimating = false
                    mTitleBar.visibility = View.GONE
                }
            })

        } else {
            val colorAnimator = ObjectAnimator.ofArgb(
                mMainView,
                "backgroundColor",
                Color.BLACK,
                Color.WHITE
            )
            val titleBarColorAnimator = ObjectAnimator.ofArgb(
                mTitleBar,
                "backgroundColor",
                Color.BLACK,
                Color.WHITE
            )
            val translateAnimator = ObjectAnimator.ofFloat(
                mTitleBar,
                View.TRANSLATION_Y,
                0f
            )
            animatorSet.playTogether(
                colorAnimator,
                titleBarColorAnimator,
                translateAnimator
            )
            animatorSet.addListener(object : AnimatorListenerAdapter() {
                override fun onAnimationStart(animation: Animator) {
                    super.onAnimationStart(animation)
                    mIsAnimating = true
                    mTitleBar.visibility = View.VISIBLE
                }

                override fun onAnimationEnd(animation: Animator) {
                    super.onAnimationEnd(animation)
                    mIsAnimating = false
                }
            })
        }
        animatorSet.start()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val photoView: PhotoView = itemView.findViewById(R.id.v_photo)
    }
}