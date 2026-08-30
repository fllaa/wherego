package com.flla.wherego.core.sync

import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await

@Singleton
class FirestoreCloudDataSource @Inject constructor() : CloudDataSource {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override val available: Boolean = true

    override suspend fun pushTransactions(uid: String, rows: List<Transaction>) {
        rows.chunked(BATCH).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { row ->
                batch.set(col(uid, "transactions").document(row.id), CloudCodec.toMap(CloudCodec.transaction(row)))
            }
            batch.commit().await()
        }
    }

    override suspend fun pullTransactions(uid: String, sinceEpoch: Long): List<Transaction> =
        pull(uid, "transactions", sinceEpoch).map(CloudCodec::transaction)

    override suspend fun pushCategories(uid: String, rows: List<Category>) {
        rows.chunked(BATCH).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { row ->
                batch.set(col(uid, "categories").document(row.id), CloudCodec.toMap(CloudCodec.category(row)))
            }
            batch.commit().await()
        }
    }

    override suspend fun pullCategories(uid: String, sinceEpoch: Long): List<Category> =
        pull(uid, "categories", sinceEpoch).map(CloudCodec::category)

    override suspend fun pushProfile(uid: String, profile: UserProfile) {
        col(uid, "profile").document("profile")
            .set(CloudCodec.toMap(CloudCodec.profile(profile)), SetOptions.merge())
            .await()
    }

    override suspend fun pullProfile(uid: String, sinceEpoch: Long): UserProfile? {
        val snap = col(uid, "profile").document("profile").get().await()
        val json = CloudCodec.fromMap(snap.data) ?: return null
        val profile = CloudCodec.profile(json)
        return profile.takeIf { it.updatedAt > sinceEpoch }
    }

    override suspend fun deleteAll(uid: String) {
        listOf("transactions", "categories", "profile").forEach { name ->
            while (true) {
                val docs = col(uid, name).limit(BATCH.toLong()).get().await().documents
                if (docs.isEmpty()) break
                val batch = db.batch()
                docs.forEach { batch.delete(it.reference) }
                batch.commit().await()
            }
        }
    }

    private suspend fun pull(uid: String, collection: String, sinceEpoch: Long) =
        col(uid, collection)
            .whereGreaterThan("updatedAt", sinceEpoch)
            .get()
            .await()
            .documents
            .mapNotNull { CloudCodec.fromMap(it.data) }

    private fun col(uid: String, name: String) =
        db.collection("users").document(uid).collection(name)

    private companion object {
        const val BATCH = 400
    }
}
