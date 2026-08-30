package com.flla.wherego.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface SyncStateDao {
    @Query("SELECT * FROM sync_state WHERE collection = :collection LIMIT 1")
    suspend fun get(collection: String): SyncStateEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: SyncStateEntity)
}
