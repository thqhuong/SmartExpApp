package com.example.smartexpapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.Ignore;
import androidx.room.PrimaryKey;

@Entity(tableName = "storage_locations")
public class StorageLocationEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String name = "";

    @NonNull
    public String type = "";

    @ColumnInfo(name = "sort_order")
    public int sortOrder;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public StorageLocationEntity() {
    }

    @Ignore
    public StorageLocationEntity(@NonNull String id, @NonNull String name, @NonNull String type, int sortOrder, long createdAt, long updatedAt) {
        this.id = id;
        this.name = name;
        this.type = type;
        this.sortOrder = sortOrder;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
    }
}
