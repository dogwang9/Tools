package com.example.swipeclean.model

data class Photo(
    val id: Long,
    val displayName: String,
    val path: String,
    val date: Long,
    val width: Int,
    val height: Int,
    val size: Long,
    var isKeep: Boolean,
    var isDelete: Boolean
) {

    fun isOperated(): Boolean = isKeep || isDelete

    fun clone(photo: Photo): Photo {
        return Photo(
            photo.id,
            photo.displayName,
            photo.path,
            photo.date,
            photo.width,
            photo.height,
            photo.size,
            photo.isKeep,
            photo.isDelete
        )
    }

}
