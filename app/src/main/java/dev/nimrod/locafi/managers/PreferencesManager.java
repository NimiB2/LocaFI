package dev.nimrod.locafi.managers;

import android.content.Context;
import android.content.SharedPreferences;

public class PreferencesManager {
    private static final String PREFS_NAME = "LocaFiPrefs";
    private static final String KEY_FIRST_TIME_PERMISSION = "first_time_permission";
    private static final String KEY_LOCATION_ENABLED = "location_enabled";
    private static final String KEY_PERMISSION_STATE = "permission_state";

    public static boolean isFirstTimePermissionRequest(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_FIRST_TIME_PERMISSION, true);
    }

    public static void setFirstTimePermissionRequest(Context context, boolean isFirst) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_FIRST_TIME_PERMISSION, isFirst).apply();
    }

    public static void setLocationEnabled(Context context, boolean enabled) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_LOCATION_ENABLED, enabled).apply();
    }

    public static boolean isLocationEnabled(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_LOCATION_ENABLED, false);
    }

    public static void setPermissionState(Context context, String state) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putString(KEY_PERMISSION_STATE, state).apply();
    }

    public static String getPermissionState(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        return prefs.getString(KEY_PERMISSION_STATE, "NA");
    }
}