package com.example.swipeclean.business;

import android.content.Context;
import android.database.Cursor;
import android.provider.MediaStore;

import androidx.annotation.Nullable;
import androidx.annotation.WorkerThread;

import com.example.swipeclean.model.Album;
import com.example.swipeclean.model.Photo;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;


public class AlbumController {

    private static volatile AlbumController mInstance;
    private final Context mAppContext;
    private List<Album> mAlbums;
    private final CompletedPhotoDao mCompletedPhotoDao;

    public static AlbumController getInstance(Context appContext) {
        if (mInstance == null) {
            synchronized (AlbumController.class) {
                if (mInstance == null) {
                    mInstance = new AlbumController(appContext);
                }
            }
        }
        return mInstance;
    }

    private AlbumController(Context context) {
        mAppContext = context.getApplicationContext();
        mCompletedPhotoDao = new CompletedPhotoDao(mAppContext);
    }

    @WorkerThread
    public void addDeletePhoto(Photo photo) {
        mCompletedPhotoDao.addDeletePhoto(photo);
    }

    @WorkerThread
    public void addKeepPhoto(Photo photo) {
        mCompletedPhotoDao.addKeepPhoto(photo);
    }

    @WorkerThread
    public void cleanCompletedPhoto(Photo photo) {
        mCompletedPhotoDao.deleteCompletedPhoto(photo.getId());
    }

    @WorkerThread
    public void converseDeleteToKeepPhoto(Photo photo) {
        mCompletedPhotoDao.converseDeleteToKeepPhoto(photo.getId());
    }

    @WorkerThread
    public List<Album> loadAlbums() {
        mAlbums = new ArrayList<>();

        Set<Long> deleteIds = mCompletedPhotoDao.getDeletePhotoIds();
        Set<Long> keepIds = mCompletedPhotoDao.getKeepPhotoIds();

        String[] projection = {
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_TAKEN,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.DATA,
                MediaStore.Images.Media.SIZE
        };

        String sortOrder = "CASE WHEN " + MediaStore.Images.Media.DATE_TAKEN + " IS NULL OR "
                + MediaStore.Images.Media.DATE_TAKEN + " = 0 THEN "
                + MediaStore.Images.Media.DATE_ADDED + " ELSE "
                + "(" + MediaStore.Images.Media.DATE_TAKEN + " / 1000) END DESC";

        Cursor cursor = mAppContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection,
                null,
                null,
                sortOrder
        );

        Map<String, Album> albums = new HashMap<>();

        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                long dateAdded = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED));
                long dateTaken = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_TAKEN));
                long size = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE));
                int width = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH));
                int height = cursor.getInt(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT));
                String displayName = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME));
                String path = cursor.getString(cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATA));

                boolean isKeep = keepIds.contains(id);
                boolean isDelete = !isKeep && deleteIds.contains(id);

                Photo photo = new Photo(id, displayName, path, dateTaken == 0 ? dateAdded * 1000 : dateTaken, width, height, size, isKeep, isDelete);
                String month = new SimpleDateFormat("MMMM, yyyy", Locale.getDefault()).format(photo.getDate());

                if (!albums.containsKey(month)) {
                    albums.put(month, new Album(new ArrayList<>(), month));
                }
                Album album = albums.get(month);
                if (album != null) {
                    album.getPhotos().add(photo);
                }
            }
            cursor.close();
        }

        if (!albums.isEmpty()) {
            albums.forEach((k, v) -> {
                v.getPhotos().sort((o1, o2) -> Boolean.compare(o2.isOperated(), o1.isOperated()));
                mAlbums.add(v);
            });
        }

        return mAlbums;
    }

    @Nullable
    public List<Album> getAlbums() {
        return mAlbums;
    }

    @WorkerThread
    public void syncDatabase() {
        Set<Long> allPhotoIds = new HashSet<>();
        Set<Long> operationIds = new HashSet<>();

        operationIds.addAll(mCompletedPhotoDao.getDeletePhotoIds());
        operationIds.addAll(mCompletedPhotoDao.getKeepPhotoIds());

        Cursor cursor = mAppContext.getContentResolver().query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                new String[]{MediaStore.Images.Media._ID},
                null,
                null,
                null
        );
        if (cursor != null) {
            while (cursor.moveToNext()) {
                long id = cursor.getLong(cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID));
                allPhotoIds.add(id);
            }
            cursor.close();
        }

        operationIds.removeAll(allPhotoIds);

        for (long id : operationIds) {
            mCompletedPhotoDao.deleteCompletedPhoto(id);
        }
    }
}
