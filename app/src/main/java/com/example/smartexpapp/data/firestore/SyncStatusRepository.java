package com.example.smartexpapp.data.firestore;

import android.content.Context;

public final class SyncStatusRepository {
    public static final String STATUS_LOCAL_ONLY = "LOCAL_ONLY";
    public static final String STATUS_SYNC_ENABLED = "SYNC_ENABLED";
    public static final String STATUS_SYNCING = "SYNCING";
    public static final String STATUS_SYNCED = "SYNCED";
    public static final String STATUS_ERROR = "ERROR";

    private static final String PREFS = "sync_status";
    private static final String KEY_STATUS = "status";
    private static final String KEY_UPDATED_AT = "updated_at";

    private SyncStatusRepository() {
    }

    public static String getStatus(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getString(KEY_STATUS, STATUS_LOCAL_ONLY);
    }

    public static long getUpdatedAt(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .getLong(KEY_UPDATED_AT, 0L);
    }

    public static void markLocalOnly(Context context) {
        setStatus(context, STATUS_LOCAL_ONLY);
    }

    public static void markSyncEnabled(Context context) {
        setStatus(context, STATUS_SYNC_ENABLED);
    }

    public static void markSyncing(Context context) {
        setStatus(context, STATUS_SYNCING);
    }

    public static void markSynced(Context context) {
        setStatus(context, STATUS_SYNCED);
    }

    public static void markError(Context context) {
        setStatus(context, STATUS_ERROR);
    }

    private static void setStatus(Context context, String status) {
        context.getApplicationContext()
                .getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_STATUS, status)
                .putLong(KEY_UPDATED_AT, System.currentTimeMillis())
                .apply();
    }
}
