package com.example.smartexpapp.data.local;

import androidx.annotation.NonNull;
import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "agent_messages")
public class AgentMessageEntity {
    @PrimaryKey
    @NonNull
    public String id = "";

    @NonNull
    public String role = "";

    @NonNull
    public String message = "";

    @ColumnInfo(name = "related_product_ids")
    public String relatedProductIds;

    @ColumnInfo(name = "source_prompt")
    public String sourcePrompt;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    public long updatedAt;

    public AgentMessageEntity() {
    }
}
