package app.wherego.core.sync

enum class MergeDecision {
    KeepLocal,
    ApplyRemote,
    PushLocal,
}

object SyncMerge {
    /**
     * Last-write-wins on [updatedAt]. Dirty local rows push when remote is not newer.
     * Missing remote → push if dirty. Missing local → apply remote.
     */
    fun decide(
        localUpdatedAt: Long?,
        localDirty: Boolean,
        remoteUpdatedAt: Long?,
    ): MergeDecision {
        if (localUpdatedAt == null && remoteUpdatedAt == null) return MergeDecision.KeepLocal
        if (localUpdatedAt == null) return MergeDecision.ApplyRemote
        if (remoteUpdatedAt == null) {
            return if (localDirty) MergeDecision.PushLocal else MergeDecision.KeepLocal
        }
        if (remoteUpdatedAt > localUpdatedAt) return MergeDecision.ApplyRemote
        if (localDirty) return MergeDecision.PushLocal
        return MergeDecision.KeepLocal
    }

    fun shouldClearDirty(updatedAtBeforePush: Long, updatedAtAfterPush: Long): Boolean =
        updatedAtBeforePush == updatedAtAfterPush
}

enum class CloudDot {
    Synced,
    Pending,
    Offline,
}
