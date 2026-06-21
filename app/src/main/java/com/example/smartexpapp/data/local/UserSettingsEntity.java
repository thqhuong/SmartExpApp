package com.example.smartexpapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "user_settings",
        foreignKeys = @ForeignKey(
                entity = StorageLocationEntity.class,
                parentColumns = "id",
                childColumns = "default_storage_location_id"
        ),
        indices = @Index("default_storage_location_id")
)
public class UserSettingsEntity {
    @PrimaryKey
    @NonNull
    public String id = "default";

    @ColumnInfo(name = "reminder_days_before")
    public int reminderDaysBefore = 3;

    @ColumnInfo(name = "reminder_notify_time_minutes", defaultValue = "540")
    public int reminderNotifyTimeMinutes = 540;

    @ColumnInfo(name = "dietary_preferences")
    public String dietaryPreferences;

    @ColumnInfo(name = "display_name")
    public String displayName = "Local User";

    @ColumnInfo(name = "dark_mode", defaultValue = "0")
    public boolean darkMode = false;

    @ColumnInfo(name = "language_tag", defaultValue = "'en'")
    @NonNull
    public String languageTag = "en";

    @ColumnInfo(name = "default_storage_location_id")
    @NonNull
    public String defaultStorageLocationId = LocalDataContract.STORAGE_ROOM_TEMP_ID;

    @ColumnInfo(name = "notification_enabled")
    public boolean notificationEnabled = true;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public UserSettingsEntity() {
    }
}
