package com.flla.wherego.core.sync

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

    /**
     * Reinstall creates a fresh guest with `updatedAt = now`. Last-write-wins would
     * keep that placeholder and hide an already-onboarded cloud profile. A guest
     * that has not finished the tour never beats a remote profile that has.
     */
    fun decideProfile(
        localOnboardingDone: Boolean?,
        localUpdatedAt: Long?,
        remoteOnboardingDone: Boolean,
        remoteUpdatedAt: Long,
    ): MergeDecision {
        if (localOnboardingDone != true && remoteOnboardingDone) {
            return MergeDecision.ApplyRemote
        }
        return decide(
            localUpdatedAt = localUpdatedAt,
            localDirty = false,
            remoteUpdatedAt = remoteUpdatedAt,
        )
    }

    /**
     * A push only speaks for the row it carried. Compare the `updatedAt` that went to the cloud
     * against the row as it stands now: unchanged means the flag can drop, moved means someone
     * edited it mid-flight and it still owes the cloud a copy.
     */
    fun shouldClearDirty(pushedUpdatedAt: Long, currentUpdatedAt: Long): Boolean =
        pushedUpdatedAt == currentUpdatedAt
}

enum class CloudDot {
    Synced,
    Pending,
    Offline,
}
