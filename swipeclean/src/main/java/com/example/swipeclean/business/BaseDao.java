package com.example.swipeclean.business;


import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public class BaseDao {

    private final BaseDBHelper mDbHelper;
    protected Context mAppContext;

    public BaseDao(Context context, BaseDBHelper dbHelper) {
        Context appContext = context.getApplicationContext();
        mDbHelper = dbHelper;
        mAppContext = appContext;
    }

    public BaseDBHelper getDbHelper() {
        return mDbHelper;
    }

    protected long getCount(String tableName, String selection, String[] selectionArgs) {
        SQLiteDatabase db = getDbHelper().getReadableDatabase();
        Cursor cursor = null;
        int count = 0;

        final String countColumnName = "item_count";
        try {
            cursor = db.query(tableName,
                    new String[]{"COUNT(*) AS " + countColumnName}, selection,
                    selectionArgs,
                    null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndex(countColumnName);
                count = cursor.getInt(columnIndex);
            }

        } finally {
            if (cursor != null) {
                cursor.close();
            }
        }

        return count;
    }
}
