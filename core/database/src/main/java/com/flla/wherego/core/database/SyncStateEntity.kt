package com.flla.wherego.core.database

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Where a collection's incremental pull resumes from.
 *
 * [lastPullCursor] is epoch **nanoseconds in the cloud's clock domain** — it is copied from the
 * `syncedAt` server timestamp of the last row this device actually received, never read off the
 * local clock. A device clock cannot be compared against timestamps another device authored:
 * a peer's backlog is stamped in the past, so a local-clock watermark skips it forever.
 *
 * `0` means "no cursor yet" and asks for a full pull.
 */
@Entity(tableName = "sync_state")
data class SyncStateEntity(
    @PrimaryKey val collection: String,
    val lastPullCursor: Long,
)
