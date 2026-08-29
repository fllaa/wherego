package app.wherego.core.database

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "receipts",
    indices = [Index("transactionId")],
)
data class ReceiptEntity(
    @PrimaryKey val id: String,
    val transactionId: String,
    val localPath: String,
    val remotePath: String?,
    val ocrRaw: String,
    val ocrAmountMinor: Long?,
    val uploaded: Boolean,
    val createdAt: Long,
)
