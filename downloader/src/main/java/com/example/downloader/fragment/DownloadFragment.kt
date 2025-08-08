package com.example.downloader.fragment

import android.annotation.SuppressLint
import android.os.Bundle
import android.text.TextUtils
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import android.widget.ProgressBar
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.example.downloader.MyApplication
import com.example.downloader.R
import com.example.downloader.adapter.TaskDetectAdapter
import com.example.downloader.business.LibHelper
import com.example.downloader.business.model.VideoInfo
import com.example.downloader.business.model.YtDlpException
import com.example.downloader.business.model.YtDlpRequest
import com.example.downloader.dialog.ClipboardDialogFragment
import com.example.downloader.model.DownloadInfo
import com.example.downloader.model.DownloadState
import com.example.downloader.model.TaskHistory
import com.example.downloader.model.VideoTask
import com.example.downloader.model.eventbus.AddHistoryMessage
import com.example.downloader.model.eventbus.UrlMessage
import com.example.lib.utils.AndroidUtils
import com.example.lib.utils.StringUtils
import com.google.android.material.textfield.TextInputEditText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.greenrobot.eventbus.EventBus
import org.greenrobot.eventbus.Subscribe
import org.greenrobot.eventbus.ThreadMode
import java.io.File

class DownloadFragment : Fragment() {

    private val TAG = "DownloadFragment"
    private val TAG_SHOW_CLIPBOARD_DIALOG: String = "show_clipboard_dialog"

    @Volatile
    private var detectingTaskCount: Int = 0

    private lateinit var mAdapter: TaskDetectAdapter
    private lateinit var mUrlEditText: TextInputEditText
    private lateinit var mProgressbar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (!EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().register(this)
        }
    }

    override fun onDestroy() {
        if (EventBus.getDefault().isRegistered(this)) {
            EventBus.getDefault().unregister(this)
        }
        super.onDestroy()
    }

    @SuppressLint("MissingInflatedId")
    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        val view = inflater.inflate(R.layout.fragment_download, container, false)
        initView(view)
        return view
    }

    override fun onStart() {
        super.onStart()
        checkClipBoard()
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    fun onMessage(message: UrlMessage) {
        searchUrl(message.url)
        mUrlEditText.setText(message.url)
    }

    private fun initView(view: View) {
        mProgressbar = view.findViewById<ProgressBar>(R.id.v_progressbar)
        mUrlEditText = view.findViewById<TextInputEditText>(R.id.v_input_edittext)
        val recyclerView = view.findViewById<RecyclerView>(R.id.v_recyclerview)
        //防止局部刷新引起闪烁
        recyclerView.setItemAnimator(null)

        mAdapter = TaskDetectAdapter()
        mAdapter.onDownloadClick = { videoTask, position ->
            // TODO: 处理安卓10以下的权限适配 处理文件已存在的情况
            // TODO: 处理多个任务同时下载出现的异常
            // TODO: temp文件夹的清理时机？
            val videoInfo = videoTask.videoInfo

            if (videoInfo.getSize() > AndroidUtils.getAvailableInternalStorageSize()) {
                //存储空间不足
                Toast.makeText(context, R.string.insufficient_storage_space, Toast.LENGTH_SHORT)
                    .show()

            } else {
                if (!TextUtils.isEmpty(videoInfo.webpageUrl)) {
                    videoTask.state = DownloadState.DOWNLOADING
                    mAdapter.notifyItemChanged(position, TaskDetectAdapter.FLAG_UPDATE_STATE)

                    try {
                        if (videoInfo.requestedFormats != null && videoInfo.requestedFormats!!.size == 2) {
                            //音视频分离的情况
                            downloadSeparation(videoTask, videoInfo, position)

                        } else {
                            //音视频没有分离的情况
                            downloadUnion(videoTask, videoInfo, position)
                        }

                    } catch (e: YtDlpException) {
                        Log.e(TAG, "Download error : ${e.title} , ${e.message}")
                        lifecycleScope.launch(Dispatchers.Main) {
                            videoTask.error = e
                            videoTask.state = DownloadState.DOWNLOAD_FAILED
                            mAdapter.notifyItemChanged(
                                position,
                                TaskDetectAdapter.FLAG_UPDATE_STATE
                            )
                        }
                    }

                } else {
                    Toast.makeText(context, R.string.url_is_empty, Toast.LENGTH_SHORT).show()
                }
            }
        }
        recyclerView.adapter = mAdapter
        context?.let {
            val layoutManager = LinearLayoutManager(it)
            layoutManager.reverseLayout = true
            layoutManager.stackFromEnd = true
            recyclerView.layoutManager = layoutManager
        }

        mUrlEditText.setOnEditorActionListener { textView, actionId, keyEvent ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                context?.let {
                    AndroidUtils.hideKeyboard(it, mUrlEditText)
                    mUrlEditText.clearFocus()
                }
                searchUrl(mUrlEditText.text.toString())
                true
            } else {
                false
            }
        }
    }

    private fun searchUrl(url: String) {

        if (TextUtils.isEmpty(url)) {
            Toast.makeText(context, R.string.url_is_empty, Toast.LENGTH_SHORT).show()
            return
        }

        if (!StringUtils.isValidWebUrl(url)) {
            Toast.makeText(context, R.string.url_is_illegal, Toast.LENGTH_SHORT).show()
            return
        }

        if (!AndroidUtils.isNetworkAvailable(requireContext())) {
            Toast.makeText(context, R.string.network_not_available, Toast.LENGTH_SHORT).show()
            return
        }

        detectingTaskCount++
        mProgressbar.visibility = View.VISIBLE

        lifecycleScope.launch(Dispatchers.IO) {
            try {
                val request = YtDlpRequest(url)
                val videoInfo = LibHelper.getVideoInfo(request)
                //logVideoInfo(videoInfo)
                withContext(Dispatchers.Main) {
                    mAdapter.insertTask(videoInfo)
                }

            } catch (e: YtDlpException) {
                Log.e(TAG, "Get task info error: $e")
                withContext(Dispatchers.Main) {
                    // TODO: 处理失败的情况
                    Toast.makeText(context, R.string.failed, Toast.LENGTH_SHORT).show()
                }

            } finally {
                withContext(Dispatchers.Main) {
                    detectingTaskCount--
                    mProgressbar.visibility = if (detectingTaskCount == 0) {
                        View.GONE
                    } else {
                        View.VISIBLE
                    }
                }
            }
        }
    }

    private fun checkClipBoard() {
        val dialogFragment = childFragmentManager.findFragmentByTag(TAG_SHOW_CLIPBOARD_DIALOG)
        if (dialogFragment is ClipboardDialogFragment) {
            dialogFragment.dismissAllowingStateLoss()
        }

        //延时 因为从安卓10开始 当app没有获取到焦点时拿不到粘贴板的内容
        lifecycleScope.launch {
            delay(500)
            context?.let {
                val textFromClipboard = AndroidUtils.getTextFromClipboard(it)
                val standardUrl = StringUtils.processUrl(textFromClipboard)
                if (StringUtils.isValidWebUrl(standardUrl)) {
                    val dialogFragment = ClipboardDialogFragment.newInstance(standardUrl)
                    dialogFragment.isCancelable = false
                    dialogFragment.show(childFragmentManager, TAG_SHOW_CLIPBOARD_DIALOG)
                }
            }
        }
    }

    private fun downloadSeparation(videoTask: VideoTask, videoInfo: VideoInfo, position: Int) {
        val taskHistory = TaskHistory()
        var isFinish = false

        lifecycleScope.launch(Dispatchers.IO) {
            var sourcePath: String? = null

            LibHelper.downloadVideo(
                videoInfo.webpageUrl!!,
                processId = null
            ) { progress, speed, line ->
                val progress = progress.toInt()

                if (progress != 0) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        mAdapter.notifyItemChanged(
                            position,
                            DownloadInfo(progress, speed)
                        )
                    }
                }

                when {
                    //下载已完成 开始合并视频和音频
                    line.startsWith("[Merger]") -> {
                        taskHistory.title = videoInfo.title
                        taskHistory.thumbnail = videoInfo.thumbnail
                        taskHistory.uploader = videoInfo.uploader
                        taskHistory.url = videoInfo.webpageUrl
                        taskHistory.duration = videoInfo.duration
                        taskHistory.downloadTime = System.currentTimeMillis()
                        taskHistory.uploadDate = videoInfo.uploadDate
                        taskHistory.source =
                            videoInfo.extractorKey ?: videoInfo.extractor

                        //用正则表达式 找到文件的下载路径
                        sourcePath = "\"([^\"]+)\"".toRegex()
                            .find(line)?.groups?.get(1)?.value
                    }

                    // 在输出Deleting之后 文件已经merge完成
                    line.startsWith("Deleting") -> {
                        if (!isFinish) {
                            isFinish = true
                            //把最终文件移出temp 记录存入数据库
                            val sourceFile = File(
                                sourcePath!!
                            )
                            val targetFile =
                                File(
                                    sourceFile.parentFile!!.parent!!,
                                    AndroidUtils.appendTimestampToFilename(sourceFile.name)
                                )
                            sourceFile.renameTo(targetFile)

                            taskHistory.path = targetFile.absolutePath
                            taskHistory.size = targetFile.length()
                            MyApplication.database.historyDao()
                                .insertHistory(taskHistory)
                            EventBus.getDefault().post(AddHistoryMessage(taskHistory))

                            lifecycleScope.launch(Dispatchers.Main) {
                                videoTask.state = DownloadState.DOWNLOADED
                                mAdapter.notifyItemChanged(
                                    position,
                                    TaskDetectAdapter.FLAG_UPDATE_STATE
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    private fun downloadUnion(videoTask: VideoTask, videoInfo: VideoInfo, position: Int) {
        lifecycleScope.launch(Dispatchers.IO) {
            LibHelper.downloadVideo(
                videoInfo.webpageUrl!!,
                processId = null
            ) { progress, speed, line ->
                val progress = progress.toInt()
                if (progress != 0) {
                    lifecycleScope.launch(Dispatchers.Main) {
                        mAdapter.notifyItemChanged(
                            position,
                            DownloadInfo(progress, speed)
                        )
                    }
                }

                if (line.startsWith("[FixupM3u8]")) {
                    lifecycleScope.launch(Dispatchers.IO) {
                        //延时 因为输出[FixupM3u8]时 下载可能并没有完全结束 为了保险起见 先延时三秒
                        delay(3000)
                        val taskHistory = TaskHistory()
                        taskHistory.title = videoInfo.title
                        taskHistory.thumbnail = videoInfo.thumbnail
                        taskHistory.uploader = videoInfo.uploader
                        taskHistory.url = videoInfo.webpageUrl
                        taskHistory.duration = videoInfo.duration
                        taskHistory.downloadTime = System.currentTimeMillis()
                        taskHistory.uploadDate = videoInfo.uploadDate
                        taskHistory.source =
                            videoInfo.extractorKey ?: videoInfo.extractor

                        //把最终文件移出temp 记录存入数据库
                        val sourceFile = File(
                            //用正则表达式 找到文件的下载路径
                            "\"([^\"]+)\"".toRegex()
                                .find(line)?.groups?.get(1)?.value!!
                        )
                        val targetFile =
                            File(
                                sourceFile.parentFile!!.parent!!,
                                AndroidUtils.appendTimestampToFilename(sourceFile.name)
                            )
                        sourceFile.renameTo(targetFile)

                        taskHistory.path = targetFile.absolutePath
                        taskHistory.size = targetFile.length()
                        MyApplication.database.historyDao()
                            .insertHistory(taskHistory)
                        EventBus.getDefault().post(AddHistoryMessage(taskHistory))

                        lifecycleScope.launch(Dispatchers.Main) {
                            videoTask.state = DownloadState.DOWNLOADED
                            mAdapter.notifyItemChanged(
                                position,
                                TaskDetectAdapter.FLAG_UPDATE_STATE
                            )
                        }
                    }
                }
            }
        }
    }

    private fun logVideoInfo(videoInfo: VideoInfo) {
        Log.i("test", "videoInfo.id: " + videoInfo.id)
        Log.i("test", "videoInfo.fullTitle: " + videoInfo.fullTitle)
        Log.i("test", "videoInfo.title: " + videoInfo.title)
        Log.i("test", "videoInfo.uploadDate: " + videoInfo.uploadDate)
        Log.i("test", "videoInfo.displayId: " + videoInfo.displayId)
        Log.i("test", "videoInfo.duration: " + videoInfo.duration)
        Log.i("test", "videoInfo.description: " + videoInfo.description)
        Log.i("test", "videoInfo.thumbnail: " + videoInfo.thumbnail)
        Log.i("test", "videoInfo.license: " + videoInfo.license)
        Log.i("test", "videoInfo.extractor: " + videoInfo.extractor)
        Log.i("test", "videoInfo.extractorKey: " + videoInfo.extractorKey)
        Log.i("test", "videoInfo.viewCount: " + videoInfo.viewCount)
        Log.i("test", "videoInfo.likeCount: " + videoInfo.likeCount)
        Log.i("test", "videoInfo.dislikeCount: " + videoInfo.dislikeCount)
        Log.i("test", "videoInfo.repostCount: " + videoInfo.repostCount)
        Log.i("test", "videoInfo.averageRating: " + videoInfo.averageRating)
        Log.i("test", "videoInfo.uploaderId: " + videoInfo.uploaderId)
        Log.i("test", "videoInfo.uploader: " + videoInfo.uploader)
        Log.i("test", "videoInfo.playerUrl: " + videoInfo.playerUrl)
        Log.i("test", "videoInfo.webpageUrl: " + videoInfo.webpageUrl)
        Log.i("test", "videoInfo.webpageUrlBasename: " + videoInfo.webpageUrlBasename)
        Log.i("test", "videoInfo.resolution: " + videoInfo.resolution)
        Log.i("test", "videoInfo.width: " + videoInfo.width)
        Log.i("test", "videoInfo.height: " + videoInfo.height)
        Log.i("test", "videoInfo.format: " + videoInfo.format)
        Log.i("test", "videoInfo.formatId: " + videoInfo.formatId)
        Log.i("test", "videoInfo.ext: " + videoInfo.ext)
        Log.i("test", "videoInfo.fileSize: " + videoInfo.fileSize)
        Log.i("test", "videoInfo.fileSizeApproximate: " + videoInfo.fileSizeApproximate)
        Log.i("test", "videoInfo.manifestUrl: " + videoInfo.manifestUrl)
        //Log.i("test", "videoInfo.url: " + videoInfo.url)

        Log.i("test", "videoInfo.httpHeaders.size: " + videoInfo.httpHeaders?.size)
        Log.i("test", "videoInfo.categories.size: " + videoInfo.categories?.size)
        Log.i("test", "videoInfo.tags.size: " + videoInfo.tags?.size)
        Log.i(
            "test",
            "videoInfo.requestedFormats.size: " + videoInfo.requestedFormats?.size
        )
        Log.i("test", "videoInfo.formats.size: " + videoInfo.formats?.size)
        Log.i("test", "videoInfo.thumbnails.size: " + videoInfo.thumbnails?.size)

        for (format in videoInfo.formats!!) {
            Log.i("format", "format.url: " + format.url)
            Log.i("format", "format.fileSize: " + format.fileSize)
            Log.i("format", "format.fileSizeApproximate: " + format.fileSizeApproximate)
            Log.i("format", "format.formatId: " + format.formatId)
            Log.i("format", "format.formatNote: " + format.formatNote)
            Log.i("format", "format.asr: " + format.asr)
            Log.i("format", "format.tbr: " + format.tbr)
            Log.i("format", "format.abr: " + format.abr)
            Log.i("format", "format.format: " + format.format)
            Log.i("format", "format.ext: " + format.ext)
            Log.i("format", "format.preference: " + format.preference)
            Log.i("format", "format.vcodec: " + format.vcodec)
            Log.i("format", "format.acodec: " + format.acodec)
            Log.i("format", "format.fps: " + format.fps)
            Log.i("format", "format.manifestUrl: " + format.manifestUrl)
            Log.i("format", "========================================")
        }

        for (format in videoInfo.formats!!) {
            Log.i("format", "format.url: " + format.url)
            Log.i("format", "format.fileSize: " + format.fileSize)
            Log.i("format", "format.fileSizeApproximate: " + format.fileSizeApproximate)
            Log.i("format", "format.formatId: " + format.formatId)
            Log.i("format", "format.formatNote: " + format.formatNote)
            Log.i("format", "format.asr: " + format.asr)
            Log.i("format", "format.tbr: " + format.tbr)
            Log.i("format", "format.abr: " + format.abr)
            Log.i("format", "format.format: " + format.format)
            Log.i("format", "format.ext: " + format.ext)
            Log.i("format", "format.preference: " + format.preference)
            Log.i("format", "format.vcodec: " + format.vcodec)
            Log.i("format", "format.acodec: " + format.acodec)
            Log.i("format", "format.fps: " + format.fps)
            Log.i("format", "format.manifestUrl: " + format.manifestUrl)
            Log.i("format", "========================================")
        }
    }

}