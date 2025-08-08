package com.example.downloader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.downloader.R
import com.example.downloader.business.model.VideoInfo
import com.example.downloader.model.DownloadInfo
import com.example.downloader.model.DownloadState
import com.example.downloader.model.VideoTask
import com.example.lib.utils.StringUtils
import com.google.android.material.progressindicator.LinearProgressIndicator
import java.util.Locale

class TaskDetectAdapter() :
    RecyclerView.Adapter<TaskDetectAdapter.MyViewHolder>() {

    companion object {
        val FLAG_UPDATE_STATE = 0
    }

    var onDownloadClick: ((videoTask: VideoTask, position: Int) -> Unit)? = null

    private val mVideoTasks = ArrayList<VideoTask>()

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_task_detect, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val videoTask: VideoTask = mVideoTasks[position]
        holder.bindData(videoTask)
        updateItem(holder, videoTask)
        holder.downloadButton.setOnClickListener {
            holder.progressView.isIndeterminate = true
            onDownloadClick?.invoke(videoTask, position)
        }
    }

    override fun onBindViewHolder(holder: MyViewHolder, position: Int, payloads: List<Any?>) {
        if (payloads.isNotEmpty()) {
            for (payload in payloads) {
                if (payload is DownloadInfo) {
                    holder.progressView.isIndeterminate = false
                    updateItem(
                        holder,
                        mVideoTasks[position],
                        payload.progress,
                        payload.speed
                    )

                } else {
                    updateItem(holder, mVideoTasks[position])
                }
            }

        } else {
            onBindViewHolder(holder, position)
        }
    }

    override fun getItemCount(): Int {
        return mVideoTasks.size
    }

    fun insertTask(videoInfo: VideoInfo) {
        mVideoTasks.add(VideoTask(videoInfo))
        notifyItemInserted(mVideoTasks.size - 1)
    }

    fun updateItem(
        holder: MyViewHolder,
        videoTask: VideoTask,
        progress: Int = 0,
        speed: String = ""
    ) {
        when (videoTask.state) {
            DownloadState.NOT_DOWNLOAD -> {
                holder.progressView.visibility = View.GONE
                holder.completeImageView.visibility = View.GONE
                holder.downloadButton.visibility = View.VISIBLE
                holder.failedView.visibility = View.GONE
                holder.downloadSpeedTextView.visibility = View.GONE
                holder.downloadSpeedTextView.text = ""
                holder.progressView.progress = 0
            }

            DownloadState.DOWNLOADING -> {
                holder.completeImageView.visibility = View.GONE
                holder.downloadButton.visibility = View.GONE
                holder.failedView.visibility = View.GONE
                holder.progressView.visibility = View.VISIBLE
                holder.downloadSpeedTextView.visibility = View.VISIBLE
                holder.downloadSpeedTextView.text = speed
                holder.progressView.progress = progress
            }

            DownloadState.DOWNLOADED -> {
                holder.progressView.visibility = View.GONE
                holder.completeImageView.visibility = View.VISIBLE
                holder.downloadButton.visibility = View.GONE
                holder.failedView.visibility = View.GONE
                holder.downloadSpeedTextView.visibility = View.GONE
                holder.downloadSpeedTextView.text = ""
                holder.progressView.progress = 0
            }

            DownloadState.DOWNLOAD_FAILED -> {
                holder.progressView.visibility = View.GONE
                holder.completeImageView.visibility = View.GONE
                holder.downloadButton.visibility = View.VISIBLE
                holder.failedTitleTextView.text = videoTask.error?.title
                holder.failedContentTextView.text = videoTask.error?.content
                holder.failedView.visibility = View.VISIBLE
                holder.downloadSpeedTextView.visibility = View.GONE
                holder.downloadSpeedTextView.text = ""
                holder.progressView.progress = 0
            }
        }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val downloadButton = itemView.findViewById<Button>(R.id.mbt_download)
        val completeImageView = itemView.findViewById<ImageView>(R.id.iv_complete)
        val progressView = itemView.findViewById<LinearProgressIndicator>(R.id.v_progress)
        val failedView = itemView.findViewById<View>(R.id.v_failed)
        val failedTitleTextView = itemView.findViewById<TextView>(R.id.tv_failed_title)
        val failedContentTextView = itemView.findViewById<TextView>(R.id.tv_failed_content)
        val downloadSpeedTextView = itemView.findViewById<TextView>(R.id.tv_speed)
        private val coverImageView = itemView.findViewById<ImageView>(R.id.iv_cover)
        private val titleTextView = itemView.findViewById<TextView>(R.id.tv_title)
        private val uploaderTextView = itemView.findViewById<TextView>(R.id.tv_uploader)
        private val formatTextView = itemView.findViewById<TextView>(R.id.tv_format)
        private val sizeTextView = itemView.findViewById<TextView>(R.id.tv_size)
        private val extensionTextView = itemView.findViewById<TextView>(R.id.tv_extension)
        private val durationTextView = itemView.findViewById<TextView>(R.id.tv_duration)
        private val dateTextView = itemView.findViewById<TextView>(R.id.tv_date)

        fun bindData(videoTask: VideoTask) {
            val videoInfo = videoTask.videoInfo
            Glide
                .with(itemView.context)
                .load(videoInfo.thumbnail)
                .centerCrop()
                .into(coverImageView)
            titleTextView.text = videoInfo.title
            extensionTextView.text = videoInfo.ext?.uppercase(Locale.getDefault())
            formatTextView.text = videoInfo.format
            durationTextView.text = StringUtils.formatDuration(videoInfo.duration.toLong())
            durationTextView.background.alpha = 180
            sizeTextView.text = StringUtils.getHumanFriendlyByteCount(videoInfo.getSize())
            uploaderTextView.text = videoInfo.uploader

            try {
                dateTextView.text = StringUtils.formatDate(
                    videoInfo.uploadDate.toString(),
                    Locale.getDefault()
                )
            } catch (_: Exception) {
                dateTextView.visibility = View.GONE
            }
        }
    }
}