package com.flla.wherego.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import kotlinx.coroutines.flow.Flow

@Dao
interface TransactionDao {
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insert(row: TransactionEntity)

    @Update
    suspend fun update(row: TransactionEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: TransactionEntity)

    @Query("SELECT * FROM transactions WHERE dirty = 1")
    suspend fun listDirty(): List<TransactionEntity>

    @Query("SELECT COUNT(*) FROM transactions WHERE dirty = 1")
    fun observeDirtyCount(): Flow<Int>

    @Query("SELECT * FROM transactions WHERE id = :id LIMIT 1")
    suspend fun get(id: String): TransactionEntity?

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY occurredOn DESC, createdAt DESC")
    fun observeActive(): Flow<List<TransactionEntity>>

    @Query("SELECT * FROM transactions WHERE deletedAt IS NULL ORDER BY occurredOn DESC, createdAt DESC")
    suspend fun listActive(): List<TransactionEntity>

    @Query(
        """
        SELECT categoryId FROM transactions
        WHERE deletedAt IS NULL AND kind = :kind
        ORDER BY createdAt DESC
        """,
    )
    suspend fun recentCategoryIds(kind: String): List<String>

    @Query(
        """
        SELECT COALESCE(SUM(amountBaseMinor), 0) FROM transactions
        WHERE deletedAt IS NULL
          AND kind = 'expense'
          AND occurredOn >= :startOn
          AND occurredOn <= :endOn
        """,
    )
    suspend fun sumExpenses(startOn: String, endOn: String): Long

    @Query("SELECT COUNT(*) FROM transactions WHERE categoryId = :categoryId")
    suspend fun countForCategory(categoryId: String): Int

    @Query("SELECT DISTINCT currency FROM transactions WHERE deletedAt IS NULL")
    suspend fun distinctCurrencies(): List<String>
}
