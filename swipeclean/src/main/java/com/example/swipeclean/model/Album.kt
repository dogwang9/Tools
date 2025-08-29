package com.example.swipeclean.model

import android.net.Uri

data class Album(
    val images: MutableList<Image>,
    val formatData: String
) {

    fun getCoverUri(): Uri? {
        if (images.isEmpty()) return null
        val image = images.firstOrNull { !it.isOperated() }
        return image?.sourceUri ?: images.last().sourceUri
    }

    fun getId(): Long = formatData.hashCode().toLong()

    fun getDateTime(): Long = images.firstOrNull()?.date ?: 0L

    fun getTotalCount(): Int = images.size

    fun getCompletedCount(): Int = images.count { it.isKeep() }

    fun getOperatedIndex(): Int = images.count { it.isOperated() }

    fun isCompleted(): Boolean = getTotalCount() == getCompletedCount()

    fun isOperated(): Boolean = getTotalCount() == getOperatedIndex()
}

