package com.flla.wherego.core.database

import android.content.Context
import android.net.Uri
import com.flla.wherego.core.common.UlidGenerator
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.Clock
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ReceiptStore @Inject constructor(
    @ApplicationContext private val context: Context,
    private val receiptDao: ReceiptDao,
    private val ledger: LedgerStore,
    private val ulid: UlidGenerator,
    private val clock: Clock,
) {
    suspend fun ingest(transactionId: String, source: Uri): ReceiptEntity? {
        val id = ulid.next()
        val dest = ReceiptFiles.dest(context, id)
        if (!ReceiptFiles.compressTo(context, source, dest)) return null
        val now = clock.millis()
        val row = ReceiptEntity(
            id = id,
            transactionId = transactionId,
            localPath = dest.absolutePath,
            remotePath = null,
            ocrRaw = "",
            ocrAmountMinor = null,
            uploaded = false,
            createdAt = now,
        )
        receiptDao.upsert(row)
        ledger.setReceiptId(transactionId, id)
        return row
    }

    suspend fun recordOcr(id: String, raw: String, amountMinor: Long?) {
        receiptDao.setOcr(id, raw, amountMinor)
    }

    suspend fun get(id: String): ReceiptEntity? = receiptDao.get(id)

    suspend fun markUploaded(id: String, remotePath: String) {
        receiptDao.markUploaded(id, remotePath)
    }
}
