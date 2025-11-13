package com.example.swipeclean.dialog

import android.app.Activity
import com.example.lib.mvvm.BaseBottomSheetDialogFragment
import com.example.swipeclean.activity.MainActivity
import com.example.swipeclean.business.ConfigHost
import com.example.tools.R
import com.example.tools.databinding.SheetDialogSortBinding

class SortDialogFragment() : BaseBottomSheetDialogFragment<SheetDialogSortBinding>() {

    companion object {
        const val DATE_DOWN: Int = 0
        const val DATE_UP: Int = 1
        const val SIZE_DOWN: Int = 2
        const val SIZE_UP: Int = 3
        const val UNFINISHED_DOWN: Int = 4
        const val UNFINISHED_UP: Int = 5

        fun newInstance(): SortDialogFragment {
            return SortDialogFragment()
        }
    }

    override fun initView() {
        val activity = activity ?: return

        val sortType = ConfigHost.getSortType(activity)
        when (sortType) {
            DATE_DOWN -> {
                binding.ivDate.setImageResource(R.drawable.ic_vector_sort_down)
            }

            DATE_UP -> {
                binding.ivDate.setImageResource(R.drawable.ic_vector_sort_up)
            }

            SIZE_DOWN -> {
                binding.ivSize.setImageResource(R.drawable.ic_vector_sort_down)
            }

            SIZE_UP -> {
                binding.ivSize.setImageResource(R.drawable.ic_vector_sort_up)
            }

            UNFINISHED_DOWN -> {
                binding.ivUnfinished.setImageResource(R.drawable.ic_vector_sort_down)
            }

            UNFINISHED_UP -> {
                binding.ivUnfinished.setImageResource(R.drawable.ic_vector_sort_up)
            }
        }

        binding.ctDate.setOnClickListener {
            doOnClick(
                when (sortType) {
                    DATE_DOWN -> DATE_UP
                    else -> DATE_DOWN
                }, activity
            )
        }

        binding.ctSize.setOnClickListener {
            doOnClick(
                when (sortType) {
                    SIZE_DOWN -> SIZE_UP
                    else -> SIZE_DOWN
                }, activity
            )
        }

        binding.ctUnfinished.setOnClickListener {
            doOnClick(
                when (sortType) {
                    UNFINISHED_DOWN -> UNFINISHED_UP
                    else -> UNFINISHED_DOWN
                }, activity
            )
        }
    }

    private fun doOnClick(targetSortType: Int, activity: Activity) {
        ConfigHost.setSortType(targetSortType, activity)
        if (activity is MainActivity) {
            activity.onChangeSort()
        }
        dismissAllowingStateLoss()
    }
}