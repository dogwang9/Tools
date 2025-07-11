package com.example.swipeclean.business;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import androidx.annotation.WorkerThread;

import com.example.swipeclean.model.Photo;

import java.util.HashSet;
import java.util.Set;


public class CompletedPhotoDao extends BaseDao {

    public CompletedPhotoDao(Context context) {
        super(context, SwipeCleanDBHelper.getInstance(context));
    }

    @WorkerThread
    public Set<Long> getDeletePhotoIds() {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        Set<Long> photoIds = new HashSet<>();

        try (Cursor cursor = db.query(CompletedPhotoTable.TABLE_NAME,
                null, CompletedPhotoTable.Columns.DELETED + " = ?", new String[]{"1"}, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    photoIds.add(cursor.getLong(cursor.getColumnIndexOrThrow(CompletedPhotoTable.Columns.SOURCE_ID)));
                } while (cursor.moveToNext());
            }
        }

        return photoIds;
    }

    @WorkerThread
    public Set<Long> getKeepPhotoIds() {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        Set<Long> photoIds = new HashSet<>();

        try (Cursor cursor = db.query(CompletedPhotoTable.TABLE_NAME,
                null, CompletedPhotoTable.Columns.DELETED + " = ?", new String[]{"0"}, null, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                do {
                    photoIds.add(cursor.getLong(cursor.getColumnIndexOrThrow(CompletedPhotoTable.Columns.SOURCE_ID)));
                } while (cursor.moveToNext());
            }
        }

        return photoIds;
    }

    @WorkerThread
    public void deleteCompletedPhoto(long id) {
        getDbHelper().getWritableDatabase().delete(CompletedPhotoTable.TABLE_NAME,
                CompletedPhotoTable.Columns.SOURCE_ID + " = ?",
                new String[]{String.valueOf(id)});
    }

    @WorkerThread
    public void addDeletePhoto(Photo photo) {
        ContentValues values = new ContentValues();
        values.put(CompletedPhotoTable.Columns.SOURCE_PATH, photo.getPath());
        values.put(CompletedPhotoTable.Columns.SOURCE_ID, photo.getId());
        values.put(CompletedPhotoTable.Columns.SIZE, photo.getSize());
        values.put(CompletedPhotoTable.Columns.DELETED, 1);
        getDbHelper().getWritableDatabase().insert(CompletedPhotoTable.TABLE_NAME,
                null,
                values);
    }

    @WorkerThread
    public void addKeepPhoto(Photo photo) {
        ContentValues values = new ContentValues();
        values.put(CompletedPhotoTable.Columns.SOURCE_PATH, photo.getPath());
        values.put(CompletedPhotoTable.Columns.SOURCE_ID, photo.getId());
        values.put(CompletedPhotoTable.Columns.SIZE, photo.getSize());
        values.put(CompletedPhotoTable.Columns.DELETED, 0);
        getDbHelper().getWritableDatabase().insert(CompletedPhotoTable.TABLE_NAME,
                null,
                values);
    }

    @WorkerThread
    public void converseDeleteToKeepPhoto(long id) {
        ContentValues values = new ContentValues();
        values.put(CompletedPhotoTable.Columns.DELETED, 0);
        getDbHelper().getWritableDatabase().update(CompletedPhotoTable.TABLE_NAME, values
                , CompletedPhotoTable.Columns.SOURCE_ID + " = ?", new String[]{String.valueOf(id)});
    }

}
