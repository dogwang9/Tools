package com.example.downloader.dialog

import android.graphics.Color
import android.graphics.Paint
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.annotation.LayoutRes
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.example.downloader.MyApplication
import com.example.downloader.R
import com.example.downloader.model.TaskHistory
import com.example.downloader.model.eventbus.DeleteHistoryMessage
import com.example.downloader.model.eventbus.UrlMessage
import com.example.lib.utils.AndroidUtils
import com.example.lib.utils.StringUtils
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.greenrobot.eventbus.EventBus
import java.io.File

class HistoryDetailDialogFragment(@LayoutRes contentLayoutId: Int = R.layout.dialog_fragment_history_detail) :
    BottomSheetDialogFragment(contentLayoutId) {

    companion object {
        private const val TAG_HISTORY = "tag_history"

        fun newInstance(taskHistory: TaskHistory): HistoryDetailDialogFragment {
            val fragment = HistoryDetailDialogFragment(R.layout.dialog_fragment_history_detail)
            val bundle = Bundle()
            bundle.putParcelable(TAG_HISTORY, taskHistory)
            fragment.arguments = bundle
            return fragment
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view: View = inflater.inflate(R.layout.dialog_fragment_history_detail, container, false)
        initView(view)
        return view
    }

    private fun initView(view: View) {
        val context = view.context
        if (context == null) return

        dialog?.window?.navigationBarColor = Color.TRANSPARENT

        val titleTextView = view.findViewById<TextView>(R.id.tv_title)
        val uploaderTextView = view.findViewById<TextView>(R.id.tv_uploader)
        val sourceAndUploadTimeTextView =
            view.findViewById<TextView>(R.id.tv_source_and_upload_time)
        val urlTextView = view.findViewById<TextView>(R.id.tv_url)
        val downloadPathTextView = view.findViewById<TextView>(R.id.tv_download_path)
        val downloadDateTextView = view.findViewById<TextView>(R.id.tv_download_date)
        val deleteButton = view.findViewById<Button>(R.id.bt_delete)
        val operationButton = view.findViewById<Button>(R.id.bt_operation)

        val taskHistory = arguments?.getParcelable<TaskHistory>(TAG_HISTORY)

        taskHistory?.apply {
            val file = path?.let { File(it) }
            if (file?.exists() == true) {
                downloadPathTextView.visibility = View.VISIBLE
                downloadPathTextView.text = path
                operationButton.text = ContextCompat.getString(context, R.string.share)
                operationButton.setOnClickListener {
                    AndroidUtils.shareFile(context, path)
                }

            } else {
                downloadPathTextView.visibility = View.GONE
                operationButton.text = ContextCompat.getString(context, R.string.re_download)
                operationButton.setOnClickListener {
                    EventBus.getDefault().post(UrlMessage(url!!))
                    dismissAllowingStateLoss()
                }
            }

            titleTextView.text = title
            uploaderTextView.text = uploader
            urlTextView.text = url
            downloadDateTextView.text = StringUtils.formatDate(downloadTime)
            sourceAndUploadTimeTextView.text =
                String.format("%s · %s", source, StringUtils.formatDate(uploadDate))

            urlTextView.paintFlags = urlTextView.paintFlags or Paint.UNDERLINE_TEXT_FLAG
            urlTextView.setOnClickListener {
                AndroidUtils.openUrl(context, url)
            }
            deleteButton.setOnClickListener {
                // TODO: 增加弹窗: 确认删除、是否保留文件
                EventBus.getDefault().post(DeleteHistoryMessage(id))
                lifecycleScope.launch(Dispatchers.IO) {
                    MyApplication.database.historyDao().deleteHistory(id)
                }
                dismissAllowingStateLoss()
            }
        }
    }
}