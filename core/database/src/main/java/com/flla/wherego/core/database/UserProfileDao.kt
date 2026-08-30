package com.flla.wherego.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface UserProfileDao {
    @Query("SELECT * FROM user_profile LIMIT 1")
    suspend fun get(): UserProfileEntity?

    @Query("SELECT * FROM user_profile LIMIT 1")
    fun observe(): Flow<UserProfileEntity?>

    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: UserProfileEntity)

    @Update
    suspend fun update(row: UserProfileEntity)
}
