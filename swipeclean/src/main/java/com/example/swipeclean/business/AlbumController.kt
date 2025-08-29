package com.example.swipeclean.business

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.room.Room
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Image
import java.text.SimpleDateFormat
import java.util.Locale

object AlbumController {

    private lateinit var mAppContext: Context
    private lateinit var mAlbums: ArrayList<Album>
    private lateinit var mImageDao: ImageDao

    fun init(applicationContext: Context) {
        mAppContext = applicationContext
        mAlbums = ArrayList()

        mImageDao = Room.databaseBuilder(applicationContext, ImageDataBase::class.java, "PictureDB")
            .build().imageDao()
    }

    fun getAlbums(): List<Album> {
        return mAlbums
    }

    @WorkerThread
    fun addImage(image: Image) {
        mImageDao.insertImage(image)
    }

    @WorkerThread
    fun cleanCompletedImage(image: Image) {
        mImageDao.delete(image.sourceId)
    }

    @WorkerThread
    fun cleanCompletedImage(images: List<Image>) {
        mImageDao.delete(images.map { it.sourceId })
    }

    @WorkerThread
    fun converseDeleteToKeepImage(image: Image) {
        mImageDao.convertDeleteToKeep(image.sourceId)
    }

    @WorkerThread
    fun converseDeleteToKeepImage(images: List<Image>) {
        mImageDao.convertDeleteToKeep(images.map { it.sourceId })
    }

    @WorkerThread
    fun loadAlbums(): ArrayList<Album> {
        mAlbums.clear()

        val deleteIds = mImageDao.getDeleteImageIds()
        val keepIds = mImageDao.getKeepImageIds()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.SIZE
        )

        val sortOrder =
            "CASE WHEN " +
                    "${MediaStore.Images.Media.DATE_TAKEN} IS NULL OR " +
                    "${MediaStore.Images.Media.DATE_TAKEN} = 0 THEN " +
                    "${MediaStore.Images.Media.DATE_ADDED} ELSE (" +
                    "${MediaStore.Images.Media.DATE_TAKEN} / 1000) END DESC"

        val cursor = mAppContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            projection,
            null,
            null,
            sortOrder
        )

        val albums = HashMap<String, Album>()
        cursor?.use {
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            val dateAddedIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
            val dateTakenIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN)
            val sizeIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
            val widthIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
            val heightIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
            val displayNameIndex =
                cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
            while (cursor.moveToNext()) {
                val id = cursor.getLong(idIndex)
                val dateAdded = cursor.getLong(dateAddedIndex)
                val dateTaken = cursor.getLong(dateTakenIndex)
                val size = cursor.getLong(sizeIndex)
                val width = cursor.getInt(widthIndex)
                val height = cursor.getInt(heightIndex)
                val displayName = cursor.getString(displayNameIndex)
                val uri =
                    ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id)

                val image = Image(
                    id,
                    size
                ).apply {
                    this.displayName = displayName
                    this.date = if (dateTaken == 0L) dateAdded * 1000 else dateTaken
                    this.width = width
                    this.height = height
                    this.sourceUri = uri
                    if (keepIds.contains(id)) doKeep() else if (deleteIds.contains(id)) doDelete()
                }

                val month = SimpleDateFormat(
                    "MMMM, yyyy",
                    Locale.getDefault()
                ).format(image.date)

                albums.takeIf { !it.containsKey(month) }?.put(month, Album(ArrayList(), month))
                albums.get(month)?.images?.add(image)
            }
        }

        albums.forEach { _, v ->
            v.images.sortByDescending { it.isOperated() }
            mAlbums.add(v)
        }

        return mAlbums
    }

    @WorkerThread
    fun syncDatabase() {
        val allImageIds = ArrayList<Long>()
        val operationIds = ArrayList<Long>()

        operationIds.addAll(mImageDao.getDeleteImageIds())
        operationIds.addAll(mImageDao.getKeepImageIds())

        val cursor = mAppContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            null
        )
        cursor?.use {
            val idIndex = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
            while (cursor.moveToNext()) {
                allImageIds.add(cursor.getLong(idIndex))
            }
        }

        operationIds.removeAll(allImageIds)
        mImageDao.delete(operationIds)
    }

}