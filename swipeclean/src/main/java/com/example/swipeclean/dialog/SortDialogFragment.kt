package com.example.swipeclean.dialog

import android.app.Activity
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import com.example.swipeclean.activity.MainActivity
import com.example.swipeclean.business.ConfigHost
import com.example.tools.R
import com.google.android.material.bottomsheet.BottomSheetDialogFragment

class SortDialogFragment() : BottomSheetDialogFragment() {

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

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.sheet_dialog_sort, container, false)
        initView(view)
        return view
    }

    private fun initView(view: View) {
        val activity = activity ?: return
        dialog?.window?.navigationBarColor = Color.TRANSPARENT

        val dateView = view.findViewById<View>(R.id.v_date)
        val sizeView = view.findViewById<View>(R.id.v_size)
        val unfinishedView = view.findViewById<View>(R.id.v_unfinished)
        val dateImageView = view.findViewById<ImageView>(R.id.iv_date)
        val sizeImageView = view.findViewById<ImageView>(R.id.iv_size)
        val unfinishedImageView = view.findViewById<ImageView>(R.id.iv_unfinished)

        val sortType = ConfigHost.getSortType(activity)
        when (sortType) {
            DATE_DOWN -> {
                dateImageView.setImageResource(R.drawable.ic_vector_sort_down)
            }

            DATE_UP -> {
                dateImageView.setImageResource(R.drawable.ic_vector_sort_up)
            }

            SIZE_DOWN -> {
                sizeImageView.setImageResource(R.drawable.ic_vector_sort_down)
            }

            SIZE_UP -> {
                sizeImageView.setImageResource(R.drawable.ic_vector_sort_up)
            }

            UNFINISHED_DOWN -> {
                unfinishedImageView.setImageResource(R.drawable.ic_vector_sort_down)
            }

            UNFINISHED_UP -> {
                unfinishedImageView.setImageResource(R.drawable.ic_vector_sort_up)
            }
        }

        dateView.setOnClickListener {
            doOnClick(
                when (sortType) {
                    DATE_DOWN -> DATE_UP
                    else -> DATE_DOWN
                }, activity
            )
        }

        sizeView.setOnClickListener {
            doOnClick(
                when (sortType) {
                    SIZE_DOWN -> SIZE_UP
                    else -> SIZE_DOWN
                }, activity
            )
        }

        unfinishedView.setOnClickListener {
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