package com.example.swipeclean.business

import android.content.ContentUris
import android.content.Context
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import androidx.room.Room
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Photo
import java.text.SimpleDateFormat
import java.util.Locale

object AlbumController {

    private lateinit var mAppContext: Context
    private lateinit var mAlbums: ArrayList<Album>
    private lateinit var mPhotoDao: PhotoDao

    fun init(applicationContext: Context) {
        mAppContext = applicationContext
        mAlbums = ArrayList<Album>()

        mPhotoDao = Room.databaseBuilder(applicationContext, PhotoDataBase::class.java, "PhotoDB")
            .build().photoDao()
    }

    fun getAlbums(): List<Album> {
        return mAlbums
    }

    @WorkerThread
    fun addPhoto(photo: Photo) {
        mPhotoDao.insertPhoto(photo)
    }

    @WorkerThread
    fun cleanCompletedPhoto(photo: Photo) {
        mPhotoDao.delete(photo.sourceId)
    }

    @WorkerThread
    fun converseDeleteToKeepPhoto(photo: Photo) {
        mPhotoDao.convertDeleteToKeep(photo.sourceId)
    }

    @WorkerThread
    fun loadAlbums(): ArrayList<Album> {
        mAlbums.clear()

        val deleteIds = mPhotoDao.getDeletePhotoIds()
        val keepIds = mPhotoDao.getKeepPhotoIds()

        val projection = arrayOf(
            MediaStore.Images.Media._ID,
            MediaStore.Images.Media.DISPLAY_NAME,
            MediaStore.Images.Media.DATE_ADDED,
            MediaStore.Images.Media.DATE_TAKEN,
            MediaStore.Images.Media.WIDTH,
            MediaStore.Images.Media.HEIGHT,
            MediaStore.Images.Media.DATA,
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
        cursor?.let {
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

                val photo = Photo(
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
                ).format(photo.date)

                if (!albums.containsKey(month)) {
                    albums.put(month, Album(ArrayList<Photo>(), month))
                }
                val album = albums.get(month)
                album?.apply {
                    photos.add(photo)
                }
            }
            cursor.close()
        }

        albums.forEach { _, v ->
            v.photos.sortByDescending { it.isOperated() }
            mAlbums.add(v)
        }

        return mAlbums
    }

    @WorkerThread
    fun syncDatabase() {
        val allPhotoIds = HashSet<Long>()
        val operationIds = HashSet<Long>()

        operationIds.addAll(mPhotoDao.getDeletePhotoIds())
        operationIds.addAll(mPhotoDao.getKeepPhotoIds())

        val cursor = mAppContext.contentResolver.query(
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
            arrayOf(MediaStore.Images.Media._ID),
            null,
            null,
            null
        )
        cursor?.let {
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                allPhotoIds.add(id)
            }
            cursor.close()
        }

        operationIds.removeAll(allPhotoIds)

        for (id in operationIds) {
            mPhotoDao.delete(id)
        }
    }

}