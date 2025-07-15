package com.example.lib.utils;

import android.annotation.SuppressLint;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaScannerConnection;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;

import androidx.annotation.WorkerThread;
import androidx.core.content.ContextCompat;

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

public class MediaStoreUtils {

    public static final int MEDIA_SCAN_BATCH_SIZE = 100;

    private static int sVideoThumbnailType = -1;

    public static Bitmap createVideoThumbnail(String path) {
        return ThumbnailUtils.createVideoThumbnail(path,
                getVideoThumbnailType());
    }

    public static Set<String/*thumbnail path*/> getThumbnailPaths(Context context) {
        Set<String> thumbnailPaths = new HashSet<>();
        Context appContext = context.getApplicationContext();
        String[] projection = {MediaStore.Images.Thumbnails.DATA};
        try (Cursor cursor = appContext.getContentResolver()
                .query(MediaStore.Images.Thumbnails.EXTERNAL_CONTENT_URI, projection,
                        null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int dataIndex = cursor.getColumnIndex(MediaStore.Images.Thumbnails.DATA);
                do {
                    String path = cursor.getString(dataIndex);
                    thumbnailPaths.add(path);


                } while (cursor.moveToNext());
            }

        } catch (Exception e) {

        }
        return thumbnailPaths;
    }

    /**
     * Note:Do not call this method frequently
     */
    public static void scan(Context context, String... paths) {
        scan(context, null, paths);
    }

    public static void scan(Context context, List<String> paths) {
        scan(context, null, paths.toArray(new String[0]));
    }

    public static void scan(Context context, MediaScannerConnection.OnScanCompletedListener listener, String... paths) {
        for (String path : paths) {

        }
        MediaScannerConnection.scanFile(context,
                paths, null,
                (path, uri) -> {

                    if (listener != null) {
                        listener.onScanCompleted(path, uri);
                    }
                });
    }

    @WorkerThread
    public static void scanSync(Context context, Runnable onAllFilesScanCompleted, String... paths) {
        CountDownLatch countDownLatch = new CountDownLatch(paths.length);
        scan(context, (path, uri) -> countDownLatch.countDown(), paths);

        try {
            if (!countDownLatch.await(2, TimeUnit.SECONDS)) {

            } else {
                if (onAllFilesScanCompleted != null) {
                    onAllFilesScanCompleted.run();
                }
            }
        } catch (InterruptedException e) {

        }
    }

    @WorkerThread
    public static void scanSync(Context context, String... paths) {
        scanSync(context, null, paths);
    }

    @WorkerThread
    public static void scanSync(Context context, List<String> paths) {
        scanSync(context, paths.toArray(new String[0]));
    }

    @WorkerThread
    public static void scanSync(Context context, List<String> paths, Runnable onAllFilesScanCompleted) {
        scanSync(context, onAllFilesScanCompleted, paths.toArray(new String[0]));
    }

    public static void scan(Context context, Uri uri) {
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(uri);
        context.sendBroadcast(intent);

    }

    private static int getVideoThumbnailType() {
        if (sVideoThumbnailType == -1) {
            sVideoThumbnailType = MediaStore.Video.Thumbnails.MINI_KIND;
        }
        return sVideoThumbnailType;
    }

    public static String getSecondaryExternalStoragePath(Context context) {
        try {
            File[] files = ContextCompat.getExternalFilesDirs(
                    context, null);

            if (files.length > 0) {

                List<String> paths = new ArrayList<>();

                for (File file : files) {

                    String path = file.getAbsolutePath();
                    int index = path.indexOf("Android/data/" + context.getPackageName() + "/files");
                    if (index > 0) {
                        paths.add(path.substring(0, index - 1));
                    }
                }

                if (paths.size() > 1) {
                    return paths.get(1);
                }
            }

        } catch (Exception ignored) {
        }
        return null;
    }

    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri     The Uri to query.
     */
    @SuppressLint("NewApi")
    public static String getPath(Context context, final Uri uri) {

        // DocumentProvider
        String uriStr = uri.toString();
        if (uriStr.startsWith("content://") && uriStr.contains("/external_files/")) {
            int indexOfExternalFiles = uriStr.indexOf("/external_files/");
            if (indexOfExternalFiles > 0) {
                String filePath = Environment.getExternalStorageDirectory().toString() + "/" + uriStr.substring(indexOfExternalFiles + 16);

                return filePath;
            }
        }
        if (DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/"
                            + split[1];
                } else {
                    String sdcardPath = getSecondaryExternalStoragePath(context);
                    if (sdcardPath == null) {
                        return null;
                    }
                    return sdcardPath + "/"
                            + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"),
                        Long.parseLong(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;

                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;

                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[]{split[1]};

                return getDataColumn(context, contentUri, selection,
                        selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {
            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     */
    private static String getDataColumn(Context context, Uri uri,
                                        String selection, String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {column};

        try {
            cursor = context.getContentResolver().query(uri, projection,
                    selection, selectionArgs, null);
            if (cursor != null && cursor.moveToFirst()) {
                final int column_index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(column_index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }

    private static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri
                .getAuthority());
    }

    private static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri
                .getAuthority());
    }

    private static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri
                .getAuthority());
    }

}
