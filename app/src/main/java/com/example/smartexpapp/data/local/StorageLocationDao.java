package com.example.smartexpapp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface StorageLocationDao {
    @Query("SELECT COUNT(*) FROM storage_locations")
    int count();

    @Query("SELECT * FROM storage_locations ORDER BY sort_order ASC")
    List<StorageLocationEntity> getAll();

    @Query("SELECT * FROM storage_locations WHERE id = :id LIMIT 1")
    StorageLocationEntity getById(String id);

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insertAll(List<StorageLocationEntity> storageLocations);
}
