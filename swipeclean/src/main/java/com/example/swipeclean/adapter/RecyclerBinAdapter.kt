package com.example.swipeclean.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.swipeclean.model.Photo
import com.example.tools.R

class RecyclerBinAdapter(
    val photos: MutableList<Photo>,
    val onItemRestoreClick: (photo: Photo, position: Int) -> Unit,
    val onItemClick: (photo: Photo, position: Int) -> Unit
) : RecyclerView.Adapter<RecyclerBinAdapter.MyViewHolder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): MyViewHolder {
        val view =
            LayoutInflater.from(parent.context)
                .inflate(R.layout.grid_item_recyclebin, parent, false)
        return MyViewHolder(view)
    }

    override fun onBindViewHolder(
        holder: MyViewHolder,
        position: Int
    ) {
        val photo = photos[position]
        val context = holder.itemView.context

        Glide
            .with(context)
            .load(photo.sourceUri)
            .placeholder(R.drawable.ic_vector_image)
            .into(holder.mPhotoImageView);

        holder.mKeepView.setOnClickListener {
            onItemRestoreClick.invoke(photo, position)
        }
        holder.itemView.setOnClickListener {
            onItemClick.invoke(photo, position)
        }
    }

    override fun getItemCount(): Int {
        return photos.size
    }

    fun removePhoto(photo: Photo) {
        photos.remove(photo)
    }

    fun getTotalSize(): Long {
        return photos.sumOf { item -> item.size }
    }


    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val mPhotoImageView: ImageView = itemView.findViewById(R.id.iv_photo)
        val mKeepView: ImageView = itemView.findViewById(R.id.iv_keep)
    }
}