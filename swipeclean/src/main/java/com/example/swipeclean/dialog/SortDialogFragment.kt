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

        val vDate = view.findViewById<View>(R.id.v_date)
        val vSize = view.findViewById<View>(R.id.v_size)
        val vUnfinished = view.findViewById<View>(R.id.v_unfinished)
        val ivDate = view.findViewById<ImageView>(R.id.iv_date)
        val ivSize = view.findViewById<ImageView>(R.id.iv_size)
        val ivUnfinished = view.findViewById<ImageView>(R.id.iv_unfinished)

        val sortType = ConfigHost.getSortType(activity)
        when (sortType) {
            DATE_DOWN -> {
                ivDate.setImageResource(R.drawable.ic_vector_sort_down)
            }

            DATE_UP -> {
                ivDate.setImageResource(R.drawable.ic_vector_sort_up)
            }

            SIZE_DOWN -> {
                ivSize.setImageResource(R.drawable.ic_vector_sort_down)
            }

            SIZE_UP -> {
                ivSize.setImageResource(R.drawable.ic_vector_sort_up)
            }

            UNFINISHED_DOWN -> {
                ivUnfinished.setImageResource(R.drawable.ic_vector_sort_down)
            }

            UNFINISHED_UP -> {
                ivUnfinished.setImageResource(R.drawable.ic_vector_sort_up)
            }
        }

        vDate.setOnClickListener {
            doOnClick(
                when (sortType) {
                    DATE_DOWN -> DATE_UP
                    else -> DATE_DOWN
                }, activity
            )
        }

        vSize.setOnClickListener {
            doOnClick(
                when (sortType) {
                    SIZE_DOWN -> SIZE_UP
                    else -> SIZE_DOWN
                }, activity
            )
        }

        vUnfinished.setOnClickListener {
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