package com.example.tool.reactiontest

import android.annotation.SuppressLint
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.SystemClock
import android.view.MotionEvent
import android.view.View
import com.example.lib.mvvm.BaseActivity
import com.example.tool.R
import com.example.tool.databinding.ActivityReactionTestAvtivityBinding
import java.util.concurrent.ThreadLocalRandom

class ReactionTestActivity : BaseActivity<ActivityReactionTestAvtivityBinding>() {
    private val mHandler: Handler = Handler(Looper.getMainLooper())
    private var mColorChangeTime = 0L

    @SuppressLint("ClickableViewAccessibility")
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding.ivArea.setOnTouchListener { view: View, motionEvent: MotionEvent ->
            when (motionEvent.action) {
                MotionEvent.ACTION_DOWN -> {
                    binding.tvResult.text = ""
                    mHandler.postDelayed({
                        mColorChangeTime = SystemClock.elapsedRealtime()
                        view.setBackgroundColor(getColor(R.color.reaction_test_area_change))
                    }, ThreadLocalRandom.current().nextLong(2000, 5000))
                }

                MotionEvent.ACTION_UP -> {
                    binding.tvResult.text =
                        if (mColorChangeTime == 0L) getString(R.string.reaction_test_warning) else getString(
                            R.string.reaction_test_result,
                            SystemClock.elapsedRealtime() - mColorChangeTime
                        )
                    mColorChangeTime = 0L
                    view.setBackgroundColor(getColor(R.color.reaction_test_area_normal))
                    mHandler.removeCallbacksAndMessages(null)
                }
            }
            true
        }

    }
}