package app.wherego.core.sync

import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

interface ReceiptUploader {
    /** Remote path `users/{uid}/receipts/{id}.jpg`, or null if skipped/failed. */
    suspend fun upload(uid: String, receiptId: String, file: File): String?
}

/** No Firebase Storage until H1. Always fail-open. */
@Singleton
class FakeReceiptUploader @Inject constructor() : ReceiptUploader {
    override suspend fun upload(uid: String, receiptId: String, file: File): String? = null
}
