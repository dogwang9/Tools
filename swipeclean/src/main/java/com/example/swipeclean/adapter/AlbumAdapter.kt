package com.example.swipeclean.adapter

import android.graphics.Paint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.swipeclean.adapter.AlbumAdapter.MyViewHolder
import com.example.swipeclean.model.Album
import com.example.swipeclean.view.DualProgressIndicator
import com.example.tools.R
import java.util.Locale

class AlbumAdapter(
    val onItemClick: (albumId: Long, albumFormatDate: String, completed: Boolean) -> Unit
) : RecyclerView.Adapter<MyViewHolder>() {

    private val mAlbums = ArrayList<Album>()

    fun setData(albums: List<Album>) {
        val newAlbums = albums.map { it.clone(it) }
        val callback = MyDiffCallback(mAlbums, newAlbums)
        val diffResult = DiffUtil.calculateDiff(callback)
        mAlbums.clear()
        mAlbums.addAll(newAlbums)
        diffResult.dispatchUpdatesTo(this)
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.list_item_album, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val context = holder.itemView.context
        if (context == null) {
            return
        }
        val album = mAlbums[position]
        Glide
            .with(context)
            .load(album.getCoverUri())
            .placeholder(R.drawable.ic_vector_image)
            .into(holder.coverImageView)

        val totalCount = album.getTotalCount()
        val completedCunt = album.getCompletedCount()
        val operatedCount = album.getOperatedIndex()

        holder.dateTextView.text = album.formatData
        holder.progressTextView.text =
            String.format(Locale.getDefault(), "%d/%d %s", completedCunt, totalCount, "张照片")
        holder.progressIndicator.setProgress(100 * operatedCount / totalCount)
        holder.progressIndicator.setSecondaryProgress(100 * completedCunt / totalCount)

        if (album.isCompleted()) {
            holder.completedImageView.visibility = View.VISIBLE
            holder.completedView.visibility = View.VISIBLE
            holder.dateTextView.setTextColor(ContextCompat.getColor(context, com.example.lib.R.color.text_sub))
            holder.dateTextView.paintFlags =
                holder.dateTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.itemView.setOnClickListener {
                onItemClick.invoke(album.getId(), album.formatData, true)
            }

        } else {
            holder.completedImageView.visibility = View.GONE
            holder.completedView.visibility = View.GONE
            holder.dateTextView.setTextColor(ContextCompat.getColor(context, com.example.lib.R.color.text_main))
            holder.dateTextView.paintFlags =
                holder.dateTextView.paintFlags and (Paint.STRIKE_THRU_TEXT_FLAG.inv())
            holder.itemView.setOnClickListener {
                onItemClick.invoke(album.getId(), album.formatData, false)
            }
        }
    }

    override fun getItemCount(): Int {
        return mAlbums.size
    }

    override fun getItemId(position: Int): Long {
        return mAlbums[position].getId()
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImageView: ImageView = itemView.findViewById(R.id.iv_cover)
        val dateTextView: TextView = itemView.findViewById(R.id.tv_date)
        val progressTextView: TextView = itemView.findViewById(R.id.tv_progress)
        val progressIndicator: DualProgressIndicator = itemView.findViewById(R.id.lp_progress)
        val completedImageView: ImageView = itemView.findViewById(R.id.iv_completed)
        val completedView: View = itemView.findViewById(R.id.v_completed)
    }

    class MyDiffCallback(val oldList: List<Album>, val newList: List<Album>) : DiffUtil.Callback() {

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
            return newList[newItemPosition].getId() == oldList[oldItemPosition].getId()
        }

        override fun areContentsTheSame(
            oldItemPosition: Int,
            newItemPosition: Int
        ): Boolean {
            val newAlbum = newList[newItemPosition]
            val oldAlbum = oldList[oldItemPosition]

            return newAlbum.getTotalCount() == oldAlbum.getTotalCount() &&
                    newAlbum.getCompletedCount() == oldAlbum.getCompletedCount() &&
                    newAlbum.getOperatedIndex() == oldAlbum.getOperatedIndex()
        }

    }
}