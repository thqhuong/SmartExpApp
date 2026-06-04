package com.example.smartexpapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.ForeignKey;
import androidx.room.Index;
import androidx.room.PrimaryKey;

@Entity(
        tableName = "expiry_scans",
        foreignKeys = @ForeignKey(
                entity = ProductEntity.class,
                parentColumns = "id",
                childColumns = "product_id",
                onDelete = ForeignKey.SET_NULL
        ),
        indices = @Index("product_id")
)
public class ExpiryScanEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @ColumnInfo(name = "product_id")
    public String productId;

    @ColumnInfo(name = "raw_text")
    @NonNull
    public String rawText = "";

    @ColumnInfo(name = "detected_date_millis")
    public Long detectedDateMillis;

    public float confidence;

    @ColumnInfo(name = "scanned_at")
    public long scannedAt;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public ExpiryScanEntity() {
    }
}
