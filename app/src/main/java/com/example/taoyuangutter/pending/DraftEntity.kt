package com.example.taoyuangutter.pending

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gutter_session_drafts")
data class DraftEntity(
    @PrimaryKey
    val id: Long,
    @ColumnInfo(name = "saved_at")
    val savedAt: Long,
    val kind: String,
    @ColumnInfo(name = "is_offline")
    val isOffline: Boolean,
    @ColumnInfo(name = "is_single_point")
    // Legacy compatibility column. New saves always persist false.
    val isSinglePoint: Boolean,
    @ColumnInfo(name = "waypoints_json")
    val waypointsJson: String
)
