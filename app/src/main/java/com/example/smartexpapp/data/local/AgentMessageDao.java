package com.example.smartexpapp.data.local;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.OnConflictStrategy;
import androidx.room.Query;

import java.util.List;

@Dao
public interface AgentMessageDao {
    @Query("SELECT * FROM agent_messages ORDER BY created_at ASC")
    List<AgentMessageEntity> getConversation();

    @Query("SELECT * FROM agent_messages ORDER BY created_at DESC LIMIT :limit")
    List<AgentMessageEntity> getRecent(int limit);

    @Query("DELETE FROM agent_messages")
    int deleteAll();

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    void insert(AgentMessageEntity message);
}
