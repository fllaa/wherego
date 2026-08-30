package com.flla.wherego.core.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface ReceiptDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(row: ReceiptEntity)

    @Query("SELECT * FROM receipts WHERE id = :id LIMIT 1")
    suspend fun get(id: String): ReceiptEntity?

    @Query("SELECT * FROM receipts WHERE transactionId = :transactionId ORDER BY createdAt DESC LIMIT 1")
    suspend fun latestFor(transactionId: String): ReceiptEntity?

    @Query(
        "UPDATE receipts SET ocrRaw = :ocrRaw, ocrAmountMinor = :ocrAmountMinor WHERE id = :id",
    )
    suspend fun setOcr(id: String, ocrRaw: String, ocrAmountMinor: Long?)

    @Query("UPDATE receipts SET uploaded = 1, remotePath = :remotePath WHERE id = :id")
    suspend fun markUploaded(id: String, remotePath: String)
}
