package com.example.downloader.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.downloader.R
import com.example.downloader.model.TaskHistory
import com.example.lib.utils.StringUtils

class TaskHistoryAdapter() : RecyclerView.Adapter<TaskHistoryAdapter.MyViewHolder>() {

    private val mTaskHistories = ArrayList<TaskHistory>()
    var onMoreClick: ((taskHistory: TaskHistory, position: Int) -> Unit)? = null

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context).inflate(R.layout.item_task_history, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val taskHistory = mTaskHistories[position]
        holder.bindData(taskHistory)
        holder.moreButton.setOnClickListener {
            onMoreClick?.invoke(taskHistory, position)
        }
    }

    override fun getItemCount(): Int {
        return mTaskHistories.size
    }

    override fun getItemId(position: Int): Long {
        return mTaskHistories[position].id
    }

    fun setData(taskHistories: ArrayList<TaskHistory>) {
//        mTaskHistories = taskHistories
//        notifyDataSetChanged()
        val callback = MyDiffCallback(mTaskHistories, taskHistories)
        val diffResult = DiffUtil.calculateDiff(callback)
        mTaskHistories.clear()
        mTaskHistories.addAll(taskHistories)
        diffResult.dispatchUpdatesTo(this)
    }

    fun insertItem(history: TaskHistory) {
        mTaskHistories.add(history)
        notifyItemInserted(mTaskHistories.size - 1)
    }

    fun deleteItem(id: Long) {
        val index = mTaskHistories.indexOfFirst { it.id == id }
        mTaskHistories.removeAt(index)
        notifyItemRemoved(index)
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val moreButton = itemView.findViewById<Button>(R.id.mbt_more)
        private val coverImageView = itemView.findViewById<ImageView>(R.id.iv_cover)
        private val titleTextView = itemView.findViewById<TextView>(R.id.tv_title)
        private val uploaderTextView = itemView.findViewById<TextView>(R.id.tv_uploader)
        private val sizeTextView = itemView.findViewById<TextView>(R.id.tv_size)
        private val durationTextView = itemView.findViewById<TextView>(R.id.tv_duration)
        private val unavailableTextView = itemView.findViewById<TextView>(R.id.tv_unavailable)

        fun bindData(taskHistory: TaskHistory) {
            Glide
                .with(itemView.context)
                .load(taskHistory.thumbnail)
                .centerCrop()
                .into(coverImageView)
            titleTextView.text = taskHistory.title
            uploaderTextView.text = taskHistory.uploader
            durationTextView.text = StringUtils.formatDuration(taskHistory.duration.toLong())
            durationTextView.background.alpha = 180

            if (taskHistory.available) {
                unavailableTextView.visibility = View.GONE
                sizeTextView.visibility = View.VISIBLE
                sizeTextView.text = StringUtils.getHumanFriendlyByteCount(taskHistory.size)

            } else {
                unavailableTextView.visibility = View.VISIBLE
                sizeTextView.visibility = View.GONE
            }
        }
    }

    class MyDiffCallback(val oldList: List<TaskHistory>, val newList: List<TaskHistory>) :
        DiffUtil.Callback() {

        override fun getOldListSize(): Int {
            return oldList.size
        }

        override fun getNewListSize(): Int {
            return newList.size
        }

        override fun areItemsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            return oldList[oldItemPosition].id == newList[newItemPosition].id
        }

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            return oldList[oldItemPosition].available == newList[newItemPosition].available
        }

    }
}