package app.wherego.core.sync

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncMergeTest {
    @Test
    fun remoteNewerWins() {
        assertEquals(
            MergeDecision.ApplyRemote,
            SyncMerge.decide(localUpdatedAt = 10L, localDirty = true, remoteUpdatedAt = 20L),
        )
        assertEquals(
            MergeDecision.ApplyRemote,
            SyncMerge.decide(localUpdatedAt = null, localDirty = false, remoteUpdatedAt = 5L),
        )
    }

    @Test
    fun localDirtyPushesWhenNotOlder() {
        assertEquals(
            MergeDecision.PushLocal,
            SyncMerge.decide(localUpdatedAt = 20L, localDirty = true, remoteUpdatedAt = 10L),
        )
        assertEquals(
            MergeDecision.PushLocal,
            SyncMerge.decide(localUpdatedAt = 10L, localDirty = true, remoteUpdatedAt = 10L),
        )
        assertEquals(
            MergeDecision.PushLocal,
            SyncMerge.decide(localUpdatedAt = 10L, localDirty = true, remoteUpdatedAt = null),
        )
    }

    @Test
    fun cleanLocalStaysWhenRemoteNotNewer() {
        assertEquals(
            MergeDecision.KeepLocal,
            SyncMerge.decide(localUpdatedAt = 10L, localDirty = false, remoteUpdatedAt = 10L),
        )
        assertEquals(
            MergeDecision.KeepLocal,
            SyncMerge.decide(localUpdatedAt = 10L, localDirty = false, remoteUpdatedAt = null),
        )
    }

    @Test
    fun clearDirtyIfUpdatedAtUnchanged() {
        assertTrue(SyncMerge.shouldClearDirty(5L, 5L))
        assertFalse(SyncMerge.shouldClearDirty(5L, 6L))
    }
}
