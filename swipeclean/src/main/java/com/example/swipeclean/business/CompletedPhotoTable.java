package com.example.swipeclean.business;

import android.database.sqlite.SQLiteDatabase;
import android.provider.BaseColumns;


public class CompletedPhotoTable extends BaseDBHelper.BaseDataBaseTable {

    public static final String TABLE_NAME = "completed_photo";

    public static class Columns implements BaseColumns {
        public static final String SOURCE_PATH = "source_path";
        public static final String DELETED = "deleted";
        public static final String SOURCE_ID = "source_id";
        public static final String SIZE = "size";
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE IF NOT EXISTS `" + TABLE_NAME + "` ("
                + Columns._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, "
                + Columns.SOURCE_PATH + " TEXT NOT NULL UNIQUE, "
                + Columns.DELETED + " INTEGER NOT NULL, "
                + Columns.SIZE + " LONG NOT NULL, "
                + Columns.SOURCE_ID + " INTEGER NOT NULL UNIQUE )");

        db.execSQL("CREATE INDEX IF NOT EXISTS completedPhotoIndex ON " + TABLE_NAME + " (" + Columns.SOURCE_ID + ");");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }
}
