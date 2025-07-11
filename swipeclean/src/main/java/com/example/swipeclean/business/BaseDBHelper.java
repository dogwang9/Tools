package com.example.swipeclean.business;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteException;
import android.database.sqlite.SQLiteOpenHelper;

import java.util.LinkedList;
import java.util.List;

public abstract class BaseDBHelper extends SQLiteOpenHelper {

    private static final boolean DEVELOPING_MODE = false;

    public interface IDatabaseTable {
        void onCreate(final SQLiteDatabase db);

        void onPostCreate(final SQLiteDatabase db);

        void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion);

        void onPostUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion);

        void onInsertTestData(final SQLiteDatabase db);
    }

    public interface IDatabaseView {
        void onCreate(final SQLiteDatabase db);

        void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion);
    }

    @SuppressWarnings({"UnusedReturnValue", "SameParameterValue"})
    public static abstract class BaseDataBaseTable implements IDatabaseTable {

        @Override
        public void onPostCreate(final SQLiteDatabase db) {
            // normally, nothing need to do
            // override this function to create TRIGGERs
        }

        @Override
        public void onPostUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {
        }

        @Override
        public void onInsertTestData(final SQLiteDatabase db) {
            // only be triggered in test mode
        }

        protected void alterTableForUpgrade(SQLiteDatabase db, String sql) {
            try {
                db.execSQL(sql);

            } catch (SQLiteException ex) {
                if (ex.getMessage() != null && ex.getMessage().contains("duplicate column name")) {

                } else {
                    throw ex;
                }
            }
        }
    }

    public static abstract class BaseDataBaseView implements IDatabaseView {

        @Override
        public void onCreate(final SQLiteDatabase db) {

        }

        @Override
        public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {

        }
    }

    private final List<IDatabaseTable> mTables = new LinkedList<>();
    private final List<IDatabaseView> mViews = new LinkedList<>();

    /**
     * each table should register itself statically
     */
    protected void addTable(IDatabaseTable table) {
        mTables.add(table);
    }

    /**
     * each view should register itself statically
     */
    protected void addView(IDatabaseView view) {
        mViews.add(view);
    }

    protected abstract void registerTables();

    protected abstract void registerViews();

    public BaseDBHelper(final Context context, final String databaseName, final int version) {
        super(context, databaseName, null, version);
        registerTables();
        registerViews();
    }

    @Override
    public void onOpen(final SQLiteDatabase db) {
        super.onOpen(db);
        if (!db.isReadOnly()) {
            enableForeignKey(db);
        }
    }

    /**
     * versions of SQLite older than 3.6.19 don't support foreign keys
     * and neither do any version compiled with SQLITE_OMIT_FOREIGN_KEY
     * http://www.sqlite.org/foreignkeys.html#fk_enable
     * <p>
     * make sure foreign key support is turned on if it's there
     * (should be already, just a double-checker)
     */
    private void enableForeignKey(final SQLiteDatabase db) {
        db.execSQL("PRAGMA foreign_keys=ON;");

        // then we check to make sure they're on
        // (if this returns no data they aren't even available, so we shouldn't even TRY to use them)
        Cursor c = db.rawQuery("PRAGMA foreign_keys", null);
        if (c.moveToFirst()) {
            int result = c.getInt(0);


        } else {
            // could use this approach in onCreate, and not rely on foreign keys it not available, etc.

            // if you had to here you could fall back to triggers
        }

        if (!c.isClosed()) {
            c.close();
        }
    }

    @Override
    public void onCreate(final SQLiteDatabase db) {

        for (IDatabaseTable t : mTables) {
            t.onCreate(db);
        }

        for (IDatabaseView v : mViews) {
            v.onCreate(db);
        }

        for (IDatabaseTable t : mTables) {
            t.onPostCreate(db);
        }

        if (DEVELOPING_MODE) {
            for (IDatabaseTable t : mTables) {
                t.onInsertTestData(db);
            }
        }
    }

    @Override
    public void onUpgrade(final SQLiteDatabase db, final int oldVersion, final int newVersion) {


        for (IDatabaseTable t : mTables) {
            t.onUpgrade(db, oldVersion, newVersion);
        }

        for (IDatabaseView v : mViews) {
            v.onUpgrade(db, oldVersion, newVersion);
        }

        beforeFinishUpgrading(db, oldVersion, newVersion);

        for (IDatabaseTable t : mTables) {
            t.onPostUpgrade(db, oldVersion, newVersion);
        }
    }

    protected void beforeFinishUpgrading(final SQLiteDatabase db, final int oldVersion, final int newVersion) {

    }
}