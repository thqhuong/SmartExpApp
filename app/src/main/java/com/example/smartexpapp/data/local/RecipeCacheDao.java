package com.example.smartexpapp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface RecipeCacheDao {
    @Query("SELECT * FROM recipe_cache ORDER BY cached_at DESC")
    List<RecipeCacheEntity> getAll();

    @Query("SELECT * FROM recipe_cache WHERE id = :id LIMIT 1")
    RecipeCacheEntity getById(String id);

    @Query("DELETE FROM recipe_cache")
    int deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(RecipeCacheEntity recipeCache);
}
