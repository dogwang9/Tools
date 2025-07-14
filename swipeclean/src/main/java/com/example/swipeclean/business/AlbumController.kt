package com.example.swipeclean.business

import android.content.Context
import android.provider.MediaStore
import androidx.annotation.WorkerThread
import com.example.swipeclean.model.Album
import com.example.swipeclean.model.Photo
import java.text.SimpleDateFormat
import java.util.Locale

object AlbumController {

    private lateinit var mAppContext: Context
    private lateinit var mAlbums: ArrayList<Album>
    private lateinit var mCompletedPhotoDao: CompletedPhotoDao

    fun init(applicationContext: Context) {
        mAppContext = applicationContext
        mAlbums = ArrayList<Album>()
        mCompletedPhotoDao = CompletedPhotoDao(mAppContext)
    }

    fun getAlbums(): List<Album> {
        return mAlbums
    }

    @WorkerThread
    fun addDeletePhoto(photo: Photo) {
        mCompletedPhotoDao.addDeletePhoto(photo)
    }

    @WorkerThread
    fun addKeepPhoto(photo: Photo) {
        mCompletedPhotoDao.addKeepPhoto(photo)
    }

    @WorkerThread
    fun cleanCompletedPhoto(photo: Photo) {
        mCompletedPhotoDao.deleteCompletedPhoto(photo.id)
    }

    @WorkerThread
    fun converseDeleteToKeepPhoto(photo: Photo) {
        mCompletedPhotoDao.converseDeleteToKeepPhoto(photo.id)
    }

    @WorkerThread
    fun loadAlbums(): List<Album> {
        mAlbums.clear()

        val deleteIds = mCompletedPhotoDao.getDeletePhotoIds()
        val keepIds = mCompletedPhotoDao.getKeepPhotoIds()

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
            while (cursor.moveToNext()) {
                val id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID))
                val dateAdded =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED))
                val dateTaken =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN))
                val size =
                    cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE))
                val width =
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH))
                val height =
                    cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT))
                val displayName =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME))
                val path =
                    cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA))

                val isKeep = keepIds.contains(id)
                val isDelete = !isKeep && deleteIds.contains(id)


                val photo = Photo(
                    id,
                    displayName,
                    path,
                    if (dateTaken == 0L) dateAdded * 1000 else dateTaken,
                    width,
                    height,
                    size,
                    isKeep,
                    isDelete
                );
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

        return mAlbums;
    }

    @WorkerThread
    fun syncDatabase() {
        val allPhotoIds = HashSet<Long>()
        val operationIds = HashSet<Long>()

        operationIds.addAll(mCompletedPhotoDao.getDeletePhotoIds())
        operationIds.addAll(mCompletedPhotoDao.getKeepPhotoIds())

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
            mCompletedPhotoDao.deleteCompletedPhoto(id)
        }
    }

}