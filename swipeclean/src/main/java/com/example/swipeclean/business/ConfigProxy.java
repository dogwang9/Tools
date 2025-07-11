package com.example.swipeclean.business;

import android.annotation.SuppressLint;
import android.app.ActivityManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Set;

public class ConfigProxy {

    private final String mConfigFileName;
    private String mTargetProcessName = null;
    private String mCurrentProcessNameCache = null;

    public ConfigProxy(String configFileName) {
        mConfigFileName = configFileName;
    }

    public ConfigProxy(String configFileName, String targetProcessName) {
        mConfigFileName = configFileName;
        mTargetProcessName = targetProcessName;
    }

    private void checkTargetProcess(Context context) {
        if (mTargetProcessName != null) {
            if (mCurrentProcessNameCache == null) {
                mCurrentProcessNameCache = getCurrentProcessName(context);
            }

            if (!mTargetProcessName.equals(mCurrentProcessNameCache)) {
                Log.e("ConfigProxy", "Not target process! CurrentProcess: " + mCurrentProcessNameCache + ", Target: " + mTargetProcessName +
                        Log.getStackTraceString(new Exception()));
            }
        }
    }

    private String getCurrentProcessName(Context context) {
        String processName = null;
        BufferedReader cmdlineReader = null;
        try {
            cmdlineReader = new BufferedReader(new InputStreamReader(
                    new FileInputStream(
                            "/proc/" + android.os.Process.myPid() + "/cmdline"),
                    StandardCharsets.ISO_8859_1));
            int c;
            StringBuilder sb = new StringBuilder();
            while ((c = cmdlineReader.read()) > 0) {
                sb.append((char) c);
            }
            processName = sb.toString();

        } catch (IOException e) {

        } finally {
            try {
                if (cmdlineReader!=null){
                    cmdlineReader.close();
                }
            } catch (IOException e) {

            }
        }

        if (TextUtils.isEmpty(processName)) {
            int myPid = android.os.Process.myPid();
            ActivityManager activityManager = (ActivityManager) context.getSystemService(Context.ACTIVITY_SERVICE);
            if (activityManager != null) {
                List<?> runningProcesses = activityManager.getRunningAppProcesses();
                if (runningProcesses != null) {
                    for (Object runningProcess : runningProcesses) {
                        ActivityManager.RunningAppProcessInfo process = (ActivityManager.RunningAppProcessInfo) runningProcess;
                        if (process.pid == myPid) {
                            processName = process.processName;
                            break;
                        }
                    }
                }
            }
        }
        return processName;
    }

    @SuppressLint("SdCardPath")
    public File getConfigFile(Context context) {
        return new File("/data/data/" + context.getPackageName() + "/shared_prefs/" + mConfigFileName + ".xml");
    }

    public String getValue(Context context, String key, String defaultValue) {
        checkTargetProcess(context);

        SharedPreferences preferences = context.getSharedPreferences(mConfigFileName,
                Context.MODE_PRIVATE);
        if (preferences == null) {
            return defaultValue;
        }

        return preferences.getString(key, defaultValue);
    }

    public boolean setValue(Context context, String key, String value) {
        checkTargetProcess(context);

        SharedPreferences.Editor editor = createPreferenceEditor(context);
        if (editor == null) {
            return false;
        }

        editor.putString(key, value);
        editor.apply();
        return true;
    }

    // boolean
    public boolean getValue(Context context, String key, boolean defaultValue) {
        checkTargetProcess(context);

        SharedPreferences preferences = context.getSharedPreferences(mConfigFileName,
                Context.MODE_PRIVATE);
        if (preferences == null) {
            return defaultValue;
        }

        return preferences.getBoolean(key, defaultValue);
    }

    public boolean setValue(Context context, String key, boolean value) {
        checkTargetProcess(context);

        SharedPreferences.Editor editor = createPreferenceEditor(context);
        if (editor == null) {
            return false;
        }

        editor.putBoolean(key, value);
        editor.apply();
        return true;
    }

    // long
    public long getValue(Context context, String key, long defaultValue) {
        checkTargetProcess(context);

        SharedPreferences preferences = context.getSharedPreferences(mConfigFileName,
                Context.MODE_PRIVATE);
        if (preferences == null) {
            return defaultValue;
        }

        return preferences.getLong(key, defaultValue);
    }

    public boolean setValue(Context context, String key, long value) {
        checkTargetProcess(context);

        SharedPreferences.Editor editor = createPreferenceEditor(context);
        if (editor == null) {
            return false;
        }

        editor.putLong(key, value);
        editor.apply();
        return true;
    }

    // int
    public int getValue(Context context, String key, int defaultValue) {
        checkTargetProcess(context);

        SharedPreferences preferences = context.getSharedPreferences(mConfigFileName,
                Context.MODE_PRIVATE);
        if (preferences == null) {
            return defaultValue;
        }

        return preferences.getInt(key, defaultValue);
    }

    public boolean setValue(Context context, String key, int value) {
        checkTargetProcess(context);

        SharedPreferences.Editor editor = createPreferenceEditor(context);
        if (editor == null) {
            return false;
        }

        editor.putInt(key, value);
        editor.apply();
        return true;
    }

    // float
    public float getValue(Context context, String key, float defaultValue) {
        checkTargetProcess(context);

        SharedPreferences preferences = context.getSharedPreferences(mConfigFileName,
                Context.MODE_PRIVATE);
        if (preferences == null) {
            return defaultValue;
        }

        return preferences.getFloat(key, defaultValue);
    }

    public boolean setValue(Context context, String key, float value) {
        checkTargetProcess(context);

        SharedPreferences.Editor editor = createPreferenceEditor(context);
        if (editor == null) {
            return false;
        }

        editor.putFloat(key, value);
        editor.apply();
        return true;
    }

    public double getValue(Context context, String key, double defaultValue) {
        checkTargetProcess(context);

        SharedPreferences preferences = context.getSharedPreferences(mConfigFileName,
                Context.MODE_PRIVATE);
        if (preferences == null) {
            return defaultValue;
        }

        String ret = preferences.getString(key, null);
        if (ret == null) {
            return defaultValue;
        }

        return Double.parseDouble(ret);
    }

    public boolean setValue(Context context, String key, double value) {
        checkTargetProcess(context);

        SharedPreferences.Editor editor = createPreferenceEditor(context);
        if (editor == null) {
            return false;
        }

        editor.putString(key, String.valueOf(value));
        editor.apply();
        return true;
    }

    // String Set
    public Set<String> getStringSet(Context context, String key, Set<String> defaultValue) {
        checkTargetProcess(context);

        SharedPreferences preferences = context.getSharedPreferences(mConfigFileName,
                Context.MODE_PRIVATE);
        if (preferences == null) {
            return defaultValue;
        }

        return preferences.getStringSet(key, defaultValue);
    }

    public boolean setStringSet(Context context, String key, Set<String> value) {
        checkTargetProcess(context);

        SharedPreferences.Editor editor = createPreferenceEditor(context);
        if (editor == null) {
            return false;
        }

        editor.putStringSet(key, value);
        editor.apply();
        return true;
    }

    public boolean remove(Context context, String key) {
        checkTargetProcess(context);

        SharedPreferences.Editor editor = createPreferenceEditor(context);
        if (editor == null) {
            return false;
        }

        editor.remove(key);
        editor.apply();
        return true;
    }

    private SharedPreferences.Editor createPreferenceEditor(Context context) {
        checkTargetProcess(context);

        SharedPreferences preferences = context.getSharedPreferences(mConfigFileName,
                Context.MODE_PRIVATE);
        if (preferences == null) {
            return null;
        }

        return preferences.edit();
    }

    public void clearData(Context context) {
        checkTargetProcess(context);

        SharedPreferences.Editor editor = createPreferenceEditor(context);
        if (editor == null) {
            return;
        }

        editor.clear();
        editor.commit();
    }

    public void flush(Context context) {
        checkTargetProcess(context);

        SharedPreferences.Editor editor = createPreferenceEditor(context);
        if (editor == null) {
            return;
        }

        editor.commit();
    }

    public boolean doesKeyExist(Context context, String key) {
        SharedPreferences preferences = context.getSharedPreferences(mConfigFileName,
                Context.MODE_PRIVATE);

        if (preferences == null) {
            return false;
        }

        return preferences.contains(key);
    }
}
