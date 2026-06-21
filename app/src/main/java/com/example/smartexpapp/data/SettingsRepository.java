package com.example.smartexpapp.data;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;

import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.firestore.UserDataSyncRepository;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.data.local.StorageLocationEntity;
import com.example.smartexpapp.data.local.UserSettingsEntity;

import java.util.Arrays;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public final class SettingsRepository {
    public static final int DEFAULT_REMINDER_NOTIFY_TIME_MINUTES = 9 * 60;
    public static final int MAX_REMINDER_DAYS_BEFORE = 365;
    private static final String SETTINGS_ID = "default";
    private static final String LEGACY_PREFS = "smart_settings";
    private static final String KEY_NOTIFICATIONS_ENABLED = "notifications_enabled";
    private static final String KEY_REMINDER_DAYS = "reminder_days_before";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_LANGUAGE_TAG = "language_tag";
    private static final ExecutorService EXECUTOR = Executors.newSingleThreadExecutor();

    public interface Callback<T> {
        void onResult(T value);
    }

    public interface ErrorCallback {
        void onError(Exception error);
    }

    public static final class SettingsSnapshot {
        private final boolean notificationsEnabled;
        private final int reminderDaysBefore;
        private final int reminderNotifyTimeMinutes;
        private final String displayName;
        private final boolean darkMode;
        private final String languageTag;
        private final String defaultStorageLocationId;
        private final String dietaryPreferences;

        public SettingsSnapshot(boolean notificationsEnabled, int reminderDaysBefore) {
            this(notificationsEnabled, reminderDaysBefore, DEFAULT_REMINDER_NOTIFY_TIME_MINUTES);
        }

        public SettingsSnapshot(boolean notificationsEnabled, int reminderDaysBefore, int reminderNotifyTimeMinutes) {
            this(notificationsEnabled, reminderDaysBefore, reminderNotifyTimeMinutes, "Local User", false, "en", LocalDataContract.STORAGE_ROOM_TEMP_ID, null);
        }

        public SettingsSnapshot(boolean notificationsEnabled, int reminderDaysBefore, String displayName) {
            this(notificationsEnabled, reminderDaysBefore, DEFAULT_REMINDER_NOTIFY_TIME_MINUTES, displayName, false, "en", LocalDataContract.STORAGE_ROOM_TEMP_ID, null);
        }

        public SettingsSnapshot(boolean notificationsEnabled, int reminderDaysBefore, String displayName, boolean darkMode) {
            this(notificationsEnabled, reminderDaysBefore, DEFAULT_REMINDER_NOTIFY_TIME_MINUTES, displayName, darkMode, "en", LocalDataContract.STORAGE_ROOM_TEMP_ID, null);
        }

        public SettingsSnapshot(boolean notificationsEnabled, int reminderDaysBefore, String displayName, boolean darkMode, String defaultStorageLocationId) {
            this(notificationsEnabled, reminderDaysBefore, DEFAULT_REMINDER_NOTIFY_TIME_MINUTES, displayName, darkMode, "en", defaultStorageLocationId, null);
        }

        public SettingsSnapshot(boolean notificationsEnabled, int reminderDaysBefore, String displayName, boolean darkMode, String defaultStorageLocationId, String dietaryPreferences) {
            this(notificationsEnabled, reminderDaysBefore, DEFAULT_REMINDER_NOTIFY_TIME_MINUTES, displayName, darkMode, "en", defaultStorageLocationId, dietaryPreferences);
        }

        public SettingsSnapshot(boolean notificationsEnabled, int reminderDaysBefore, String displayName, boolean darkMode, String languageTag, String defaultStorageLocationId, String dietaryPreferences) {
            this(notificationsEnabled, reminderDaysBefore, DEFAULT_REMINDER_NOTIFY_TIME_MINUTES, displayName, darkMode, languageTag, defaultStorageLocationId, dietaryPreferences);
        }

        public SettingsSnapshot(boolean notificationsEnabled, int reminderDaysBefore, int reminderNotifyTimeMinutes, String displayName, boolean darkMode, String languageTag, String defaultStorageLocationId, String dietaryPreferences) {
            this.notificationsEnabled = notificationsEnabled;
            this.reminderDaysBefore = normalizeReminderDaysBefore(reminderDaysBefore);
            this.reminderNotifyTimeMinutes = normalizeReminderNotifyTimeMinutes(reminderNotifyTimeMinutes);
            this.displayName = displayName == null || displayName.trim().isEmpty()
                    ? "Local User"
                    : displayName.trim();
            this.darkMode = darkMode;
            this.languageTag = normalizeLanguageTag(languageTag);
            this.defaultStorageLocationId = normalizeStorageLocationId(defaultStorageLocationId);
            this.dietaryPreferences = normalizeOptionalText(dietaryPreferences);
        }

        public boolean areNotificationsEnabled() {
            return notificationsEnabled;
        }

        public int getReminderDaysBefore() {
            return reminderDaysBefore;
        }

        public int getReminderNotifyTimeMinutes() {
            return reminderNotifyTimeMinutes;
        }

        public String getDisplayName() {
            return displayName;
        }

        public boolean isDarkMode() {
            return darkMode;
        }

        public String getLanguageTag() {
            return languageTag;
        }

        public String getDefaultStorageLocationId() {
            return defaultStorageLocationId;
        }

        public String getDefaultStorageName() {
            return LocalDataContract.storageNameForId(defaultStorageLocationId);
        }

        public String getDietaryPreferences() {
            return dietaryPreferences == null ? "" : dietaryPreferences;
        }

        public String getDietaryPreferencesLabel() {
            return dietaryPreferences == null ? "No dietary preferences" : dietaryPreferences;
        }
    }

    private SettingsRepository() {
    }

    public static SettingsSnapshot getSettings(Context context) {
        SettingsSnapshot settings = toSnapshot(getOrCreateSettings(context, AppDatabase.getInstance(context)));
        cacheDarkMode(context, settings.isDarkMode());
        cacheLanguageTag(context, settings.getLanguageTag());
        return settings;
    }

    public static void getSettingsAsync(Context context, Callback<SettingsSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> getSettings(context), callback, errorCallback);
    }

    public static SettingsSnapshot getSettings(AppDatabase database) {
        return toSnapshot(getOrCreateSettings(database));
    }

    public static boolean getCachedDarkMode(Context context) {
        return context.getApplicationContext()
                .getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
                .getBoolean(KEY_DARK_MODE, false);
    }

    public static String getCachedLanguageTag(Context context) {
        return normalizeLanguageTag(context.getApplicationContext()
                .getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
                .getString(KEY_LANGUAGE_TAG, "en"));
    }

    public static boolean areNotificationsEnabled(Context context) {
        return getSettings(context).areNotificationsEnabled();
    }

    public static void setNotificationsEnabled(Context context, boolean enabled) {
        AppDatabase database = AppDatabase.getInstance(context);
        UserSettingsEntity settings = getOrCreateSettings(context, database);
        settings.notificationEnabled = enabled;
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
        UserDataSyncRepository.syncSettingsAsync(context, database);
    }

    public static void setNotificationsEnabledAsync(Context context, boolean enabled, Callback<SettingsSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> {
            setNotificationsEnabled(context, enabled);
            return getSettings(context);
        }, callback, errorCallback);
    }

    public static void setNotificationSettings(Context context, boolean enabled, int reminderDaysBefore, int reminderNotifyTimeMinutes) {
        AppDatabase database = AppDatabase.getInstance(context);
        UserSettingsEntity settings = getOrCreateSettings(context, database);
        settings.notificationEnabled = enabled;
        settings.reminderDaysBefore = normalizeReminderDaysBefore(reminderDaysBefore);
        settings.reminderNotifyTimeMinutes = normalizeReminderNotifyTimeMinutes(reminderNotifyTimeMinutes);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
        UserDataSyncRepository.syncSettingsAsync(context, database);
    }

    public static void setNotificationSettingsAsync(Context context, boolean enabled, int reminderDaysBefore, int reminderNotifyTimeMinutes, Callback<SettingsSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> {
            setNotificationSettings(context, enabled, reminderDaysBefore, reminderNotifyTimeMinutes);
            return getSettings(context);
        }, callback, errorCallback);
    }

    public static void setNotificationSettings(AppDatabase database, boolean enabled, int reminderDaysBefore, int reminderNotifyTimeMinutes) {
        UserSettingsEntity settings = getOrCreateSettings(database);
        settings.notificationEnabled = enabled;
        settings.reminderDaysBefore = normalizeReminderDaysBefore(reminderDaysBefore);
        settings.reminderNotifyTimeMinutes = normalizeReminderNotifyTimeMinutes(reminderNotifyTimeMinutes);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
    }

    public static void setNotificationsEnabled(AppDatabase database, boolean enabled) {
        UserSettingsEntity settings = getOrCreateSettings(database);
        settings.notificationEnabled = enabled;
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
    }

    public static int getReminderDaysBefore(Context context) {
        return getSettings(context).getReminderDaysBefore();
    }

    public static int getReminderNotifyTimeMinutes(Context context) {
        return getSettings(context).getReminderNotifyTimeMinutes();
    }

    public static void setReminderDaysBefore(Context context, int days) {
        AppDatabase database = AppDatabase.getInstance(context);
        UserSettingsEntity settings = getOrCreateSettings(context, database);
        settings.reminderDaysBefore = normalizeReminderDaysBefore(days);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
        UserDataSyncRepository.syncSettingsAsync(context, database);
    }

    public static void setReminderDaysBeforeAsync(Context context, int days, Callback<SettingsSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> {
            setReminderDaysBefore(context, days);
            return getSettings(context);
        }, callback, errorCallback);
    }

    public static void setReminderDaysBefore(AppDatabase database, int days) {
        UserSettingsEntity settings = getOrCreateSettings(database);
        settings.reminderDaysBefore = normalizeReminderDaysBefore(days);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
    }

    public static void setReminderNotifyTimeMinutes(Context context, int minutesAfterMidnight) {
        AppDatabase database = AppDatabase.getInstance(context);
        UserSettingsEntity settings = getOrCreateSettings(context, database);
        settings.reminderNotifyTimeMinutes = normalizeReminderNotifyTimeMinutes(minutesAfterMidnight);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
        UserDataSyncRepository.syncSettingsAsync(context, database);
    }

    public static void setReminderNotifyTimeMinutesAsync(Context context, int minutesAfterMidnight, Callback<SettingsSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> {
            setReminderNotifyTimeMinutes(context, minutesAfterMidnight);
            return getSettings(context);
        }, callback, errorCallback);
    }

    public static void setReminderNotifyTimeMinutes(AppDatabase database, int minutesAfterMidnight) {
        UserSettingsEntity settings = getOrCreateSettings(database);
        settings.reminderNotifyTimeMinutes = normalizeReminderNotifyTimeMinutes(minutesAfterMidnight);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
    }

    public static void setDisplayName(Context context, String displayName) {
        AppDatabase database = AppDatabase.getInstance(context);
        UserSettingsEntity settings = getOrCreateSettings(context, database);
        settings.displayName = normalizeDisplayName(displayName);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
        UserDataSyncRepository.syncSettingsAsync(context, database);
    }

    public static void setDisplayNameAsync(Context context, String displayName, Callback<SettingsSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> {
            setDisplayName(context, displayName);
            return getSettings(context);
        }, callback, errorCallback);
    }

    public static void setDisplayName(AppDatabase database, String displayName) {
        UserSettingsEntity settings = getOrCreateSettings(database);
        settings.displayName = normalizeDisplayName(displayName);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
    }

    public static void setDarkMode(Context context, boolean darkMode) {
        AppDatabase database = AppDatabase.getInstance(context);
        UserSettingsEntity settings = getOrCreateSettings(context, database);
        settings.darkMode = darkMode;
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
        cacheDarkMode(context, darkMode);
        UserDataSyncRepository.syncSettingsAsync(context, database);
    }

    public static void setDarkModeAsync(Context context, boolean darkMode, Callback<SettingsSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> {
            setDarkMode(context, darkMode);
            return getSettings(context);
        }, callback, errorCallback);
    }

    public static void setDarkMode(AppDatabase database, boolean darkMode) {
        UserSettingsEntity settings = getOrCreateSettings(database);
        settings.darkMode = darkMode;
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
    }

    public static void setLanguageTag(Context context, String languageTag) {
        AppDatabase database = AppDatabase.getInstance(context);
        String normalizedLanguageTag = normalizeLanguageTag(languageTag);
        UserSettingsEntity settings = getOrCreateSettings(context, database);
        settings.languageTag = normalizedLanguageTag;
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
        cacheLanguageTag(context, normalizedLanguageTag);
        UserDataSyncRepository.syncSettingsAsync(context, database);
    }

    public static void setLanguageTagAsync(Context context, String languageTag, Callback<SettingsSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> {
            setLanguageTag(context, languageTag);
            return getSettings(context);
        }, callback, errorCallback);
    }

    public static void setLanguageTag(AppDatabase database, String languageTag) {
        UserSettingsEntity settings = getOrCreateSettings(database);
        settings.languageTag = normalizeLanguageTag(languageTag);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
    }

    public static void setDefaultStorageLocation(Context context, String storageLocationId) {
        AppDatabase database = AppDatabase.getInstance(context);
        UserSettingsEntity settings = getOrCreateSettings(context, database);
        settings.defaultStorageLocationId = normalizeStorageLocationId(storageLocationId);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
        UserDataSyncRepository.syncSettingsAsync(context, database);
    }

    public static void setDefaultStorageLocationAsync(Context context, String storageLocationId, Callback<SettingsSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> {
            setDefaultStorageLocation(context, storageLocationId);
            return getSettings(context);
        }, callback, errorCallback);
    }

    public static void setDefaultStorageLocation(AppDatabase database, String storageLocationId) {
        ensureStorageLocations(database);
        UserSettingsEntity settings = getOrCreateSettings(database);
        settings.defaultStorageLocationId = normalizeStorageLocationId(storageLocationId);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
    }

    public static void setDietaryPreferences(Context context, String dietaryPreferences) {
        AppDatabase database = AppDatabase.getInstance(context);
        UserSettingsEntity settings = getOrCreateSettings(context, database);
        settings.dietaryPreferences = normalizeOptionalText(dietaryPreferences);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
        UserDataSyncRepository.syncSettingsAsync(context, database);
    }

    public static void setDietaryPreferencesAsync(Context context, String dietaryPreferences, Callback<SettingsSnapshot> callback, ErrorCallback errorCallback) {
        execute(() -> {
            setDietaryPreferences(context, dietaryPreferences);
            return getSettings(context);
        }, callback, errorCallback);
    }

    public static void setDietaryPreferences(AppDatabase database, String dietaryPreferences) {
        UserSettingsEntity settings = getOrCreateSettings(database);
        settings.dietaryPreferences = normalizeOptionalText(dietaryPreferences);
        settings.updatedAt = System.currentTimeMillis();
        database.userSettingsDao().insert(settings);
    }

    private static UserSettingsEntity getOrCreateSettings(Context context, AppDatabase database) {
        UserSettingsEntity existing = database.userSettingsDao().getById(SETTINGS_ID);
        if (existing != null) {
            return existing;
        }
        SharedPreferences legacy = context.getApplicationContext().getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE);
        return createDefaultSettings(
                database,
                legacy.getBoolean(KEY_NOTIFICATIONS_ENABLED, true),
                legacy.getInt(KEY_REMINDER_DAYS, 3),
                legacy.getBoolean(KEY_DARK_MODE, false),
                legacy.getString(KEY_LANGUAGE_TAG, "en")
        );
    }

    private static UserSettingsEntity getOrCreateSettings(AppDatabase database) {
        UserSettingsEntity existing = database.userSettingsDao().getById(SETTINGS_ID);
        if (existing != null) {
            return existing;
        }
        return createDefaultSettings(database, true, 3, false, "en");
    }

    private static UserSettingsEntity createDefaultSettings(AppDatabase database, boolean notificationsEnabled, int reminderDaysBefore, boolean darkMode, String languageTag) {
        ensureStorageLocations(database);
        long now = System.currentTimeMillis();
        UserSettingsEntity settings = new UserSettingsEntity();
        settings.id = SETTINGS_ID;
        settings.defaultStorageLocationId = LocalDataContract.STORAGE_ROOM_TEMP_ID;
        settings.reminderDaysBefore = normalizeReminderDaysBefore(reminderDaysBefore);
        settings.reminderNotifyTimeMinutes = DEFAULT_REMINDER_NOTIFY_TIME_MINUTES;
        settings.notificationEnabled = notificationsEnabled;
        settings.displayName = "Local User";
        settings.darkMode = darkMode;
        settings.languageTag = normalizeLanguageTag(languageTag);
        settings.createdAt = now;
        settings.updatedAt = now;
        database.userSettingsDao().insert(settings);
        return settings;
    }

    private static void ensureStorageLocations(AppDatabase database) {
        if (database.storageLocationDao().count() > 0) {
            return;
        }
        long now = System.currentTimeMillis();
        database.storageLocationDao().insertAll(Arrays.asList(
                new StorageLocationEntity(LocalDataContract.STORAGE_ROOM_TEMP_ID, LocalDataContract.STORAGE_ROOM_TEMP_NAME, "ROOM", 0, now, now),
                new StorageLocationEntity(LocalDataContract.STORAGE_REFRIGERATOR_ID, LocalDataContract.STORAGE_REFRIGERATOR_NAME, "COLD", 1, now, now),
                new StorageLocationEntity(LocalDataContract.STORAGE_FREEZE_ID, LocalDataContract.STORAGE_FREEZE_NAME, "FROZEN", 2, now, now)
        ));
    }

    private static SettingsSnapshot toSnapshot(UserSettingsEntity settings) {
        return new SettingsSnapshot(
                settings.notificationEnabled,
                settings.reminderDaysBefore,
                settings.reminderNotifyTimeMinutes,
                settings.displayName,
                settings.darkMode,
                settings.languageTag,
                settings.defaultStorageLocationId,
                settings.dietaryPreferences
        );
    }

    private static int normalizeReminderDaysBefore(int days) {
        return Math.max(0, Math.min(MAX_REMINDER_DAYS_BEFORE, days));
    }

    private static int normalizeReminderNotifyTimeMinutes(int minutesAfterMidnight) {
        return Math.max(0, Math.min((24 * 60) - 1, minutesAfterMidnight));
    }

    private static String normalizeDisplayName(String displayName) {
        if (displayName == null || displayName.trim().isEmpty()) {
            return "Local User";
        }
        return displayName.trim();
    }

    private static String normalizeStorageLocationId(String storageLocationId) {
        if (LocalDataContract.STORAGE_REFRIGERATOR_ID.equals(storageLocationId)) {
            return LocalDataContract.STORAGE_REFRIGERATOR_ID;
        }
        if (LocalDataContract.STORAGE_FREEZE_ID.equals(storageLocationId)) {
            return LocalDataContract.STORAGE_FREEZE_ID;
        }
        return LocalDataContract.STORAGE_ROOM_TEMP_ID;
    }

    private static String normalizeLanguageTag(String languageTag) {
        if (languageTag != null && languageTag.trim().toLowerCase().startsWith("vi")) {
            return "vi";
        }
        return "en";
    }

    private static String normalizeOptionalText(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        return value.trim().replaceAll("\\s+", " ");
    }

    private static void cacheDarkMode(Context context, boolean darkMode) {
        context.getApplicationContext()
                .getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_DARK_MODE, darkMode)
                .apply();
    }

    private static void cacheLanguageTag(Context context, String languageTag) {
        context.getApplicationContext()
                .getSharedPreferences(LEGACY_PREFS, Context.MODE_PRIVATE)
                .edit()
                .putString(KEY_LANGUAGE_TAG, normalizeLanguageTag(languageTag))
                .apply();
    }

    private interface Work<T> {
        T run() throws Exception;
    }

    private static <T> void execute(Work<T> work, Callback<T> callback, ErrorCallback errorCallback) {
        EXECUTOR.execute(() -> {
            try {
                T result = work.run();
                if (callback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> callback.onResult(result));
                }
            } catch (Exception error) {
                if (errorCallback != null) {
                    new Handler(Looper.getMainLooper()).post(() -> errorCallback.onError(error));
                }
            }
        });
    }
}
