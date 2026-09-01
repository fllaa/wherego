package com.flla.wherego.core.sync

import com.flla.wherego.core.model.Category
import com.flla.wherego.core.model.Transaction
import com.flla.wherego.core.model.UserProfile

/**
 * Rows a pull delivered, plus the cursor the next pull resumes from.
 *
 * [cursor] is epoch nanoseconds in the cloud's clock domain, taken from the `syncedAt` stamp of
 * the newest row in [rows]. It carries the caller's own `sinceCursor` back when nothing arrived,
 * so a watermark never advances past a row this device has not seen.
 */
data class CloudPage<T>(val rows: List<T>, val cursor: Long)

interface CloudDataSource {
    val available: Boolean

    suspend fun pushTransactions(uid: String, rows: List<Transaction>)

    /** `sinceCursor` of [NO_CURSOR] pulls the whole collection. */
    suspend fun pullTransactions(uid: String, sinceCursor: Long): CloudPage<Transaction>

    suspend fun pushCategories(uid: String, rows: List<Category>)

    /** `sinceCursor` of [NO_CURSOR] pulls the whole collection. */
    suspend fun pullCategories(uid: String, sinceCursor: Long): CloudPage<Category>

    suspend fun pushProfile(uid: String, profile: UserProfile)

    /**
     * One document, so it is always read whole — [SyncMerge.decideProfile] is the only gate that
     * decides whether it wins. A cursor here would just be a second, weaker gate.
     */
    suspend fun pullProfile(uid: String): UserProfile?

    /** Removes every document under `users/{uid}/`. */
    suspend fun deleteAll(uid: String)

    companion object {
        /** No watermark yet: pull everything, including rows written before `syncedAt` existed. */
        const val NO_CURSOR = 0L

        /** Server-assigned write time, the only clock both devices agree on. */
        const val SYNCED_AT = "syncedAt"
    }
}
