package com.flla.wherego.core.sync

import android.net.Uri
import com.google.firebase.storage.FirebaseStorage
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirebaseReceiptUploader @Inject constructor() : ReceiptUploader {
    override suspend fun upload(uid: String, receiptId: String, file: File): String? {
        return try {
            val path = "users/$uid/receipts/$receiptId.jpg"
            val ref = FirebaseStorage.getInstance().reference.child(path)
            ref.putFile(Uri.fromFile(file)).await()
            path
        } catch (_: Exception) {
            null
        }
    }
}
