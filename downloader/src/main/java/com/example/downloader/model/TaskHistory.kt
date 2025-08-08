package com.example.downloader.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.Ignore
import androidx.room.PrimaryKey
import kotlinx.parcelize.IgnoredOnParcel

@kotlinx.parcelize.Parcelize
@Entity(tableName = "task")
data class TaskHistory(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    var duration: Int = 0,
    var downloadTime: Long = 0,
    var uploadDate: String? = null,
    var source: String? = null,
    var title: String? = null,
    var thumbnail: String? = null,
    var uploader: String? = null,
    var url: String? = null,
    var path: String? = null,
) : Parcelable {
    @IgnoredOnParcel
    @Ignore
    var available: Boolean = true

    @IgnoredOnParcel
    @Ignore
    var size: Long = 0
}