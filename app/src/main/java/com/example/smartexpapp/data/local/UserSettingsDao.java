package com.example.smartexpapp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

@Dao
public interface UserSettingsDao {
    @Query("SELECT * FROM user_settings WHERE id = :id LIMIT 1")
    UserSettingsEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(UserSettingsEntity settings);
}
