package com.example.swipeclean.business;

import android.content.Context;


public class SwipeCleanDBHelper extends BaseDBHelper {

    private static final String DATABASE_NAME = "swipe_clean.db";

    private static final int DB_VERSION = 1;

    private static volatile SwipeCleanDBHelper gInstance;

    private SwipeCleanDBHelper(Context context, String databaseName, int version) {
        super(context, databaseName, version);
    }

    public static SwipeCleanDBHelper getInstance(Context context) {
        if (gInstance == null) {
            synchronized (SwipeCleanDBHelper.class) {
                if (gInstance == null) {
                    gInstance = new SwipeCleanDBHelper(context, DATABASE_NAME, DB_VERSION);
                }
            }
        }
        return gInstance;
    }

    @Override
    protected void registerTables() {
        addTable(new CompletedPhotoTable());
    }

    @Override
    protected void registerViews() {

    }
}
