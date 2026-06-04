package com.example.smartexpapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "recipe_cache")
public class RecipeCacheEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String provider = "";

    @NonNull
    public String title = "";

    @ColumnInfo(name = "image_url")
    public String imageUrl;

    @ColumnInfo(name = "source_url")
    public String sourceUrl;

    @ColumnInfo(name = "used_ingredients")
    public String usedIngredients;

    @ColumnInfo(name = "missing_ingredients")
    public String missingIngredients;

    @ColumnInfo(name = "cached_at")
    public long cachedAt;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public RecipeCacheEntity() {
    }
}
