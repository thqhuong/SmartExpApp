package com.example.smartexpapp.data;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;

import com.example.smartexpapp.data.local.AppDatabase;
import com.example.smartexpapp.data.local.LocalDataContract;
import com.example.smartexpapp.data.local.UserSettingsEntity;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 35)
public class SettingsRepositoryTest {
    private AppDatabase database;

    @Before
    public void setUp() {
        Context context = ApplicationProvider.getApplicationContext();
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase.class)
                .allowMainThreadQueries()
                .build();
    }

    @After
    public void tearDown() {
        database.close();
    }

    @Test
    public void getSettingsCreatesRoomBackedDefaults() {
        SettingsRepository.SettingsSnapshot settings = SettingsRepository.getSettings(database);

        assertTrue(settings.areNotificationsEnabled());
        assertEquals(3, settings.getReminderDaysBefore());
        assertEquals(540, settings.getReminderNotifyTimeMinutes());
        assertEquals("Local User", settings.getDisplayName());
        assertFalse(settings.isDarkMode());
        assertEquals("en", settings.getLanguageTag());
        assertEquals(LocalDataContract.STORAGE_ROOM_TEMP_ID, settings.getDefaultStorageLocationId());
        UserSettingsEntity entity = database.userSettingsDao().getById("default");
        assertNotNull(entity);
        assertEquals(LocalDataContract.STORAGE_ROOM_TEMP_ID, entity.defaultStorageLocationId);
        assertEquals("en", entity.languageTag);
        assertEquals(3, database.storageLocationDao().count());
    }

    @Test
    public void settingsUpdatesPersistToRoom() {
        SettingsRepository.setNotificationsEnabled(database, false);
        SettingsRepository.setReminderDaysBefore(database, 5);
        SettingsRepository.setReminderNotifyTimeMinutes(database, 21 * 60 + 30);
        SettingsRepository.setDisplayName(database, "Kitchen Team");
        SettingsRepository.setDarkMode(database, true);
        SettingsRepository.setLanguageTag(database, "vi-VN");
        SettingsRepository.setDefaultStorageLocation(database, LocalDataContract.STORAGE_REFRIGERATOR_ID);
        SettingsRepository.setDietaryPreferences(database, " vegetarian, dairy-free ");

        SettingsRepository.SettingsSnapshot settings = SettingsRepository.getSettings(database);

        assertFalse(settings.areNotificationsEnabled());
        assertEquals(5, settings.getReminderDaysBefore());
        assertEquals(1290, settings.getReminderNotifyTimeMinutes());
        assertEquals("Kitchen Team", settings.getDisplayName());
        assertTrue(settings.isDarkMode());
        assertEquals("vi", settings.getLanguageTag());
        assertEquals(LocalDataContract.STORAGE_REFRIGERATOR_ID, settings.getDefaultStorageLocationId());
        assertEquals(LocalDataContract.STORAGE_REFRIGERATOR_NAME, settings.getDefaultStorageName());
        assertEquals("vegetarian, dairy-free", settings.getDietaryPreferences());
        assertEquals("vegetarian, dairy-free", settings.getDietaryPreferencesLabel());
        UserSettingsEntity entity = database.userSettingsDao().getById("default");
        assertNotNull(entity);
        assertFalse(entity.notificationEnabled);
        assertEquals(5, entity.reminderDaysBefore);
        assertEquals(1290, entity.reminderNotifyTimeMinutes);
        assertEquals("Kitchen Team", entity.displayName);
        assertTrue(entity.darkMode);
        assertEquals("vi", entity.languageTag);
        assertEquals(LocalDataContract.STORAGE_REFRIGERATOR_ID, entity.defaultStorageLocationId);
        assertEquals("vegetarian, dairy-free", entity.dietaryPreferences);
    }

    @Test
    public void notificationSettingsUpdatePersistsAllNotificationFieldsTogether() {
        SettingsRepository.setNotificationSettings(database, false, 14, 8 * 60 + 15);

        SettingsRepository.SettingsSnapshot settings = SettingsRepository.getSettings(database);

        assertFalse(settings.areNotificationsEnabled());
        assertEquals(14, settings.getReminderDaysBefore());
        assertEquals(495, settings.getReminderNotifyTimeMinutes());
    }

    @Test
    public void reminderDaysAreClampedToZero() {
        SettingsRepository.setReminderDaysBefore(database, -2);

        assertEquals(0, SettingsRepository.getSettings(database).getReminderDaysBefore());
    }

    @Test
    public void reminderDaysAreClampedToMax() {
        SettingsRepository.setReminderDaysBefore(database, 500);

        assertEquals(SettingsRepository.MAX_REMINDER_DAYS_BEFORE, SettingsRepository.getSettings(database).getReminderDaysBefore());
    }

    @Test
    public void reminderNotifyTimeIsClampedToValidDayMinutes() {
        SettingsRepository.setReminderNotifyTimeMinutes(database, -1);
        assertEquals(0, SettingsRepository.getSettings(database).getReminderNotifyTimeMinutes());

        SettingsRepository.setReminderNotifyTimeMinutes(database, 24 * 60);
        assertEquals(1439, SettingsRepository.getSettings(database).getReminderNotifyTimeMinutes());
    }

    @Test
    public void defaultStorageLocationFallsBackToRoomTempForInvalidValues() {
        SettingsRepository.setDefaultStorageLocation(database, "invalid-storage");

        assertEquals(
                LocalDataContract.STORAGE_ROOM_TEMP_ID,
                SettingsRepository.getSettings(database).getDefaultStorageLocationId()
        );
    }

    @Test
    public void blankDietaryPreferencesClearToEmptySnapshot() {
        SettingsRepository.setDietaryPreferences(database, "vegan");
        SettingsRepository.setDietaryPreferences(database, "   ");

        SettingsRepository.SettingsSnapshot settings = SettingsRepository.getSettings(database);
        UserSettingsEntity entity = database.userSettingsDao().getById("default");

        assertEquals("", settings.getDietaryPreferences());
        assertEquals("No dietary preferences", settings.getDietaryPreferencesLabel());
        assertNotNull(entity);
        assertNull(entity.dietaryPreferences);
    }

    @Test
    public void unsupportedLanguageFallsBackToEnglish() {
        SettingsRepository.setLanguageTag(database, "fr");

        SettingsRepository.SettingsSnapshot settings = SettingsRepository.getSettings(database);

        assertEquals("en", settings.getLanguageTag());
    }
}
