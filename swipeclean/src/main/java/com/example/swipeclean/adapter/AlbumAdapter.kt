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
        val newAlbums = albums.map{it.clone(it)}
        val callback = MyDiffCallback(mAlbums, newAlbums)
        val diffResult = DiffUtil.calculateDiff(callback)
        mAlbums.clear()
        mAlbums.addAll(newAlbums)
        diffResult.dispatchUpdatesTo(this);
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
        val album = mAlbums[position]

        Glide
            .with(context)
            .load(album.getCoverPath())
            .placeholder(R.drawable.ic_vector_image)
            .into(holder.mCoverImageView)

        val totalCount = album.getTotalCount()
        val completedCunt = album.getCompletedCount()
        val operatedCount = album.getOperatedIndex()

        holder.mDateTextView.text = album.formatData
        holder.mProgressTextView.text =
            String.format(Locale.getDefault(), "%d/%d %s", completedCunt, totalCount, "张照片")
        holder.mProgressIndicator.setProgress(100 * operatedCount / totalCount)
        holder.mProgressIndicator.setSecondaryProgress(100 * completedCunt / totalCount)

        if (album.isCompleted()) {
            holder.mCompletedImageView.visibility = View.VISIBLE
            holder.mCompletedView.visibility = View.VISIBLE
            holder.mDateTextView.setTextColor(ContextCompat.getColor(context, R.color.text_sub))
            holder.mDateTextView.paintFlags =
                holder.mDateTextView.paintFlags or Paint.STRIKE_THRU_TEXT_FLAG
            holder.itemView.setOnClickListener {
                onItemClick.invoke(album.getId(), album.formatData, true)
            }

        } else {
            holder.mCompletedImageView.visibility = View.GONE
            holder.mCompletedView.visibility = View.GONE
            holder.mDateTextView.setTextColor(ContextCompat.getColor(context, R.color.text_main))
            holder.mDateTextView.paintFlags =
                holder.mDateTextView.paintFlags and (Paint.STRIKE_THRU_TEXT_FLAG.inv())
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
        var mCoverImageView: ImageView = itemView.findViewById(R.id.iv_cover)
        var mDateTextView: TextView = itemView.findViewById(R.id.tv_date)
        var mProgressTextView: TextView = itemView.findViewById(R.id.tv_progress)
        var mProgressIndicator: DualProgressIndicator = itemView.findViewById(R.id.lp_progress)
        var mCompletedImageView: ImageView = itemView.findViewById(R.id.iv_completed)
        var mCompletedView: View = itemView.findViewById(R.id.v_completed)
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
            val newAlbum = newList[newItemPosition];
            val oldAlbum = oldList[oldItemPosition];

            return newAlbum.getTotalCount() == oldAlbum.getTotalCount() &&
                    newAlbum.getCompletedCount() == oldAlbum.getCompletedCount() &&
                    newAlbum.getOperatedIndex() == oldAlbum.getOperatedIndex();
        }

    }
}