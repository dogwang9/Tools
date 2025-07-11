package com.example.swipeclean.model

data class Album(
    val photos: MutableList<Photo>,
    val formatData: String
) {

    fun getCoverPath(): String? {
        if (photos.isEmpty()) return null
        val photo = photos.firstOrNull { !it.isOperated() }
        return photo?.path ?: photos.last().path
    }

    fun getId(): Long = formatData.hashCode().toLong()

    fun getDateTime(): Long = photos.firstOrNull()?.date ?: 0L

    fun getTotalCount(): Int = photos.size

    fun getCompletedCount(): Int = photos.count { it.isKeep }

    fun getOperatedIndex(): Int = photos.count { it.isOperated() }

    fun isCompleted(): Boolean = getTotalCount() == getCompletedCount()

    fun isOperated(): Boolean = getTotalCount() == getOperatedIndex()

    fun clone(album: Album): Album {
        val clonedPhotos = album.photos.map { it.clone(it) }.toMutableList()
        return Album(clonedPhotos, album.formatData)
    }
}

