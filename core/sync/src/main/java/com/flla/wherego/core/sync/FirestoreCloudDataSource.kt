package com.flla.wherego.core.sync

import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile
import com.flla.wherego.core.sync.CloudDataSource.Companion.NO_CURSOR
import com.flla.wherego.core.sync.CloudDataSource.Companion.SYNCED_AT
import com.google.firebase.Timestamp
import com.google.firebase.firestore.DocumentReference
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import com.google.firebase.firestore.Source
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

/**
 * Every document carries `syncedAt`, a server-assigned write time. `updatedAt` stays what it
 * always was — the authoring device's clock, and the input to [SyncMerge.decide] — but it cannot
 * drive an incremental pull: a peer that parked rows offline pushes them stamped in the past, so
 * a reader filtering on `updatedAt` above its own last-pull time never sees them.
 */
@Singleton
class FirestoreCloudDataSource @Inject constructor() : CloudDataSource {
    private val db: FirebaseFirestore = FirebaseFirestore.getInstance()

    override val available: Boolean = true

    override suspend fun pushTransactions(uid: String, rows: List<Transaction>) {
        push(uid, "transactions", rows.map { it.id to CloudCodec.transaction(it) })
    }

    override suspend fun pullTransactions(uid: String, sinceCursor: Long): CloudPage<Transaction> =
        pull(uid, "transactions", sinceCursor, CloudCodec::transaction)

    override suspend fun pushCategories(uid: String, rows: List<Category>) {
        push(uid, "categories", rows.map { it.id to CloudCodec.category(it) })
    }

    override suspend fun pullCategories(uid: String, sinceCursor: Long): CloudPage<Category> =
        pull(uid, "categories", sinceCursor, CloudCodec::category)

    override suspend fun pushProfile(uid: String, profile: UserProfile) {
        col(uid, "profile").document("profile")
            .set(stamped(CloudCodec.profile(profile)), SetOptions.merge())
            .await()
    }

    override suspend fun pullProfile(uid: String): UserProfile? {
        val snap = col(uid, "profile").document("profile").get(Source.SERVER).await()
        val json = CloudCodec.fromMap(snap.data) ?: return null
        return CloudCodec.profile(json)
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

    private suspend fun push(uid: String, collection: String, rows: List<Pair<String, JSONObject>>) {
        rows.chunked(BATCH).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { (id, json) ->
                batch.set(col(uid, collection).document(id), stamped(json))
            }
            batch.commit().await()
        }
    }

    /**
     * Reads from [Source.SERVER] so `syncedAt` is always resolved — a cached snapshot reports a
     * pending server timestamp as null and orders it as if it were, which would corrupt the
     * cursor. Rows written before `syncedAt` existed match no range filter, so the first pull
     * (cursor [NO_CURSOR]) reads the collection whole and stamps them on the way out.
     */
    private suspend fun <T> pull(
        uid: String,
        collection: String,
        sinceCursor: Long,
        decode: (JSONObject) -> T,
    ): CloudPage<T> {
        val query = if (sinceCursor == NO_CURSOR) {
            col(uid, collection)
        } else {
            col(uid, collection)
                .whereGreaterThan(SYNCED_AT, timestampOf(sinceCursor))
                .orderBy(SYNCED_AT)
        }
        val documents = query.get(Source.SERVER).await().documents
        val rows = ArrayList<T>(documents.size)
        val unstamped = ArrayList<DocumentReference>()
        var cursor = sinceCursor
        for (doc in documents) {
            CloudCodec.fromMap(doc.data)?.let { rows += decode(it) }
            val stamp = doc.getTimestamp(SYNCED_AT)
            if (stamp == null) unstamped += doc.reference else cursor = maxOf(cursor, cursorOf(stamp))
        }
        if (unstamped.isNotEmpty()) backfill(unstamped)
        return CloudPage(rows, cursor)
    }

    /**
     * Gives pre-`syncedAt` documents a stamp so later pulls can range over them. They were just
     * delivered by the full read above, and the stamp lands after [cursor], so the next pull
     * re-delivers them exactly once — an idempotent upsert either way.
     */
    private suspend fun backfill(refs: List<DocumentReference>) {
        refs.chunked(BATCH).forEach { chunk ->
            val batch = db.batch()
            chunk.forEach { batch.set(it, mapOf(SYNCED_AT to FieldValue.serverTimestamp()), SetOptions.merge()) }
            batch.commit().await()
        }
    }

    private fun stamped(json: JSONObject): Map<String, Any> =
        CloudCodec.toMap(json).also { it[SYNCED_AT] = FieldValue.serverTimestamp() }

    private fun cursorOf(stamp: Timestamp): Long =
        stamp.seconds * NANOS_PER_SECOND + stamp.nanoseconds

    private fun timestampOf(cursor: Long): Timestamp = Timestamp(
        Math.floorDiv(cursor, NANOS_PER_SECOND),
        Math.floorMod(cursor, NANOS_PER_SECOND).toInt(),
    )

    private fun col(uid: String, name: String) =
        db.collection("users").document(uid).collection(name)

    private companion object {
        const val BATCH = 400
        const val NANOS_PER_SECOND = 1_000_000_000L
    }
}
