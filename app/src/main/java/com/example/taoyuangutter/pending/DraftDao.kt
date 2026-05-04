package com.example.taoyuangutter.pending

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface DraftDao {
    @Query("SELECT * FROM gutter_session_drafts ORDER BY saved_at DESC")
    fun getAll(): List<DraftEntity>

    @Query("SELECT * FROM gutter_session_drafts WHERE id = :id LIMIT 1")
    fun getById(id: Long): DraftEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    fun upsert(draft: DraftEntity)

    @Query("DELETE FROM gutter_session_drafts WHERE id = :id")
    fun deleteById(id: Long)

    @Query("SELECT COUNT(*) FROM gutter_session_drafts")
    fun count(): Int
}
