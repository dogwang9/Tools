package com.example.swipeclean.business;

import android.content.Context;

public class SwipeCleanConfigHost {

    private static final String CONFIG_FILE_NAME = "swipe_clean";
    private static final ConfigProxy gConfigProxy = new ConfigProxy(CONFIG_FILE_NAME);

    private static final String KEY_CLEANED_SIZE = "cleaned_size";
    private static final String KEY_LAST_ENTER_SWIPE_CLEAN_TIME = "last_enter_swipe_clean_time";
    private static final String KEY_IS_SWIPE_CLEAN_GUIDANCE_ENABLED = "is_swipe_clean_guidance_enable";

    private SwipeCleanConfigHost() {
    }

    public static long getCleanedSize(Context context) {
        return gConfigProxy.getValue(context, KEY_CLEANED_SIZE, 0L);
    }

    public static void setCleanedSize(long size, Context context) {
        gConfigProxy.setValue(context, KEY_CLEANED_SIZE, getCleanedSize(context) + size);
    }

    public static long getLastEnterSwipeCleanTime(Context context) {
        return gConfigProxy.getValue(context, KEY_LAST_ENTER_SWIPE_CLEAN_TIME, 0L);
    }

    public static void setLastEnterSwipeCleanTime(Context context, long time) {
        gConfigProxy.setValue(context, KEY_LAST_ENTER_SWIPE_CLEAN_TIME, time);
    }

    public static boolean getSwipeCleanGuidanceEnable(Context context) {
        boolean enable = gConfigProxy.getValue(context, KEY_IS_SWIPE_CLEAN_GUIDANCE_ENABLED, true);
        gConfigProxy.setValue(context, KEY_IS_SWIPE_CLEAN_GUIDANCE_ENABLED, false);
        return enable;
    }
}
