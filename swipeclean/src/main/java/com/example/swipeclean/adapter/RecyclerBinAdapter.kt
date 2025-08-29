package com.example.swipeclean.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.example.swipeclean.model.Image
import com.example.tools.R

class RecyclerBinAdapter(
    val images: MutableList<Image>,
    val onItemRestoreClick: (image: Image, position: Int) -> Unit,
    val onItemClick: (photoImageView: ImageView, image: Image, position: Int) -> Unit
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
        val image = images[position]

        Glide
            .with(holder.itemView.context)
            .load(image.sourceUri)
            .placeholder(R.drawable.ic_vector_image)
            .into(holder.coverImageView)

        holder.keepView.setOnClickListener {
            onItemRestoreClick.invoke(image, holder.bindingAdapterPosition)
        }
        holder.itemView.setOnClickListener {
            onItemClick.invoke(holder.coverImageView, image, holder.bindingAdapterPosition)
        }
    }

    override fun getItemCount(): Int {
        return images.size
    }

    fun removeImage(image: Image) {
        images.remove(image)
    }

    fun getTotalSize(): Long {
        return images.sumOf { item -> item.size }
    }

    class MyViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val coverImageView: ImageView = itemView.findViewById(R.id.iv_cover)
        val keepView: ImageView = itemView.findViewById(R.id.iv_keep)
    }
}