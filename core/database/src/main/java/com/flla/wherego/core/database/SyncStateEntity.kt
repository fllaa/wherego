package com.flla.wherego.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val collection: String,
    val lastPullEpoch: Long,
    val lastPushEpoch: Long,
)
