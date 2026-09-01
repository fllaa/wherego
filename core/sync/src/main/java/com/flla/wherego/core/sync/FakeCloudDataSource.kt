package com.flla.wherego.core.sync

import android.content.Context
import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile
import com.flla.wherego.core.sync.CloudDataSource.Companion.NO_CURSOR
import com.flla.wherego.core.sync.CloudDataSource.Companion.SYNCED_AT
import dagger.hilt.android.qualifiers.ApplicationContext
import java.io.File
import java.util.concurrent.atomic.AtomicLong
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

/**
 * Files under `filesDir/wherego-cloud` standing in for Firestore. It assigns `syncedAt` the way
 * the server does — strictly increasing, independent of any row's `updatedAt` — so the cursor
 * behaviour a device relies on is the same one this fake exercises.
 */
@Singleton
class FakeCloudDataSource @Inject constructor(
    @ApplicationContext context: Context,
) : CloudDataSource {
    private val root = File(context.filesDir, "wherego-cloud")

    /** Stands in for the server clock: wall time in nanos, bumped when two writes collide. */
    private val serverClock = AtomicLong(NO_CURSOR)

    override val available: Boolean = true

    override suspend fun pushTransactions(uid: String, rows: List<Transaction>) {
        rows.forEach { write(uid, "transactions", it.id, CloudCodec.transaction(it)) }
    }

    override suspend fun pullTransactions(uid: String, sinceCursor: Long): CloudPage<Transaction> =
        pull(uid, "transactions", sinceCursor, CloudCodec::transaction)

    override suspend fun pushCategories(uid: String, rows: List<Category>) {
        rows.forEach { write(uid, "categories", it.id, CloudCodec.category(it)) }
    }

    override suspend fun pullCategories(uid: String, sinceCursor: Long): CloudPage<Category> =
        pull(uid, "categories", sinceCursor, CloudCodec::category)

    override suspend fun pushProfile(uid: String, profile: UserProfile) {
        write(uid, "profile", "profile", CloudCodec.profile(profile))
    }

    override suspend fun pullProfile(uid: String): UserProfile? =
        read(uid, "profile", "profile")?.let(CloudCodec::profile)

    override suspend fun deleteAll(uid: String) {
        File(root, uid).deleteRecursively()
    }

    private fun <T> pull(
        uid: String,
        collection: String,
        sinceCursor: Long,
        decode: (JSONObject) -> T,
    ): CloudPage<T> {
        val full = sinceCursor == NO_CURSOR
        val rows = ArrayList<T>()
        var cursor = sinceCursor
        for ((file, json) in readAll(uid, collection).sortedBy { it.second.optLong(SYNCED_AT) }) {
            var stamp = json.optLong(SYNCED_AT, NO_CURSOR)
            if (!full && stamp <= sinceCursor) continue
            rows += decode(json)
            if (stamp == NO_CURSOR) {
                // Written before `syncedAt` existed: stamp it so later pulls can range over it.
                stamp = nextStamp()
                file.writeText(json.put(SYNCED_AT, stamp).toString())
            } else {
                cursor = maxOf(cursor, stamp)
            }
        }
        return CloudPage(rows, cursor)
    }

    private fun nextStamp(): Long {
        val wall = System.currentTimeMillis() * NANOS_PER_MILLI
        return serverClock.updateAndGet { last -> maxOf(last + 1, wall) }
    }

    private fun dir(uid: String, collection: String): File =
        File(root, "$uid/$collection").also { it.mkdirs() }

    private fun write(uid: String, collection: String, id: String, json: JSONObject) {
        File(dir(uid, collection), "$id.json").writeText(json.put(SYNCED_AT, nextStamp()).toString())
    }

    private fun read(uid: String, collection: String, id: String): JSONObject? {
        val file = File(dir(uid, collection), "$id.json")
        if (!file.exists()) return null
        return JSONObject(file.readText())
    }

    private fun readAll(uid: String, collection: String): List<Pair<File, JSONObject>> {
        val folder = dir(uid, collection)
        return folder.listFiles { f -> f.extension == "json" }
            ?.map { it to JSONObject(it.readText()) }
            .orEmpty()
    }

    private companion object {
        const val NANOS_PER_MILLI = 1_000_000L
    }
}
