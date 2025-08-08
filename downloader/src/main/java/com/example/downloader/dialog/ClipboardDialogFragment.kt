package com.example.downloader.dialog

import android.content.res.Resources
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import androidx.fragment.app.DialogFragment
import com.example.downloader.R
import com.example.downloader.model.eventbus.UrlMessage
import com.example.lib.utils.AndroidUtils
import org.greenrobot.eventbus.EventBus

class ClipboardDialogFragment : DialogFragment() {

    companion object {

        private const val TAG_CLIPBOARD_URL = "tag_clipboard_url"

        fun newInstance(url: String): ClipboardDialogFragment {
            val dialogFragment = ClipboardDialogFragment()
            val bundle = Bundle()
            bundle.putString(TAG_CLIPBOARD_URL, url)
            dialogFragment.arguments = bundle
            return dialogFragment
        }
    }


    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.dialog_fragment_clipboard_url, container, false)
        initView(view)
        return view
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.apply {
            setLayout(
                Resources.getSystem().displayMetrics.widthPixels - AndroidUtils.dpToPx(40),
                ViewGroup.LayoutParams.WRAP_CONTENT
            )
            setBackgroundDrawableResource(R.drawable.shape_dialog_fragment)
        }
    }

    private fun initView(view: View) {
        val urlTextView = view.findViewById<TextView>(R.id.tv_url)
        val url = arguments?.getString(TAG_CLIPBOARD_URL)
        urlTextView.text = url

        view.findViewById<Button>(R.id.btn_open_url).setOnClickListener {
            dismissAllowingStateLoss()
            EventBus.getDefault().post(UrlMessage(url!!))
//            context?.let {
//                AndroidUtil.clearClipboard(it)
//            }
        }
        view.findViewById<Button>(R.id.iv_close).setOnClickListener {
            dismissAllowingStateLoss()
        }

        view.findViewById<Button>(R.id.btn_clear_clipboard).setOnClickListener {
            dismissAllowingStateLoss()
            context?.let {
                AndroidUtils.clearClipboard(it)
            }
        }
    }
}