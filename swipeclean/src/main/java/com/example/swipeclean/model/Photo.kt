package com.example.swipeclean.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.example.swipeclean.other.Constants

@Entity(tableName = "photo")
data class Photo(
    val sourceId: Long,
    val size: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var tag: Int = Constants.PHOTO_NOT_OPERATION
) {
    @Ignore
    var displayName: String = ""

    @Ignore
    var sourceUri: Uri? = null

    @Ignore
    var date: Long = 0

    @Ignore
    var width: Int = 0

    @Ignore
    var height: Int = 0

    fun isOperated(): Boolean = tag != Constants.PHOTO_NOT_OPERATION

    fun isDelete(): Boolean = tag == Constants.PHOTO_DELETE

    fun isKeep(): Boolean = tag == Constants.PHOTO_KEEP

    fun cancelOperated() {
        tag = Constants.PHOTO_NOT_OPERATION
    }

    fun doDelete() {
        tag = Constants.PHOTO_DELETE
    }

    fun doKeep() {
        tag = Constants.PHOTO_KEEP
    }

    fun clone(photo: Photo): Photo {
        return Photo(
            photo.sourceId,
            photo.size,
            photo.id,
            photo.tag
        ).apply {
            this.displayName = photo.displayName
            this.date = photo.date
            this.width = photo.width
            this.height = photo.height
            this.sourceUri = photo.sourceUri
        }
    }
}
