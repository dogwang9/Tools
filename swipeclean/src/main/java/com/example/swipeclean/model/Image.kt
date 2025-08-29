package com.example.swipeclean.model

import android.net.Uri
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import com.example.swipeclean.other.Constants.IMAGE_DELETE
import com.example.swipeclean.other.Constants.IMAGE_KEEP
import com.example.swipeclean.other.Constants.IMAGE_NOT_OPERATION

@Entity(tableName = "image")
data class Image(
    val sourceId: Long,
    val size: Long,
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var tag: Int = IMAGE_NOT_OPERATION
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

    fun isOperated(): Boolean = tag != IMAGE_NOT_OPERATION

    fun isDelete(): Boolean = tag == IMAGE_DELETE

    fun isKeep(): Boolean = tag == IMAGE_KEEP

    fun cancelOperated() {
        tag = IMAGE_NOT_OPERATION
    }

    fun doDelete() {
        tag = IMAGE_DELETE
    }

    fun doKeep() {
        tag = IMAGE_KEEP
    }
}
